package com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup;

import static com.elastisys.scale.cloudadapters.aws.commons.functions.AwsEc2Functions.toInstanceId;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.transform;
import static java.lang.String.format;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.ec2.model.Instance;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.client.AutoScalingClient;
import com.elastisys.scale.cloudadapters.aws.commons.functions.InstanceToMachine;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapter;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScalingGroupConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroup;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.StartMachinesException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.schema.JsonValidator;
import com.elastisys.scale.commons.net.retryable.Requester;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;
import com.elastisys.scale.commons.net.retryable.retryhandlers.RetryUntilNoException;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Atomics;
import com.google.gson.JsonObject;

/**
 * A {@link ScalingGroup} implementation that manages an AWS Auto Scaling Group
 * over the AWS Auto Scaling API.
 * <p/>
 * This client assumes that an AWS <i>Auto Scaling group</i> with a proper
 * <i>launch configuration</i> (specifying how new instances are to be created)
 * has already been created by external means (for instance, through the AWS
 * Auto Scaling command-line interface). Load-balancing between instances, if
 * needed, is also assumed to be taken care of, either via Elastic Load Balancer
 * or with a custom-made load balancing solution.
 *
 * @see BaseCloudAdapter
 *
 * 
 *
 */
public class AwsAsScalingGroup implements ScalingGroup {
	static Logger LOG = LoggerFactory.getLogger(AwsAsScalingGroup.class);

	/**
	 * JSON Schema describing valid {@link AwsAsScalingGroupConfig}instances.
	 */
	private static final JsonObject CONFIG_SCHEMA = JsonUtils
			.parseJsonResource("awsas-scaling-group-schema.json");

	/** AWS AutoScaling client API configuration. */
	private final AtomicReference<AwsAsScalingGroupConfig> config;
	/** Name of the managed scaling group. */
	private final AtomicReference<String> scalingGroupName;

	/** A client used to communicate with the AWS Auto Scaling API. */
	private final AutoScalingClient client;

	/**
	 * Creates a new {@link AwsAsScalingGroup}. Needs to be configured before
	 * use.
	 *
	 * @param client
	 *            A client used to communicate with the AWS Auto Scaling API.
	 */
	public AwsAsScalingGroup(AutoScalingClient client) {
		this.config = Atomics.newReference();
		this.scalingGroupName = Atomics.newReference();

		this.client = client;
	}

