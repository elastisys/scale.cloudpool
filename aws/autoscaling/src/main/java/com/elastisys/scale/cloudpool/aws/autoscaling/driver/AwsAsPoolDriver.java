package com.elastisys.scale.cloudpool.aws.autoscaling.driver;

import static com.elastisys.scale.commons.json.JsonUtils.toJson;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.CloudProviders;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.client.AutoScalingClient;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.config.ProvisioningTemplate;
import com.elastisys.scale.cloudpool.aws.commons.ScalingTags;
import com.elastisys.scale.cloudpool.aws.commons.functions.InstanceToMachine;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.TerminateMachinesException;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * A {@link CloudPoolDriver} implementation that manages an AWS Auto Scaling
 * Group over the AWS Auto Scaling API.
 * <p/>
 * This client assumes that an AWS <i>Auto Scaling group</i> with a proper
 * <i>launch configuration</i> (specifying how new instances are to be created)
 * has already been created by external means (for instance, through the AWS
 * Auto Scaling command-line interface). Load-balancing between instances, if
 * needed, is also assumed to be taken care of, either via Elastic Load Balancer
 * or with a custom-made load balancing solution.
 *
 * @see BaseCloudPool
 */
public class AwsAsPoolDriver implements CloudPoolDriver {
    static Logger LOG = LoggerFactory.getLogger(AwsAsPoolDriver.class);

    public static final String REQUESTED_ID_PREFIX = "i-requested";

    /** The current driver configuration. */
    private DriverConfig config;

    /** A client used to communicate with the AWS Auto Scaling API. */
    private final AutoScalingClient client;

    /** Prevent concurrent access to critical sections. */
    private final Object lock = new Object();

    /**
     * Creates a new {@link AwsAsPoolDriver}. Needs to be configured before use.
     *
     * @param client
     *            A client used to communicate with the AWS Auto Scaling API.
     */
    public AwsAsPoolDriver(AutoScalingClient client) {
        this.config = null;

        this.client = client;
    }

    @Override
    public void configure(DriverConfig configuration) throws IllegalArgumentException, CloudPoolDriverException {
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

    /**
     * Returns the machine instances in the Auto Scaling Group pool.
     * <p/>
     * For the case where {@code desiredCapacity} is greater than the number of
     * started instances, we produce a number of placeholder {@link Machine}s
     * (in {@code REQUESTED} state) for requested, but not yet acquired,
     * instances in an Auto Scaling Group. The number of produced placeholder
     * instances is the the difference between {@code desiredCapacity} and
     * {@code actualCapacity}.
     * <p/>
     * Rationale: the desired capacity of the AWS Auto Scaling Group may differ
     * from the actual number of instances in the group. If the desiredCapacity
     * of the Auto Scaling Group is greater than the actual number of instances
     * in the group, we should return placeholder Machines in {@code REQUESTED}
     * state for the missing instances. This prevents the {@link BaseCloudPool}
     * from regarding the scaling group too small and ordering new machines via
     * startMachines.
     *
     * @see com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver#listMachines()
     */
    @Override
    public List<Machine> listMachines() throws CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        try {
            AutoScalingGroup group = this.client.getAutoScalingGroup(scalingGroupName());
            int desiredCapacity = group.getDesiredCapacity();

            // fetch actual scaling group members
            List<Instance> groupInstances = this.client.getAutoScalingGroupMembers(scalingGroupName());
            List<Machine> acquiredMachines = groupInstances.stream().map(new InstanceToMachine())
                    .collect(Collectors.toList());
            int actualCapacity = acquiredMachines.size();

            // requested, but not yet allocated, machines
            int missingInstances = Math.max(desiredCapacity - actualCapacity, 0);
            LaunchConfiguration launchConfig = this.client.getLaunchConfiguration(group.getLaunchConfigurationName());
            List<Machine> requestedInstances = pseudoMachines(missingInstances, launchConfig);

            List<Machine> pool = new ArrayList<>();
            pool.addAll(acquiredMachines);
            pool.addAll(requestedInstances);
            return pool;
        } catch (Exception e) {
            throw new CloudPoolDriverException(
                    format("failed to retrieve machines in cloud pool \"%s\", Auto Scaling Group \"%s\": %s",
                            getPoolName(), scalingGroupName(), e.getMessage()),
                    e);
        }
    }

