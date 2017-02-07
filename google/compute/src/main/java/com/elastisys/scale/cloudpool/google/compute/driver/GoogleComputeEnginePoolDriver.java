package com.elastisys.scale.cloudpool.google.compute.driver;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.CloudProviders;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.google.commons.api.CloudApiSettings;
import com.elastisys.scale.cloudpool.google.commons.api.compute.ComputeClient;
import com.elastisys.scale.cloudpool.google.commons.api.compute.InstanceGroupClient;
import com.elastisys.scale.cloudpool.google.commons.api.compute.functions.InstanceToMachine;
import com.elastisys.scale.cloudpool.google.commons.api.compute.metadata.MetadataKeys;
import com.elastisys.scale.cloudpool.google.commons.utils.InstanceGroupUrl;
import com.elastisys.scale.cloudpool.google.commons.utils.MetadataUtil;
import com.elastisys.scale.cloudpool.google.commons.utils.ZoneUtils;
import com.elastisys.scale.cloudpool.google.compute.driver.config.ProvisioningTemplate;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.url.UrlUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceGroupManager;
import com.google.api.services.compute.model.InstanceTemplate;
import com.google.api.services.compute.model.ManagedInstance;

/**
 * A {@link CloudPoolDriver} that manages the size of a Google Compute Engine
 * <a href="https://cloud.google.com/compute/docs/instance-groups/">Instance
 * Group</a>.
 * <p/>
 * This instance group is assumed to already exist. Load-balancing between
 * instances in the group is considered out-of-scope and needs to be
 * enabled/configured by external means.
 */
public class GoogleComputeEnginePoolDriver implements CloudPoolDriver {
    private static final Logger LOG = LoggerFactory.getLogger(GoogleComputeEnginePoolDriver.class);

    /**
     * Name prefix added to psuedo instances generated for requested (but not
     * yet acquired) instances.
     */
    public static final String REQUESTED_INSTANCE_PREFIX = "requested";

    /** Google Compute Engine client. */
    private final ComputeClient client;

    /** The currently set {@link DriverConfig}. */
    private DriverConfig config;

    /** Protect critical sections from concurrent access. */
    private final Object lock = new Object();

    public GoogleComputeEnginePoolDriver(ComputeClient client) {
        this.client = client;
    }

    @Override
    public void configure(DriverConfig configuration) throws IllegalArgumentException, CloudPoolDriverException {
        configuration.validate();

        synchronized (this.lock) {
            // parse and validate GCE-specific cloudApiSettings
            CloudApiSettings cloudApiSettings = configuration.parseCloudApiSettings(CloudApiSettings.class);
            cloudApiSettings.validate();

            // parse and validate GCE-specific provisioningTemplate
            ProvisioningTemplate provisioningTemplate = configuration
                    .parseProvisioningTemplate(ProvisioningTemplate.class);
            provisioningTemplate.validate();

            this.client.configure(cloudApiSettings);
            this.config = configuration;
        }
    }

    @Override
    public List<Machine> listMachines() throws IllegalStateException, CloudPoolDriverException {
        ensureConfigured();

        InstanceGroupClient instanceGroupApi = instanceGroupClient();
        InstanceGroupManager instanceGroup = instanceGroupApi.getInstanceGroup();
        List<ManagedInstance> members = instanceGroupApi.listInstances();

        // fetch detailed metadata about each member instance
        List<Instance> instances = new ArrayList<>();
        for (ManagedInstance member : members) {
            String instanceUrl = member.getInstance();
            Instance instance = this.client.getInstance(instanceUrl);
            instances.add(instance);
        }

        // convert from Instance to Machine
        List<Machine> machines = instances.stream().map(new InstanceToMachine()::apply).collect(Collectors.toList());

        int numMembers = members.size();
        // use max of targetSize and number of member instances since there is a
        // small chance that the group size has changed between the call to get
        // the instance group metadata and the call to list instances
        int targetSize = Math.max(instanceGroup.getTargetSize(), numMembers);
        int numAwaiting = targetSize - numMembers;
        LOG.debug("psuedo (requested) instances: {}   (member instances: {}, target size: {})", numAwaiting, numMembers,
                instanceGroup.getTargetSize());
        if (numAwaiting > 0) {
            // return placeholder machines for requested-but-not-yet-acquired
            InstanceTemplate instanceTemplate = this.client.getInstanceTemplate(instanceGroup.getInstanceTemplate());
            machines.addAll(placeholderMachines(numAwaiting, instanceTemplate));
        }

        return machines;
    }

