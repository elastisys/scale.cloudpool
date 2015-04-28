package com.elastisys.scale.cloudpool.aws.ec2.driver;

import static com.elastisys.scale.commons.json.JsonUtils.toJson;
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
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.CloudPoolMetadata;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolIdentifier;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.commons.ScalingFilters;
import com.elastisys.scale.cloudpool.aws.commons.ScalingTags;
import com.elastisys.scale.cloudpool.aws.commons.functions.InstanceToMachine;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2Client;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.CloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Atomics;

/**
 * A {@link CloudPoolDriver} implementation that operates against the AWS EC2
 * cloud API.
 *
 * @see BaseCloudPool
 */
public class Ec2PoolDriver implements CloudPoolDriver {

	static Logger LOG = LoggerFactory.getLogger(Ec2PoolDriver.class);

	/** {@link Ec2PoolDriver} configuration. */
	private final AtomicReference<Ec2PoolDriverConfig> config;
	/** Logical name of the managed machine pool. */
	private final AtomicReference<String> poolName;

	/** A client used to communicate with the AWS EC2 API. */
	private final Ec2Client client;

	/**
	 * Supported API versions by this implementation.
	 */
	public final static List<String> supportedApiVersions = Lists
			.<String> newArrayList("3.0", "3.1");
	/**
	 * Cloud pool metadata for this implementation.
	 */
	public final static CloudPoolMetadata cloudPoolMetadata = new CloudPoolMetadata(
			PoolIdentifier.AWS_EC2, supportedApiVersions);

	/**
	 * Creates a new {@link Ec2PoolDriver}. Needs to be configured before use.
	 *
	 * @param client
	 *            The {@link Ec2Client} used to communicate with the AWS EC2
	 *            API.
	 */
	public Ec2PoolDriver(Ec2Client client) {
		this.config = Atomics.newReference();
		this.poolName = Atomics.newReference();
		this.client = client;
	}

	@Override
	public void configure(BaseCloudPoolConfig configuration)
			throws CloudPoolDriverException {
		CloudPoolConfig poolConfig = configuration.getCloudPool();
		checkArgument(poolConfig != null, "missing cloudPool config");
		try {
			// parse and validate cloud login configuration
			Ec2PoolDriverConfig config = JsonUtils.toObject(
					poolConfig.getDriverConfig(), Ec2PoolDriverConfig.class);
			config.validate();
			this.config.set(config);
			this.poolName.set(poolConfig.getName());
			this.client.configure(config.getAwsAccessKeyId(),
					config.getAwsSecretAccessKey(), config.getRegion());
		} catch (Exception e) {
			throw new CloudPoolDriverException(format(
					"failed to apply configuration: %s", e.getMessage()), e);
		}
	}

	@Override
	public List<Machine> listMachines() throws CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured Ec2PoolDriver");