    /**
     * Creates a number of "pseudo machine" in {@code REQUESTED} state as a
     * place-holder machines for a desired but not yet acquired Auto Scaling
     * Group members.
     *
     * @param missingInstances
     *            Number of missing Auto Scaling Group instances.
     * @param launchConfig
     *            The launch configuration of the Auto Scaling Group.
     * @return
     */
    private List<Machine> pseudoMachines(int missingInstances, LaunchConfiguration launchConfig) {
        List<Machine> requestedInstances = new ArrayList<>();
        for (int i = 0; i < missingInstances; i++) {
            String pseudoId = String.format("%s%d", REQUESTED_ID_PREFIX, i + 1);
            requestedInstances.add(pseudoMachine(pseudoId, launchConfig));
        }
        return requestedInstances;
    }

    /**
     * Creates a "pseudo machine" in {@code REQUESTED} state as a place-holder
     * machine for a desired but not yet acquired Auto Scaling Group member.
     * <p/>
     * We set the request time to <code>null</code>, since AWS AutoScaling does
     * not support reporting it and attempting to keep track of it manually is
     * rather awkward and brittle.
     *
     * @param pseudoId
     *            The identifier to assign to the pseudo machine.
     * @param launchConfig
     *            The launch configuration that describes how to launch an Auto
     *            Scaling Group on-demand instance (or spot instance).
     * @return The pseudo machine.
     */
    private Machine pseudoMachine(String pseudoId, LaunchConfiguration launchConfig) {
        // are spot instances or on-demand instances being launched for the Auto
        // Scaling Group
        String cloudProvider = launchConfig.getSpotPrice() != null ? CloudProviders.AWS_SPOT : CloudProviders.AWS_EC2;
        String instanceType = launchConfig.getInstanceType();
        return Machine.builder().id(pseudoId).machineState(MachineState.REQUESTED).cloudProvider(cloudProvider)
                .region(cloudApiSettings().getRegion()).machineSize(instanceType).build();
    }

