package com.elastisys.scale.cloudpool.aws.autoscaling.driver;

import static com.elastisys.scale.commons.json.JsonUtils.toJson;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.transform;
import static java.lang.String.format;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.client.AutoScalingClient;
import com.elastisys.scale.cloudpool.aws.commons.ScalingTags;
import com.elastisys.scale.cloudpool.aws.commons.functions.InstanceToMachine;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.CloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Atomics;

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

	/** AWS AutoScaling client API configuration. */
	private final AtomicReference<AwsAsPoolDriverConfig> config;
	/** Logical name of the managed machine pool. */
	private final AtomicReference<String> poolName;

	/** A client used to communicate with the AWS Auto Scaling API. */
	private final AutoScalingClient client;

	/**
	 * Creates a new {@link AwsAsPoolDriver}. Needs to be configured before use.
	 *
	 * @param client
	 *            A client used to communicate with the AWS Auto Scaling API.
	 */
	public AwsAsPoolDriver(AutoScalingClient client) {
		this.config = Atomics.newReference();
		this.poolName = Atomics.newReference();

		this.client = client;
	}

	@Override
	public void configure(BaseCloudPoolConfig configuration)
			throws CloudPoolDriverException {
		checkArgument(configuration != null, "config cannot be null");
		CloudPoolConfig poolConfig = configuration.getCloudPool();
		checkArgument(poolConfig != null, "missing cloudPool config");

		try {
			// parse and validate cloud login configuration
			AwsAsPoolDriverConfig config = JsonUtils.toObject(
					poolConfig.getDriverConfig(), AwsAsPoolDriverConfig.class);
			config.validate();
			this.config.set(config);
			this.poolName.set(poolConfig.getName());
			this.client.configure(config);
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, CloudPoolDriverException.class);
			throw new CloudPoolDriverException(String.format(
					"failed to apply configuration: %s", e.getMessage()));
		}
	}

	@Override
	public List<Machine> listMachines() throws CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		try {
			AutoScalingGroup group = this.client
					.getAutoScalingGroup(getPoolName());
			// requested, but not yet allocated, machines
			List<Machine> requestedInstances = requestedInstances(group
					.getInstances().size(), group.getDesiredCapacity());
			// actual scaling group members
			List<Instance> groupInstances = this.client
					.getAutoScalingGroupMembers(getPoolName());
			List<Machine> acquiredMachines = Lists.newArrayList(transform(
					groupInstances, new InstanceToMachine()));

			List<Machine> pool = Lists.newArrayList();
			pool.addAll(acquiredMachines);
			pool.addAll(requestedInstances);
			return pool;
		} catch (Exception e) {
			throw new CloudPoolDriverException(format(
					"failed to retrieve machines in scaling group \"%s\": %s",
					getPoolName(), e.getMessage()), e);
		}
	}

	/**
	 * Produces a number of placeholder {@link Machine}s (in {@code REQUESTED}
	 * state) for requested, but not yet acquired, instances in an Auto Scaling
	 * Group. The number of produced placeholder instances is the the difference
	 * between {@code desiredCapacity} and {@code actualCapacity}.
	 * <p/>
	 * Rationale: the desired capacity of the AWS Auto Scaling Group may differ
	 * from the actual number of instances in the group. If the desiredCapacity
	 * of the Auto Scaling Group is greater than the actual number of instances
	 * in the group, we should return placeholder Machines in REQUESTED state
	 * for the missing instances. This prevents the {@link BaseCloudPool} from
	 * regarding the scaling group too small and ordering new machines via
	 * startMachines.
	 *
	 * @param actualCapacity
	 *            The actual scaling group size.
	 * @param desiredCapacity
	 *            The desired scaling group size.
	 * @return
	 */
	private List<Machine> requestedInstances(int actualCapacity,
			int desiredCapacity) {
		int missingInstances = desiredCapacity - actualCapacity;

		List<Machine> requestedInstances = Lists.newArrayList();
		for (int i = 0; i < missingInstances; i++) {
			String pseudoId = String.format("%s%d", REQUESTED_ID_PREFIX,
					(i + 1));
			requestedInstances.add(new Machine(pseudoId,
					MachineState.REQUESTED, ServiceState.UNKNOWN, null, null,
					null));
		}
		return requestedInstances;
	}

	@Override
	public List<Machine> startMachines(int count, ScaleOutConfig scaleUpConfig)
			throws StartMachinesException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		List<Machine> requestedMachines = Lists.newArrayList();
		try {
			// We simply set the desired size of the scaling group without
			// waiting for the request to be fulfilled, simply because there is
			// no bulletproof method of knowing when this particular desired
			// size request has taken effect. Waiting for the group size to
			// reach the desired size is problematic, since the desired size may
			// be set to some other value while we are waiting.
			AutoScalingGroup group = this.client
					.getAutoScalingGroup(getPoolName());
			int newDesiredSize = group.getDesiredCapacity() + count;
			LOG.info("starting {} new instance(s) in scaling group '{}': "
					+ "changing desired capacity from {} to {}", count,
					getPoolName(), group.getDesiredCapacity(), newDesiredSize);
			this.client.setDesiredSize(getPoolName(), newDesiredSize);
			requestedMachines = requestedInstances(group.getInstances().size(),
					newDesiredSize);
		} catch (Exception e) {
			throw new StartMachinesException(count, requestedMachines, e);
		}

		return requestedMachines;
	}

	@Override
	public void terminateMachine(String machineId)
			throws CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		// verify that machine exists in group
		getMachineOrFail(machineId);
		try {
			if (machineId.startsWith(REQUESTED_ID_PREFIX)) {
				// we were asked to terminate a placeholder instance (a
				// requested, but not yet assigned, instance). just decrement
				// desiredCapacity of the group.
				AutoScalingGroup group = this.client
						.getAutoScalingGroup(getPoolName());
				int desiredSize = group.getDesiredCapacity();
				int newSize = desiredSize - 1;
				LOG.debug("termination request for placeholder instance {}, "
						+ "reducing desiredCapacity from {} to {}", machineId,
						desiredSize, newSize);
				this.client.setDesiredSize(getPoolName(), newSize);
			} else {
				LOG.info("terminating instance {}", machineId);
				this.client.terminateInstance(getPoolName(), machineId);
			}
		} catch (Exception e) {
			String message = format("failed to terminate instance \"%s\": %s",
					machineId, e.getMessage());
			throw new CloudPoolDriverException(message, e);
		}
	}

	@Override
	public void attachMachine(String machineId) throws NotFoundException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		try {
			this.client.attachInstance(getPoolName(), machineId);
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, NotFoundException.class);
			String message = format("failed to attach instance \"%s\": %s",
					machineId, e.getMessage());
			throw new CloudPoolDriverException(message, e);
		}
	}

	@Override
	public void detachMachine(String machineId) throws NotFoundException,
			CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		// verify that machine exists in group
		getMachineOrFail(machineId);

		try {
			this.client.detachInstance(getPoolName(), machineId);
		} catch (Exception e) {
			String message = format("failed to detach instance \"%s\": %s",
					machineId, e.getMessage());
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
			Tag tag = new Tag().withKey(ScalingTags.SERVICE_STATE_TAG)
					.withValue(serviceState.name());
			this.client.tagInstance(machineId, Arrays.asList(tag));
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, CloudPoolDriverException.class);
			String message = format(
					"failed to tag service state on server \"%s\": %s",
					machineId, e.getMessage());
			throw new CloudPoolDriverException(message, e);
		}
	}

	@Override
	public void setMembershipStatus(String machineId,
			MembershipStatus membershipStatus) throws NotFoundException,
			CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		// verify that machine exists in group
		getMachineOrFail(machineId);

		try {
			Tag tag = new Tag().withKey(ScalingTags.MEMBERSHIP_STATUS_TAG)
					.withValue(JsonUtils.toString(toJson(membershipStatus)));
			this.client.tagInstance(machineId, Arrays.asList(tag));
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, CloudPoolDriverException.class);
			String message = format(
					"failed to tag membership status on server \"%s\": %s",
					machineId, e.getMessage());
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

		throw new NotFoundException(String.format(
				"no machine with id '%s' found in scaling group", machineId));
	}

	@Override
	public String getPoolName() {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		return this.poolName.get();
	}

	boolean isConfigured() {
		return config() != null;
	}

	AwsAsPoolDriverConfig config() {
		return this.config.get();
	}
}
