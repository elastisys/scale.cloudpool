package com.elastisys.scale.cloudpool.openstack.driver;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.transform;
import static java.lang.String.format;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.openstack.driver.client.OpenstackClient;
import com.elastisys.scale.cloudpool.openstack.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.openstack.driver.config.ProvisioningTemplate;
import com.elastisys.scale.cloudpool.openstack.functions.ServerToMachine;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * A {@link CloudPoolDriver} implementation that operates against OpenStack.
 *
 * @see BaseCloudPool
 */
public class OpenStackPoolDriver implements CloudPoolDriver {
    private static Logger LOG = LoggerFactory.getLogger(OpenStackPoolDriver.class);

    /** The current driver configuration. */
    private DriverConfig config;

    /** Client used to communicate with the OpenStack API. */
    private final OpenstackClient client;

    /** Lock to prevent concurrent access to critical sections. */
    private final Object lock = new Object();

    /**
     * The name of the cloud provider that the cloud pool operates against. For
     * example, {@code RackSpace}. This will only be used to set the
     * {@link Machine#getCloudProvider()} field of pool members.
     */
    private final String cloudProvider;

    /**
     * Creates a new {@link OpenStackPoolDriver}. Needs to be configured before
     * use.
     *
     * @param client
     *            The client to be used to communicate with the OpenStack API.
     * @param cloudProvider
     *            The name of the cloud provider that the cloud pool operates
     *            against. For example, {@code RackSpace}. This will only be
     *            used to set the {@link Machine#getCloudProvider()} field of
     *            pool members.
     */
    public OpenStackPoolDriver(OpenstackClient client, String cloudProvider) {
        this.config = null;
        this.client = client;
        this.cloudProvider = cloudProvider;
    }

    @Override
    public void configure(DriverConfig configuration) throws CloudPoolDriverException {
        synchronized (this.lock) {
            // parse and validate openstack-specific cloudApiSettings
            CloudApiSettings cloudApiSettings = configuration.parseCloudApiSettings(CloudApiSettings.class);
            cloudApiSettings.validate();

            // parse and validate openstack-specific provisioningTemplate
            ProvisioningTemplate provisioningTemplate = configuration
                    .parseProvisioningTemplate(ProvisioningTemplate.class);
            provisioningTemplate.validate();

            this.config = configuration;
            this.client.configure(cloudApiSettings);
        }
    }

    @Override
    public List<Machine> listMachines() throws CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        try {
            List<Server> servers = this.client.getServers(Constants.CLOUD_POOL_TAG, getPoolName());
            return transform(servers, serverToMachine());
        } catch (Exception e) {
            throw new CloudPoolDriverException(
                    format("failed to retrieve machines in cloud pool \"%s\": %s", getPoolName(), e.getMessage()), e);
        }
    }

    @Override
    public List<Machine> startMachines(int count) throws StartMachinesException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        List<Machine> startedMachines = Lists.newArrayList();
        try {
            for (int i = 0; i < count; i++) {
                // tag new server with cloud pool membership
                Map<String, String> tags = ImmutableMap.of(Constants.CLOUD_POOL_TAG, getPoolName());
                Server newServer = this.client.launchServer(uniqueServerName(), provisioningTemplate(), tags);
                startedMachines.add(serverToMachine().apply(newServer));

                if (provisioningTemplate().isAssignFloatingIp()) {
                    String serverId = newServer.getId();
                    this.client.assignFloatingIp(serverId);
                    // update meta data to include the public IP
                    startedMachines.set(i, serverToMachine().apply(this.client.getServer(serverId)));
                }
            }
        } catch (Exception e) {
            throw new StartMachinesException(count, startedMachines, e);
        }
        return startedMachines;
    }

    @Override
    public void terminateMachine(String machineId) throws CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        // verify that machine exists in group
        getMachineOrFail(machineId);

        try {
            this.client.terminateServer(machineId);
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, CloudPoolDriverException.class);
            String message = format("failed to terminate server \"%s\": %s", machineId, e.getMessage());
            throw new CloudPoolDriverException(message, e);
        }
    }

    @Override
    public void attachMachine(String machineId) throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        Map<String, String> tags = ImmutableMap.of(Constants.CLOUD_POOL_TAG, getPoolName());
        try {
            this.client.tagServer(machineId, tags);
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, NotFoundException.class);
            Throwables.propagateIfInstanceOf(e, CloudPoolDriverException.class);
            String message = format("failed to attach server \"%s\": %s", machineId, e.getMessage());
            throw new CloudPoolDriverException(message, e);
        }

    }

    @Override
    public void detachMachine(String machineId) throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        // verify that machine exists in group
        getMachineOrFail(machineId);

        try {
            List<String> tagKeys = Arrays.asList(Constants.CLOUD_POOL_TAG);
            this.client.untagServer(machineId, tagKeys);
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, CloudPoolDriverException.class);
            String message = format("failed to detach server \"%s\": %s", machineId, e.getMessage());
            throw new CloudPoolDriverException(message, e);
        }
    }

    @Override
    public void setServiceState(String machineId, ServiceState serviceState) throws NotFoundException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        // verify that machine exists in group
        getMachineOrFail(machineId);

        try {
            LOG.debug("service state {} reported for {}", serviceState.name(), machineId);
            // set serviceState as tag on machine instance
            Map<String, String> tags = ImmutableMap.of(Constants.SERVICE_STATE_TAG, serviceState.name());
            this.client.tagServer(machineId, tags);
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, CloudPoolDriverException.class);
            String message = format("failed to tag service state on server \"%s\": %s", machineId, e.getMessage());
            throw new CloudPoolDriverException(message, e);
        }
    }

    @Override
    public void setMembershipStatus(String machineId, MembershipStatus membershipStatus)
            throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        // verify that machine exists in group
        getMachineOrFail(machineId);

        try {
            LOG.debug("membership status {} reported for {}", membershipStatus, machineId);
            // set serviceState as tag on machine instance
            Map<String, String> tags = ImmutableMap.of(Constants.MEMBERSHIP_STATUS_TAG,
                    JsonUtils.toString(JsonUtils.toJson(membershipStatus)));
            this.client.tagServer(machineId, tags);
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, CloudPoolDriverException.class);
            String message = format("failed to tag membership status on server \"%s\": %s", machineId, e.getMessage());
            throw new CloudPoolDriverException(message, e);
        }
    }

    @Override
    public String getPoolName() {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        return config().getPoolName();
    }

    /**
     * Retrieves a particular member machine from the cloud pool or throws an
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
     * Create a unique server host name for a new server instance. This is
     * necessary to make sure that different hosts do not report monitoring
     * values using the same host name, which may confuse OpenTSDB.
     *
     * @return
     */
    private String uniqueServerName() {
        String prefix = getPoolName();
        String suffix = UUID.randomUUID().toString();
        return String.format("%s-%s", prefix, suffix);
    }

    /**
     * A {@link Function} that converts from {@link Server} to {@link Machine}.
     *
     * @return
     */
    private ServerToMachine serverToMachine() {
        return new ServerToMachine(this.cloudProvider, cloudApiSettings().getRegion());
    }

    boolean isConfigured() {
        return config() != null;
    }

    DriverConfig config() {
        return this.config;
    }

    ProvisioningTemplate provisioningTemplate() {
        return config().parseProvisioningTemplate(ProvisioningTemplate.class);
    }

    CloudApiSettings cloudApiSettings() {
        return config().parseCloudApiSettings(CloudApiSettings.class);
    }

}