		try {
			// filter instances on cloud pool tag
			Filter filter = new Filter().withName(
					ScalingFilters.CLOUD_POOL_TAG_FILTER).withValues(
					getPoolName());
			List<Instance> instances = this.client.getInstances(asList(filter));
			return Lists.transform(instances, new InstanceToMachine());
		} catch (Exception e) {
			throw new CloudPoolDriverException(format(
					"failed to retrieve machines in cloud pool \"%s\": %s",
					getPoolName(), e.getMessage()), e);
		}
	}

	@Override
	public List<Machine> startMachines(int count, ScaleOutConfig scaleUpConfig)
			throws StartMachinesException {
		checkState(isConfigured(), "attempt to use unconfigured Ec2PoolDriver");

		List<Machine> startedMachines = Lists.newArrayList();
		try {
			for (int i = 0; i < count; i++) {
				Instance newInstance = launchInstance(scaleUpConfig);
				startedMachines.add(InstanceToMachine.convert(newInstance));

				// set cloud pool tag to make sure machine is recognized as a
				// cloud pool member
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
	public void terminateMachine(String machineId)
			throws CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured Ec2PoolDriver");

		// verify that machine exists in group
		getMachineOrFail(machineId);

		try {
			LOG.info("terminating instance {}", machineId);
			this.client.terminateInstance(machineId);
		} catch (Exception e) {
			String message = format("failed to terminate instance \"%s\": %s",
					machineId, e.getMessage());
			throw new CloudPoolDriverException(message, e);
		}
	}

	@Override
	public void attachMachine(String machineId) throws NotFoundException,
			CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured Ec2PoolDriver");

		// verify that machine exists
		this.client.getInstanceMetadata(machineId);

		try {
			Tag tag = createTag(ScalingTags.CLOUD_POOL_TAG, getPoolName());
			tagInstance(machineId, Arrays.asList(tag));
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, NotFoundException.class);
			throw new CloudPoolDriverException(String.format(
					"failed to attach '%s' to cloud pool: %s", machineId,
					e.getMessage()), e);
		}
	}

	@Override
	public void detachMachine(String machineId) throws NotFoundException,
			CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured Ec2PoolDriver");

		// verify that machine exists in group
		getMachineOrFail(machineId);

		Tag membershipTag = createTag(ScalingTags.CLOUD_POOL_TAG, getPoolName());
		try {
			this.client.untagResource(machineId, asList(membershipTag));
		} catch (Exception e) {
			throw new CloudPoolDriverException(String.format(
					"failed to remove tag '%s' from instance %s: %s",
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
			Tag tag = createTag(ScalingTags.SERVICE_STATE_TAG,
					serviceState.name());
			tagInstance(machineId, Arrays.asList(tag));
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, NotFoundException.class);
			throw new CloudPoolDriverException(String.format(
					"failed to set service state for instance %s: %s",
					machineId, e.getMessage()), e);
		}
	}

	@Override
	public void setMembershipStatus(String machineId,
			MembershipStatus membershipStatus) throws NotFoundException,
			CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured Ec2PoolDriver");

		// verify that machine exists in group
		getMachineOrFail(machineId);

		try {
			Tag tag = createTag(ScalingTags.MEMBERSHIP_STATUS_TAG,
					JsonUtils.toString(toJson(membershipStatus)));
			tagInstance(machineId, Arrays.asList(tag));
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, NotFoundException.class);
			throw new CloudPoolDriverException(String.format(
					"failed to set membership status for instance %s: %s",
					machineId, e.getMessage()), e);
		}

	}

	@Override
	public String getPoolName() {
		checkState(isConfigured(), "attempt to use unconfigured Ec2PoolDriver");
		return this.poolName.get();
	}

	@Override
	public CloudPoolMetadata getMetadata() {
		return cloudPoolMetadata;
	}

	boolean isConfigured() {
		return config() != null;
	}

	Ec2PoolDriverConfig config() {
		return this.config.get();
	}

	private Instance launchInstance(ScaleOutConfig scaleUpConfig)
			throws CloudPoolDriverException {
		try {
			return this.client.launchInstance(scaleUpConfig);
		} catch (Exception e) {
			throw new CloudPoolDriverException(format(
					"failed to launch instance: %s", e.getMessage()), e);
		}
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

		throw new NotFoundException(String.format(
				"no machine with id '%s' found in cloud pool", machineId));
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
		String instanceName = String.format("%s-%s", getPoolName(),
				instance.getInstanceId());
		Tag nameTag = createTag(ScalingTags.INSTANCE_NAME_TAG, instanceName);

		// tag new instance with cloud pool
		Tag cloudPoolTag = createTag(ScalingTags.CLOUD_POOL_TAG, getPoolName());
		return Arrays.asList(nameTag, cloudPoolTag);
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
	private Instance tagInstance(String instanceId, List<Tag> tags)
			throws NotFoundException, CloudPoolDriverException {
		this.client.tagResource(instanceId, tags);
		return this.client.getInstanceMetadata(instanceId);
	}

}