    @Override
    public List<Machine> startMachines(int count) throws StartMachinesException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        try {
            // We simply set the desired size of the scaling group without
            // waiting for the request to be fulfilled, simply because there is
            // no bulletproof method of knowing when this particular desired
            // size request has taken effect. Waiting for the group size to
            // reach the desired size is problematic, since the desired size may
            // be set to some other value while we are waiting.
            AutoScalingGroup group = this.client.getAutoScalingGroup(scalingGroupName());
            LaunchConfiguration launchConfig = this.client.getLaunchConfiguration(group.getLaunchConfigurationName());
            int newDesiredSize = group.getDesiredCapacity() + count;
            LOG.info("starting {} new instance(s) in scaling group '{}': " + "changing desired capacity from {} to {}",
                    count, scalingGroupName(), group.getDesiredCapacity(), newDesiredSize);
            this.client.setDesiredSize(scalingGroupName(), newDesiredSize);
            return pseudoMachines(count, launchConfig);
        } catch (Exception e) {
            List<Machine> empty = Collections.emptyList();
            throw new StartMachinesException(count, empty, e);
        }
    }

    @Override
    public void terminateMachines(List<String> machineIds)
            throws IllegalStateException, TerminateMachinesException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");
        List<String> victimIds = new ArrayList<>(machineIds);
        LOG.info("request to terminate instances: {}", machineIds);

        List<Machine> poolMembers = listMachines();

        // track termination failures
        Map<String, Throwable> failures = new HashMap<>();

        // only terminate pool members (error mark others)
        nonPoolMembers(machineIds, poolMembers).stream().forEach(machineId -> {
            failures.put(machineId,
                    new NotFoundException(String.format("machine %s is not a member of the pool", machineId)));
            victimIds.remove(machineId);
        });

        List<String> terminated = new ArrayList<>();
        for (String machineId : victimIds) {
            try {
                terminateMachine(machineId);
                terminated.add(machineId);
            } catch (Exception e) {
                failures.put(machineId, e);
            }
        }

        if (!failures.isEmpty()) {
            throw new TerminateMachinesException(terminated, failures);
        }
    }

    /**
     * Terminates a single machine from the Auto Scaling Group.
     *
     * @param machineId
     *
     * @throws NotFoundException
     * @throws AmazonClientException
     */
    private void terminateMachine(String machineId) throws NotFoundException, AmazonClientException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        if (machineId.startsWith(REQUESTED_ID_PREFIX)) {
            // we were asked to terminate a placeholder instance (a
            // requested, but not yet assigned, instance). just decrement
            // desiredCapacity of the group.
            AutoScalingGroup group = this.client.getAutoScalingGroup(scalingGroupName());
            int desiredSize = group.getDesiredCapacity();
            int newSize = desiredSize - 1;
            LOG.debug("termination request for placeholder instance {}, " + "reducing desiredCapacity from {} to {}",
                    machineId, desiredSize, newSize);
            this.client.setDesiredSize(scalingGroupName(), newSize);
        } else {
            LOG.info("terminating instance {}", machineId);
            this.client.terminateInstance(scalingGroupName(), machineId);
        }
    }

    @Override
    public void attachMachine(String machineId) throws NotFoundException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        try {
            this.client.attachInstance(scalingGroupName(), machineId);
        } catch (Exception e) {
            if (e instanceof NotFoundException) {
                throw e;
            }
            String message = format("failed to attach instance \"%s\": %s", machineId, e.getMessage());
            throw new CloudPoolDriverException(message, e);
        }
    }

    @Override
    public void detachMachine(String machineId) throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        // verify that machine exists in group
        getMachineOrFail(machineId);

        try {
            this.client.detachInstance(scalingGroupName(), machineId);
        } catch (Exception e) {
            String message = format("failed to detach instance \"%s\": %s", machineId, e.getMessage());
            throw new CloudPoolDriverException(message, e);
        }
    }

    @Override
    public void setServiceState(String machineId, ServiceState serviceState)
            throws NotFoundException, CloudPoolDriverException {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        // verify that machine exists in group
        getMachineOrFail(machineId);

        try {
            Tag tag = new Tag().withKey(ScalingTags.SERVICE_STATE_TAG).withValue(serviceState.name());
            this.client.tagInstance(machineId, Arrays.asList(tag));
        } catch (Exception e) {
            if (e instanceof NotFoundException) {
                throw e;
            }
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
            Tag tag = new Tag().withKey(ScalingTags.MEMBERSHIP_STATUS_TAG)
                    .withValue(JsonUtils.toString(toJson(membershipStatus)));
            this.client.tagInstance(machineId, Arrays.asList(tag));
        } catch (Exception e) {
            if (e instanceof NotFoundException) {
                throw e;
            }
            String message = format("failed to tag membership status on server \"%s\": %s", machineId, e.getMessage());
            throw new CloudPoolDriverException(message, e);
        }
    }

    /**
     * Retrieves a particular member instance from the scaling group or throws
     * an exception if it could not be found.
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
     * Returns the list of machine ids (from a given list of machine ids) that
     * are *not* members of the given pool.
     *
     * @param machineIds
     * @param pool
     * @return
     */
    private static List<String> nonPoolMembers(List<String> machineIds, List<Machine> pool) {
        return machineIds.stream().filter(machineId -> !member(machineId, pool)).collect(Collectors.toList());
    }

    /**
     * Returns <code>true</code> if the given instance id is found in the given
     * machine pool.
     *
     * @param machineId
     * @param pool
     * @return
     */
    private static boolean member(String machineId, List<Machine> pool) {
        return pool.stream().anyMatch(m -> m.getId().equals(machineId));
    }

    @Override
    public String getPoolName() {
        checkState(isConfigured(), "attempt to use unconfigured driver");

        return config().getPoolName();
    }

    /**
     * Returns the name of the managed Auto Scaling Group. If specified in
     * {@link ProvisioningTemplate}, use that name. Otherwise, default to pool
     * name.
     *
     * @return
     */
    String scalingGroupName() {
        return provisioningTemplate().getAutoScalingGroup().orElse(getPoolName());
    }

    boolean isConfigured() {
        return config() != null;
    }

    DriverConfig config() {
        return this.config;
    }

    CloudApiSettings cloudApiSettings() {
        return this.config.parseCloudApiSettings(CloudApiSettings.class);
    }

    ProvisioningTemplate provisioningTemplate() {
        return this.config.parseProvisioningTemplate(ProvisioningTemplate.class);
    }
}