	@Override
	public void configure(BaseCloudAdapterConfig configuration)
			throws ScalingGroupException {
		checkArgument(configuration != null, "config cannot be null");
		ScalingGroupConfig scalingGroup = configuration.getScalingGroup();
		checkArgument(scalingGroup != null, "missing scalingGroup config");

		try {
			// validate against client config schema
			JsonValidator.validate(CONFIG_SCHEMA, scalingGroup.getConfig());
			// parse and validate cloud login configuration
			AwsAsScalingGroupConfig config = JsonUtils.toObject(
					scalingGroup.getConfig(), AwsAsScalingGroupConfig.class);
			config.validate();
			this.config.set(config);
			this.scalingGroupName.set(scalingGroup.getName());
			this.client.configure(config);
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, ScalingGroupException.class);
			throw new ScalingGroupException(String.format(
					"failed to apply configuration: %s", e.getMessage()));
		}
	}

	@Override
	public List<Machine> listMachines() throws ScalingGroupException {
		checkState(isConfigured(), "attempt to use unconfigured ScalingGroup");

		try {
			// The desired capacity of the AWS Auto Scaling Group is just the
			// requested size and may differ from the actual number of instances
			// in the pool, for example when there is a shortage of instances.
			// If the desiredCapacity of the Auto Scaling Group is greater than
			// the actual number of instances in the group, we should return
			// dummy Machines in REQUESTED state for the missing instances. This
			// prevents the BaseCloudAdapter from regarding the scaling group
			// too small and ordering new machines via startMachines.
			AutoScalingGroup autoScalingGroup = this.client
					.getAutoScalingGroup(getScalingGroupName());
			int desiredCapacity = autoScalingGroup.getDesiredCapacity();
			int actualCapacity = autoScalingGroup.getInstances().size();
			LOG.debug("desiredCapacity: {}, actual capacity: {}",
					desiredCapacity, actualCapacity);
			List<Machine> requestedInstances = Lists.newArrayList();
			int missingInstances = desiredCapacity - actualCapacity;
			if (missingInstances > 0) {
				for (int i = 1; i <= missingInstances; i++) {
					String pseudoId = String.format("i-requested%d", i);
					requestedInstances.add(new Machine(pseudoId,
							MachineState.REQUESTED, null, null, null, null));
				}
			}

			// retrieve all scaling group members
			List<Instance> groupInstances = this.client
					.getAutoScalingGroupMembers(getScalingGroupName());
			List<Machine> acquiredInstances = Lists.newArrayList(transform(
					groupInstances, new InstanceToMachine()));

			List<Machine> group = Lists.newArrayList();
			group.addAll(acquiredInstances);
			group.addAll(requestedInstances);
			LOG.debug("scaling group members: {}", group);
			return group;
		} catch (Exception e) {
			throw new ScalingGroupException(format(
					"failed to retrieve machines in scaling group \"%s\": %s",
					getScalingGroupName(), e.getMessage()), e);
		}
	}

	@Override
	public List<Machine> startMachines(int count, ScaleUpConfig scaleUpConfig)
			throws StartMachinesException {
		checkState(isConfigured(), "attempt to use unconfigured ScalingGroup");

		List<Machine> startedMachines = Lists.newArrayList();
		try {
			// get current group membership: {G}
			AutoScalingGroup group = this.client
					.getAutoScalingGroup(getScalingGroupName());
			List<Instance> initialGroup = this.client
					.getAutoScalingGroupMembers(getScalingGroupName());
			LOG.debug("initial group: {}", initialGroup);
			// increase desiredCapacity and wait for group to reach new size
			int newDesiredSize = group.getDesiredCapacity() + count;
			LOG.info("starting {} new instance(s) in scaling group '{}': "
					+ "changing desired capacity from {} to {}", count,
					getScalingGroupName(), group.getDesiredCapacity(),
					newDesiredSize);
			this.client.setDesiredSize(getScalingGroupName(), newDesiredSize);

			// get new group membership: {N}
			List<Instance> expandedGroup = this.client
					.getAutoScalingGroupMembers(getScalingGroupName());
			LOG.debug("group after raising desired capacity: {}", expandedGroup);
			// new member instance(s): {N} - {G}
			List<Instance> startedInstances = difference(expandedGroup,
					initialGroup);
			LOG.debug("started instances: {}", startedInstances);
			startedMachines = transform(startedInstances,
					new InstanceToMachine());

			// await new instance(s) being assigned an IP address
			List<Instance> membersWithIp = Lists.newArrayList();
			for (Instance createdInstance : startedInstances) {
				membersWithIp.add(awaitIpAddress(createdInstance));
			}
			startedMachines = transform(membersWithIp, new InstanceToMachine());
		} catch (Exception e) {
			throw new StartMachinesException(count, startedMachines, e);
		}

		return startedMachines;
	}

	@Override
	public void terminateMachine(String machineId) throws ScalingGroupException {
		checkState(isConfigured(), "attempt to use unconfigured ScalingGroup");

		try {
			LOG.info("terminating instance {}", machineId);
			this.client.terminateInstance(getScalingGroupName(), machineId);
		} catch (Exception e) {
			String message = format("failed to terminate instance \"%s\": %s",
					machineId, e.getMessage());
			throw new ScalingGroupException(message);
		}
	}

	@Override
	public String getScalingGroupName() {
		checkState(isConfigured(), "attempt to use unconfigured ScalingGroup");

		return this.scalingGroupName.get();
	}

	boolean isConfigured() {
		return config() != null;
	}

	AwsAsScalingGroupConfig config() {
		return this.config.get();
	}

	/**
	 * Returns the set difference between two instance collections. That is, all
	 * instances in group1 that are <i>not</i> also in group2.
	 *
	 * @param group1
	 * @param group2
	 * @return
	 */
	private List<Instance> difference(List<Instance> group1,
			List<Instance> group2) {
		List<String> group1Ids = Lists.transform(group1, toInstanceId());
		LOG.debug("group1 ids: {}", group1Ids);
		List<String> group2Ids = Lists.transform(group2, toInstanceId());
		LOG.debug("group2 ids: {}", group2Ids);
		Set<String> uniqueGroup1Ids = Sets.difference(
				Sets.newHashSet(group1Ids), Sets.newHashSet(group2Ids));
		LOG.debug("new instance ids: {}", uniqueGroup1Ids);

		List<Instance> uniqueGroup1Members = Lists.newArrayList();
		for (Instance instance : group1) {
			if (uniqueGroup1Ids.contains(instance.getInstanceId())) {
				uniqueGroup1Members.add(instance);
			}
		}
		return uniqueGroup1Members;
	}

	/**
	 * Waits for a newly created instance to be assigned a public IP address.
	 *
	 * @param instance
	 * @return
	 * @return Returns updated meta data for the {@link Instance}, with its
	 *         public IP address set.
	 * @throws ScalingGroupException
	 */
	private Instance awaitIpAddress(Instance instance)
			throws ScalingGroupException {
		String instanceId = instance.getInstanceId();
		try {
			LOG.info("waiting for '{}' to be assigned a public IP address",
					instanceId);
			String taskName = String
					.format("ip-address-waiter{%s}", instanceId);
			RetryableRequest<String> awaitIp = new RetryableRequest<>(
					new InstanceIpGetter(this.client, instanceId),
					new RetryUntilNoException<String>(12, 10000), taskName);
			String ipAddress = awaitIp.call();
			LOG.info("instance was assigned public IP address {}", ipAddress);
			// re-read instance meta data
			return this.client.getInstanceMetadata(instanceId);
		} catch (Exception e) {
			throw new ScalingGroupException(format(
					"gave up waiting for instance \"%s\" to be "
							+ "assigned a public IP address: %s", instanceId,
					e.getMessage()), e);
		}
	}

	/**
	 * Simple {@link Requester} that fetches the public IP address of a certain
	 * instance if one has been assigned. If the instance doesn't have a public
	 * IP address, a {@link IllegalStateException} is thrown.
	 *
	 * 
	 *
	 */
	public static class InstanceIpGetter implements Requester<String> {

		private final AutoScalingClient client;
		private final String instanceId;

		public InstanceIpGetter(AutoScalingClient client, String instanceId) {
			this.client = client;
			this.instanceId = instanceId;
		}

		@Override
		public String call() throws Exception {
			Instance instance = this.client
					.getInstanceMetadata(this.instanceId);
			checkState(instance.getPublicIpAddress() != null,
					"instance has not been assigned a public IP address");
			return instance.getPublicIpAddress();
		}
	}
}
