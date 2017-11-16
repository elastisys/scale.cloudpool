package com.elastisys.scale.cloudpool.aws.ec2.driver;

import static com.elastisys.scale.commons.json.JsonUtils.toJson;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.commons.ScalingFilters;
import com.elastisys.scale.cloudpool.aws.commons.ScalingTags;
import com.elastisys.scale.cloudpool.aws.commons.functions.InstanceToMachine;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2Client;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2ProvisioningTemplate;
import com.elastisys.scale.cloudpool.aws.ec2.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.TerminateMachinesException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * A {@link CloudPoolDriver} implementation that operates against the AWS EC2
 * cloud API.
 *
 * @see BaseCloudPool
 */
public class Ec2PoolDriver implements CloudPoolDriver {

    static Logger LOG = LoggerFactory.getLogger(Ec2PoolDriver.class);

    /** The current driver configuration. */
    private DriverConfig config;

    /** A client used to communicate with the AWS EC2 API. */
    private final Ec2Client client;

    /** Lock to prevent concurrent access to critical sections. */
    private final Object lock = new Object();

    /**
     * Creates a new {@link Ec2PoolDriver}. Needs to be configured before use.
     *
     * @param client
     *            The {@link Ec2Client} used to communicate with the AWS EC2
     *            API.
     */
    public Ec2PoolDriver(Ec2Client client) {
        this.config = null;
        this.client = client;
    }

    @Override
    public void configure(DriverConfig configuration) throws IllegalArgumentException, CloudPoolDriverException {

        synchronized (this.lock) {
            CloudApiSettings cloudApiSettings = configuration.parseCloudApiSettings(CloudApiSettings.class);
            cloudApiSettings.validate();

            // parse and validate openstack-specific provisioningTemplate
            Ec2ProvisioningTemplate provisioningTemplate = configuration
                    .parseProvisioningTemplate(Ec2ProvisioningTemplate.class);
            provisioningTemplate.validate();

            this.config = configuration;
            ClientConfiguration clientConfig = new ClientConfiguration()
                    .withConnectionTimeout(cloudApiSettings.getConnectionTimeout())
                    .withSocketTimeout(cloudApiSettings.getSocketTimeout());
            this.client.configure(cloudApiSettings.getAwsAccessKeyId(), cloudApiSettings.getAwsSecretAccessKey(),
                    cloudApiSettings.getRegion(), clientConfig);
        }
    }

    @Override
    public List<Machine> listMachines() throws CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured Ec2PoolDriver");

