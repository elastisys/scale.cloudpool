package com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jersey.repackaged.com.google.common.base.Throwables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudadapers.api.NotFoundException;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.ServiceState;
import com.elastisys.scale.cloudadapters.aws.commons.ScalingTags;
import com.elastisys.scale.cloudadapters.aws.commons.functions.InstanceToMachine;
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

				// set scaling group tag to make sure machine is recognized as a
				// scaling group member
				List<Tag> tags = instanceTags(newInstance);
				newInstance = tagInstance(newInstance.getInstanceId(), tags);
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

		// verify that machine exists in group
		getMachineOrFail(machineId);

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
	public void attachMachine(String machineId) throws NotFoundException,
			ScalingGroupException {
		checkState(isConfigured(), "attempt to use unconfigured ScalingGroup");

		try {
			Tag tag = createTag(ScalingTags.SCALING_GROUP_TAG,
					getScalingGroupName());
			tagInstance(machineId, Arrays.asList(tag));
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, NotFoundException.class);
			throw new ScalingGroupException(String.format(
					"failed to attach '%s' to scaling group: %s", machineId,
					e.getMessage()), e);
		}
	}

	@Override
	public void detachMachine(String machineId) throws NotFoundException,
			ScalingGroupException {
		checkState(isConfigured(), "attempt to use unconfigured ScalingGroup");

		// verify that machine exists in group
		getMachineOrFail(machineId);

		Tag membershipTag = createTag(ScalingTags.SCALING_GROUP_TAG,
				getScalingGroupName());
		try {
			this.client.untagInstance(machineId, asList(membershipTag));
		} catch (Exception e) {
			throw new ScalingGroupException(String.format(
					"failed to remove tag '%s' from instance %s: %s",
					membershipTag, machineId, e.getMessage()), e);
		}
	}

	@Override
	public void setServiceState(String machineId, ServiceState serviceState)
			throws NotFoundException, ScalingGroupException {
		checkState(isConfigured(), "attempt to use unconfigured ScalingGroup");

		// verify that machine exists in group
		getMachineOrFail(machineId);

		try {
			Tag tag = createTag(ScalingTags.SERVICE_STATE_TAG,
					serviceState.name());
			tagInstance(machineId, Arrays.asList(tag));
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, NotFoundException.class);
			throw new ScalingGroupException(String.format(
					"failed to set service state for instance %s: %s",
					machineId, e.getMessage()), e);
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
			return this.client.launchInstance(scaleUpConfig);
		} catch (Exception e) {
			throw new ScalingGroupException(format(
					"failed to launch instance: %s", e.getMessage()), e);
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

	private Tag createTag(String key, String value) {
		return new Tag().withKey(key).withValue(value);
	}

	/**
	 * Creates tags to assign to a new {@link Instance}.
	 *
	 * @param instance
	 * @return
	 */
	private List<Tag> instanceTags(Instance instance) {
		// assign a name to the instance
		String instanceName = String.format("%s-%s", getScalingGroupName(),
				instance.getInstanceId());
		Tag nameTag = createTag(Constants.NAME_TAG, instanceName);

		// tag new instance with scaling group
		Tag scalingGroupTag = createTag(ScalingTags.SCALING_GROUP_TAG,
				getScalingGroupName());
		return Arrays.asList(nameTag, scalingGroupTag);
	}

	/**
	 * Set tags on a started instance.
	 *
	 * @param instanceId
	 *            The id of the instance to tag.
	 * @param tags
	 *            The tags to set on the instance.
	 * @return Returns updated meta data about the {@link Instance}. throws
	 *         {@link NotFoundException}
	 * @throws ScalingGroupException
	 */
	private Instance tagInstance(String instanceId, List<Tag> tags)
			throws NotFoundException, ScalingGroupException {
		this.client.tagInstance(instanceId, tags);
		return this.client.getInstanceMetadata(instanceId);
	}

}