    @Override
    public List<Machine> startMachines(int count) throws IllegalStateException, StartMachinesException {
        ensureConfigured();

        if (count == 0) {
            return Collections.emptyList();
        }

        InstanceGroupClient instanceGroupApi = instanceGroupClient();
        InstanceGroupManager instanceGroup = instanceGroupApi.getInstanceGroup();
        int targetSize = instanceGroup.getTargetSize() + count;
        LOG.info("starting {} new instances in instance group '{}': changing targetSize from {} to {}", count,
                instanceGroup.getName(), instanceGroup.getTargetSize(), targetSize);
        instanceGroupApi.resize(targetSize);

        InstanceTemplate instanceTemplate = this.client.getInstanceTemplate(instanceGroup.getInstanceTemplate());

        return placeholderMachines(count, instanceTemplate);
    }

    @Override
    public void terminateMachine(String instanceUrl)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        ensureConfigured();

        if (UrlUtils.basename(instanceUrl).startsWith(REQUESTED_INSTANCE_PREFIX)) {
            // we were asked to terminate a pseudo instance for a requested, but
            // not yet acquired, instance. just decrement the targetSize of the
            // group.
            int targetSize = instanceGroupClient().getInstanceGroup().getTargetSize();
            int decrementedSize = targetSize - 1;
            LOG.info("termination request for psuedo instance {}, reducing target size from {} to {}", instanceUrl,
                    targetSize, decrementedSize);
            instanceGroupClient().resize(decrementedSize);
        } else {
            ensureGroupMember(instanceUrl);
            LOG.info("removing instance {} from instance group {} ...", UrlUtils.basename(instanceUrl),
                    provisioningTemplate().getInstanceGroup());
            instanceGroupClient().deleteInstances(Arrays.asList(instanceUrl));
        }
    }

    @Override
    public void attachMachine(String instanceUrl)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        throw new CloudPoolDriverException(
                "the Google Compute Engine API does not support attaching instances to a managed instance group (created from an instance template)");
    }

    @Override
    public void detachMachine(String instanceUrl)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        ensureConfigured();

        ensureGroupMember(instanceUrl);

        LOG.info("detaching instance {} from instance group {} ...", UrlUtils.basename(instanceUrl),
                provisioningTemplate().getInstanceGroup());
        instanceGroupClient().abandonInstances(Arrays.asList(instanceUrl));
    }

    @Override
    public void setServiceState(String instanceUrl, ServiceState serviceState)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        ensureConfigured();

        Instance instance = ensureGroupMember(instanceUrl);

        LOG.debug("setting service state {} for instance {} ...", serviceState.name(), UrlUtils.basename(instanceUrl));

        Map<String, String> metadataMap = MetadataUtil.toMap(instance.getMetadata());
        metadataMap.put(MetadataKeys.SERVICE_STATE, serviceState.name());

        this.client.setMetadata(instanceUrl, instance.getMetadata().setItems(MetadataUtil.toItems(metadataMap)));
    }

    @Override
    public void setMembershipStatus(String instanceUrl, MembershipStatus membershipStatus)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        ensureConfigured();

        Instance instance = ensureGroupMember(instanceUrl);

        LOG.debug("setting membership status {} for instance {} ...", membershipStatus, UrlUtils.basename(instanceUrl));

        Map<String, String> metadataMap = MetadataUtil.toMap(instance.getMetadata());
        metadataMap.put(MetadataKeys.MEMBERSHIP_STATUS, JsonUtils.toString(JsonUtils.toJson(membershipStatus)));

        this.client.setMetadata(instanceUrl, instance.getMetadata().setItems(MetadataUtil.toItems(metadataMap)));
    }

    @Override
    public String getPoolName() throws IllegalStateException {
        ensureConfigured();
        return config().getPoolName();
    }

    /**
     * Creates a number of placeholder {@link Machine}s in {@code REQUESTED}
     * machine state to represent instances that have been requested but not yet
     * been acquired by the backing GCE instance group.
     *
     * @param numPseudoMachines
     * @param instanceTemplate
     * @return
     */
    private List<Machine> placeholderMachines(int numPseudoMachines, InstanceTemplate instanceTemplate) {
        List<Machine> psuedoMachines = new ArrayList<>();
        for (int i = 1; i <= numPseudoMachines; i++) {
            psuedoMachines
                    .add(Machine.builder().id(REQUESTED_INSTANCE_PREFIX + "-" + instanceTemplate.getName() + "-" + i)
                            .cloudProvider(CloudProviders.GCE).region(getRegion())
                            .machineSize(instanceTemplate.getProperties().getMachineType())
                            .machineState(MachineState.REQUESTED).launchTime(UtcTime.now()).build());
        }
        return psuedoMachines;
    }

    private String getRegion() {
        if (provisioningTemplate().isSingleZoneGroup()) {
            String zone = provisioningTemplate().getZone();
            return ZoneUtils.regionName(zone);
        }
        return provisioningTemplate().getRegion();
    }

    private Instance ensureGroupMember(String instanceUrl) throws NotFoundException {
        return getGroupMember(instanceUrl);
    }

    /**
     * Returns a particular group member, or throws a {@link NotFoundException}
     * if the instance either does not exist, or it is not found to be a member
     * of the instance group.
     *
     * @param instanceUrl
     * @return
     * @throws NotFoundException
     */
    private Instance getGroupMember(String instanceUrl) throws NotFoundException {
        InstanceGroupClient instanceGroupClient = instanceGroupClient();
        InstanceGroupManager instanceGroup = instanceGroupClient.getInstanceGroup();
        List<ManagedInstance> members = instanceGroupClient.listInstances();

        for (ManagedInstance member : members) {
            if (member.getInstance().equals(instanceUrl)) {
                return this.client.getInstance(member.getInstance());
            }
        }
        throw new NotFoundException(
                String.format("instance %s does not exist in instance group %s", instanceUrl, instanceGroup));
    }

    private void ensureConfigured() {
        checkState(isConfigured(), "attempt to use GCE cloud pool before being configured");
    }

    private boolean isConfigured() {
        return config() != null;
    }

    DriverConfig config() {
        return this.config;
    }

    CloudApiSettings cloudApiSettings() {
        ensureConfigured();
        return this.config.parseCloudApiSettings(CloudApiSettings.class);
    }

    ProvisioningTemplate provisioningTemplate() {
        ensureConfigured();
        return this.config.parseProvisioningTemplate(ProvisioningTemplate.class);
    }

    /**
     * Returns an {@link InstanceGroupClient} for managing the configured
     * instance group.
     *
     * @return
     */
    InstanceGroupClient instanceGroupClient() {
        ProvisioningTemplate provisioningTemplate = provisioningTemplate();
        if (provisioningTemplate.isSingleZoneGroup()) {
            InstanceGroupUrl groupUrl = InstanceGroupUrl.managedZonal(provisioningTemplate.getProject(),
                    provisioningTemplate.getZone(), provisioningTemplate.getInstanceGroup());
            return this.client.singleZoneInstanceGroup(groupUrl.getUrl());
        } else {
            InstanceGroupUrl groupUrl = InstanceGroupUrl.managedRegional(provisioningTemplate.getProject(),
                    provisioningTemplate.getRegion(), provisioningTemplate.getInstanceGroup());
            return this.client.multiZoneInstanceGroup(groupUrl.getUrl());
        }
    }
}