        // filter instances on cloud pool tag
        return Lists.transform(getPoolInstances(), new InstanceToMachine());
    }

    @Override
    public List<Machine> startMachines(int count) throws StartMachinesException {
        checkState(isConfigured(), "attempt to use unconfigured Ec2PoolDriver");

        List<Machine> startedMachines = Lists.newArrayList();
        try {
            // set tag to make sure instances are recognized as pool members
            Ec2ProvisioningTemplate template = provisioningTemplate();
            template = template.withTag(ScalingTags.CLOUD_POOL_TAG, getPoolName());

            List<Instance> newInstances = this.client.launchInstances(template, count);
            startedMachines = Lists.transform(newInstances, new InstanceToMachine());

            // set instance Name tags
            for (Instance instance : newInstances) {
                tagInstance(instance.getInstanceId(), nameTag(instance));
            }
        } catch (Exception e) {
            throw new StartMachinesException(count, startedMachines, e);
        }

        return startedMachines;
    }

    @Override
    public void terminateMachines(List<String> instanceIds)
            throws IllegalStateException, TerminateMachinesException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured Ec2PoolDriver");
        List<String> victimIds = new ArrayList<>(instanceIds);
        LOG.info("request to terminate instances: {}", instanceIds);

        List<Instance> poolInstances = getPoolInstances();

        // track termination failures
        Map<String, Throwable> failures = new HashMap<>();

        // only terminate pool members (error mark other requests)
        nonPoolMembers(instanceIds, poolInstances).stream().forEach(instanceId -> {
            failures.put(instanceId,
                    new NotFoundException(String.format("instance %s is not a member of the pool", instanceId)));
            victimIds.remove(instanceId);
        });

        // none of the machine ids were pool members
        if (victimIds.isEmpty()) {
            throw new TerminateMachinesException(Collections.emptyList(), failures);
        }

        LOG.debug("terminating pool member instances: {}", victimIds);
        try {
            this.client.terminateInstances(victimIds);
        } catch (Exception e) {
            String message = format("failed to terminate instances %s: %s", victimIds, e.getMessage());
            throw new CloudPoolDriverException(message, e);
        }

        if (!failures.isEmpty()) {
            throw new TerminateMachinesException(victimIds, failures);
        }
    }

    /**
     * Returns the list of machine ids (from a given list of machine ids) that
     * are *not* members of the given pool.
     *
     * @param machineIds
     * @param pool
     * @return
     */
    private static List<String> nonPoolMembers(List<String> machineIds, List<Instance> pool) {
        return machineIds.stream().filter(machineId -> !member(machineId, pool)).collect(Collectors.toList());
    }

    /**
     * Returns <code>true</code> if the given instance id is found in the given
     * instance pool.
     *
     * @param instanceId
     * @param pool
     * @return
     */
    private static boolean member(String instanceId, List<Instance> pool) {
        return pool.stream().anyMatch(i -> i.getInstanceId().equals(instanceId));
    }

    @Override
    public void attachMachine(String machineId) throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured Ec2PoolDriver");

        // verify that machine exists
        this.client.getInstanceMetadata(machineId);

        try {
            Tag tag = new Tag(ScalingTags.CLOUD_POOL_TAG, getPoolName());
            tagInstance(machineId, tag);
        } catch (Exception e) {
            Throwables.throwIfInstanceOf(e, NotFoundException.class);
            throw new CloudPoolDriverException(
                    String.format("failed to attach '%s' to cloud pool: %s", machineId, e.getMessage()), e);
        }
    }

    @Override
    public void detachMachine(String machineId) throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured Ec2PoolDriver");

        // verify that machine exists in group
        getMachineOrFail(machineId);

        Tag membershipTag = new Tag(ScalingTags.CLOUD_POOL_TAG, getPoolName());
        try {
            this.client.untagResource(machineId, asList(membershipTag));
        } catch (Exception e) {
            throw new CloudPoolDriverException(String.format("failed to remove tag '%s' from instance %s: %s",
                    membershipTag, machineId, e.getMessage()), e);
        }
    }

    @Override
    public void setServiceState(String machineId, ServiceState serviceState)
            throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured Ec2PoolDriver");

        // verify that machine exists in group
        getMachineOrFail(machineId);

        try {
            Tag tag = new Tag(ScalingTags.SERVICE_STATE_TAG, serviceState.name());
            tagInstance(machineId, tag);
        } catch (Exception e) {
            Throwables.throwIfInstanceOf(e, NotFoundException.class);
            throw new CloudPoolDriverException(
                    String.format("failed to set service state for instance %s: %s", machineId, e.getMessage()), e);
        }
    }

    @Override
    public void setMembershipStatus(String machineId, MembershipStatus membershipStatus)
            throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured Ec2PoolDriver");

        // verify that machine exists in group
        getMachineOrFail(machineId);

        try {
            Tag tag = new Tag(ScalingTags.MEMBERSHIP_STATUS_TAG, JsonUtils.toString(toJson(membershipStatus)));
            tagInstance(machineId, tag);
        } catch (Exception e) {
            Throwables.throwIfInstanceOf(e, NotFoundException.class);
            throw new CloudPoolDriverException(
                    String.format("failed to set membership status for instance %s: %s", machineId, e.getMessage()), e);
        }

    }

    @Override
    public String getPoolName() {
        checkState(isConfigured(), "attempt to use unconfigured Ec2PoolDriver");
        return config().getPoolName();
    }

    /**
     * Retrieves a particular member instance from the pool or throws an
     * exception if it could not be found.
     *
     * @param machineId
     *            The id of the machine of interest.
     * @return
     * @throws NotFoundException
     */
    private Machine getMachineOrFail(String machineId) throws NotFoundException {
        List<Machine> machines = listMachines();
        for (Machine machine : machines) {
            if (machine.getId().equals(machineId)) {
                return machine;
            }
        }

        throw new NotFoundException(String.format("no machine with id '%s' found in cloud pool", machineId));
    }

    /**
     * Creates a name tag to assign to a new pool member {@link Instance}.
     *
     * @param instance
     * @return
     */
    private Tag nameTag(Instance instance) {
        // assign a name to the instance
        String instanceName = String.format("%s-%s", getPoolName(), instance.getInstanceId());
        return new Tag(ScalingTags.INSTANCE_NAME_TAG, instanceName);
    }

    /**
     * Set a collection of tags on a machine instance.
     *
     * @param instanceId
     *            The id of the instance to tag.
     * @param tags
     *            The tags to set on the instance.
     * @return Returns updated meta data about the {@link Instance}. throws
     *         {@link NotFoundException}
     * @throws CloudPoolDriverException
     */
    private Instance tagInstance(String instanceId, Tag... tags) throws NotFoundException, CloudPoolDriverException {
        this.client.tagResource(instanceId, Arrays.asList(tags));
        return this.client.getInstanceMetadata(instanceId);
    }

    /**
     * Retrieves all {@link Instance}s that are members of the machine pool.
     *
     * @return
     * @throws CloudPoolDriverException
     */
    private List<Instance> getPoolInstances() throws CloudPoolDriverException {
        try {
            Filter filter = new Filter().withName(ScalingFilters.CLOUD_POOL_TAG_FILTER).withValues(getPoolName());
            List<Instance> instances = this.client.getInstances(asList(filter));
            return instances;
        } catch (Exception e) {
            throw new CloudPoolDriverException(
                    format("failed to retrieve machines in cloud pool \"%s\": %s", getPoolName(), e.getMessage()), e);
        }
    }

    boolean isConfigured() {
        return config() != null;
    }

    DriverConfig config() {
        return this.config;
    }

    CloudApiSettings cloudApiSettings() {
        return config().parseCloudApiSettings(CloudApiSettings.class);
    }

    Ec2ProvisioningTemplate provisioningTemplate() {
        return config().parseProvisioningTemplate(Ec2ProvisioningTemplate.class);
    }

}
