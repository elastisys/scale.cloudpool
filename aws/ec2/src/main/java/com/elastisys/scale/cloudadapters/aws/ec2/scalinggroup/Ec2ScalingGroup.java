package com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.aws.ec2.functions.InstanceToMachine;
import com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.client.Ec2Client;
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
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Atomics;
import com.google.gson.JsonObject;

/**
 * A {@link ScalingGroup} implementation that operates against the AWS EC2 cloud
 * API.
 *
 * @see BaseCloudAdapter
 *
 *
 *
 */
public class Ec2ScalingGroup implements ScalingGroup {

	/** JSON Schema describing valid {@link Ec2ScalingGroupConfig} instances. */
	private static final JsonObject JSON_SCHEMA = JsonUtils
			.parseJsonResource("ec2-scaling-group-schema.json");

	static Logger LOG = LoggerFactory.getLogger(Ec2ScalingGroup.class);

	/** Scaling Group configuration. */
	private final AtomicReference<Ec2ScalingGroupConfig> config;
	/** Name of the managed scaling group. */
	private final AtomicReference<String> scalingGroupName;

	/** A client used to communicate with the AWS EC2 API. */
	private final Ec2Client client;

	/**
	 * Creates a new {@link OpenStackScalingGroup}. Needs to be configured
	 * before use.
	 *
	 * @param client
	 *            The {@link Ec2Client} used to communicate with the AWS EC2
	 *            API.
	 */
	public Ec2ScalingGroup(Ec2Client client) {
		this.config = Atomics.newReference();
		this.scalingGroupName = Atomics.newReference();
		this.client = client;
	}

	@Override
	public void configure(BaseCloudAdapterConfig configuration)
			throws ScalingGroupException {
		ScalingGroupConfig scalingGroupConfig = configuration.getScalingGroup();
		checkArgument(scalingGroupConfig != null, "missing scalingGroup config");
		try {
			// validate against client config schema
			JsonValidator.validate(JSON_SCHEMA, scalingGroupConfig.getConfig());
			// parse and validate cloud login configuration
			Ec2ScalingGroupConfig config = JsonUtils
					.toObject(scalingGroupConfig.getConfig(),
							Ec2ScalingGroupConfig.class);
			config.validate();
			this.config.set(config);
			this.scalingGroupName.set(scalingGroupConfig.getName());
			this.client.configure(config);
		} catch (Exception e) {
			throw new ScalingGroupException(format(
					"failed to apply configuration: %s", e.getMessage()), e);
		}
	}

	@Override
	public List<Machine> listMachines() throws ScalingGroupException {
		checkState(isConfigured(), "attempt to use unconfigured ScalingGroup");

		try {
			// filter instances on scaling group tag
			Filter filter = new Filter().withName(
					Constants.SCALING_GROUP_TAG_FILTER_KEY).withValues(
					getScalingGroupName());
			List<Instance> instances = this.client.getInstances(asList(filter));
			return Lists.transform(instances, new InstanceToMachine());
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
			for (int i = 0; i < count; i++) {
				Instance newInstance = launchInstance(scaleUpConfig);
				startedMachines.add(InstanceToMachine.convert(newInstance));

				newInstance = awaitIpAddress(newInstance);
				startedMachines.set(i, InstanceToMachine.convert(newInstance));

				// set scaling group tag to make sure machine is recognized as a
				// scaling group member
				newInstance = tagInstance(newInstance, getScalingGroupName());
				startedMachines.set(i, InstanceToMachine.convert(newInstance));

			}
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
			this.client.terminateInstance(machineId);
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

	Ec2ScalingGroupConfig config() {
		return this.config.get();
	}

	private Instance launchInstance(ScaleUpConfig scaleUpConfig)
			throws ScalingGroupException {
		try {
			Instance launchedInstance = this.client
					.launchInstance(scaleUpConfig);
			// refresh meta data
			return this.client.getInstanceMetadata(launchedInstance
					.getInstanceId());
		} catch (Exception e) {
			throw new ScalingGroupException(format(
					"failed to launch instance: %s", e.getMessage()), e);
		}
	}

	/**
	 * Waits for a newly created instance to be assigned a public IP address.
	 *
	 * @param instance
	 * @return
	 * @return Updated meta data about the {@link Instance}, with its public IP
	 *         address set.
	 * @throws ScalingGroupException
	 */
	private Instance awaitIpAddress(Instance instance)
			throws ScalingGroupException {
		try {
			LOG.debug("waiting for '{}' to be assigned a public IP address",
					instance.getInstanceId());
			String taskName = String.format("ip-address-waiter{%s}",
					instance.getInstanceId());
			RetryableRequest<String> awaitIp = new RetryableRequest<>(
					new InstanceIpGetter(this.client, instance.getInstanceId()),
					new RetryUntilNoException<String>(12, 10000), taskName);
			String ipAddress = awaitIp.call();
			LOG.debug("instance was assigned public IP address {}", ipAddress);
			// re-read instance meta data
			return this.client.getInstanceMetadata(instance.getInstanceId());
		} catch (Exception e) {
			throw new ScalingGroupException(
					format("gave up waiting for instance \"%s\" to be assigned a public IP address: %s",
							instance.getInstanceId(), e.getMessage()), e);
		}
	}

	/**
	 * Tag a started instance with the {@link Constants#SCALING_GROUP_TAG}.
	 *
	 * @param instance
	 *            The instance to tag.
	 * @return Returns updated meta data about the {@link Instance}.
	 * @throws ScalingGroupException
	 */
	private Instance tagInstance(Instance instance, String scalingGroup)
			throws ScalingGroupException {
		// assign a name to the instance via the Name tag
		String instanceName = String.format("%s-%s", scalingGroup,
				instance.getInstanceId());
		Tag nameTag = new Tag().withKey(Constants.NAME_TAG).withValue(
				instanceName);

		// tag new instance with scaling group
		Tag scalingGroupTag = new Tag().withKey(Constants.SCALING_GROUP_TAG)
				.withValue(getScalingGroupName());

		try {
			this.client.tagInstance(instance.getInstanceId(),
					Arrays.asList(nameTag, scalingGroupTag));
		} catch (Exception e) {
			throw new ScalingGroupException(String.format(
					"failed to set \"%s\" tag on instance \"%s\": %s",
					Constants.SCALING_GROUP_TAG, instance.getInstanceId(),
					e.getMessage()), e);
		}
		return this.client.getInstanceMetadata(instance.getInstanceId());
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

		private final Ec2Client client;
		private final String instanceId;

		public InstanceIpGetter(Ec2Client client, String instanceId) {
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
