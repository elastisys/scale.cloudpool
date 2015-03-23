package com.elastisys.scale.cloudpool.openstack.driver;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.transform;
import static java.lang.String.format;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.CloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.openstack.driver.client.OpenstackClient;
import com.elastisys.scale.cloudpool.openstack.driver.config.OpenStackPoolDriverConfig;
import com.elastisys.scale.cloudpool.openstack.functions.ServerToMachine;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.schema.JsonValidator;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Atomics;
import com.google.gson.JsonObject;

/**
 * A {@link CloudPoolDriver} implementation that operates against OpenStack.
 *
 * @see BaseCloudPool
 */
public class OpenStackPoolDriver implements CloudPoolDriver {
	private static Logger LOG = LoggerFactory
			.getLogger(OpenStackPoolDriver.class);

	/**
	 * JSON Schema describing valid {@link OpenStackPoolDriverConfig} instances.
	 */
	private static final JsonObject CONFIG_SCHEMA = JsonUtils
			.parseJsonResource("openstack-pool-driver-schema.json");

	/** OpenStack client API configuration. */
	private final AtomicReference<OpenStackPoolDriverConfig> config;
	/** Logical name of the managed machine pool. */
	private final AtomicReference<String> poolName;

	/** Client used to communicate with the OpenStack API. */
	private final OpenstackClient client;

	/**
	 * Creates a new {@link OpenStackPoolDriver}. Needs to be configured before
	 * use.
	 *
	 * @param client
	 *            The client to be used to communicate with the OpenStack API.
	 */
	public OpenStackPoolDriver(OpenstackClient client) {
		this.config = Atomics.newReference();
		this.poolName = Atomics.newReference();
		this.client = client;
	}

	@Override
	public void configure(BaseCloudPoolConfig configuration)
			throws CloudPoolDriverException {
		CloudPoolConfig cloudPoolConfig = configuration.getCloudPool();
		checkArgument(cloudPoolConfig != null, "missing cloudPool config");

		try {
			// validate against client config schema
			JsonValidator.validate(CONFIG_SCHEMA,
					cloudPoolConfig.getDriverConfig());
			// parse and validate cloud login configuration
			OpenStackPoolDriverConfig config = JsonUtils.toObject(
					cloudPoolConfig.getDriverConfig(),
					OpenStackPoolDriverConfig.class);
			config.validate();
			this.config.set(config);
			this.poolName.set(cloudPoolConfig.getName());
			this.client.configure(config);
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, CloudPoolDriverException.class);
			throw new CloudPoolDriverException(
					format("failed to apply driver configuration: %s",
							e.getMessage()), e);
		}
	}

	@Override
	public List<Machine> listMachines() throws CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		try {
			List<Server> servers = this.client.getServers(
					Constants.CLOUD_POOL_TAG, getPoolName());
			return transform(servers, new ServerToMachine());
		} catch (Exception e) {
			throw new CloudPoolDriverException(format(
					"failed to retrieve machines in scaling group \"%s\": %s",
					this.poolName, e.getMessage()), e);
		}
	}

	@Override
	public List<Machine> startMachines(int count, ScaleOutConfig scaleUpConfig)
			throws StartMachinesException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		List<Machine> startedMachines = Lists.newArrayList();
		try {
			for (int i = 0; i < count; i++) {
				// tag new server with scaling group membership
				Map<String, String> tags = ImmutableMap.of(
						Constants.CLOUD_POOL_TAG, getPoolName());
				Server newServer = this.client.launchServer(uniqueServerName(),
						scaleUpConfig, tags);
				startedMachines.add(ServerToMachine.convert(newServer));

				if (config().isAssignFloatingIp()) {
					String serverId = newServer.getId();
					this.client.assignFloatingIp(serverId);
					// update meta data to include the public IP
					startedMachines.set(i, ServerToMachine.convert(this.client
							.getServer(serverId)));
				}
			}
		} catch (Exception e) {
			throw new StartMachinesException(count, startedMachines, e);
		}
		return startedMachines;
	}

	@Override
	public void terminateMachine(String machineId)
			throws CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		// verify that machine exists in group
		getMachineOrFail(machineId);

		try {
			this.client.terminateServer(machineId);
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, CloudPoolDriverException.class);
			String message = format("failed to terminate server \"%s\": %s",
					machineId, e.getMessage());
			throw new CloudPoolDriverException(message, e);
		}
	}

	@Override
	public void attachMachine(String machineId) throws NotFoundException,
			CloudPoolDriverException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		Map<String, String> tags = ImmutableMap.of(Constants.CLOUD_POOL_TAG,
				getPoolName());
		try {
			this.client.tagServer(machineId, tags);
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, NotFoundException.class);
			Throwables.propagateIfInstanceOf(e, CloudPoolDriverException.class);
			String message = format("failed to attach server \"%s\": %s",
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
			List<String> tagKeys = Arrays.asList(Constants.CLOUD_POOL_TAG);
			this.client.untagServer(machineId, tagKeys);
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, CloudPoolDriverException.class);
			String message = format("failed to detach server \"%s\": %s",
					machineId, e.getMessage());
			throw new CloudPoolDriverException(message, e);
		}
	}

	@Override
	public void setServiceState(String machineId, ServiceState serviceState)
			throws NotFoundException {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		// verify that machine exists in group
		getMachineOrFail(machineId);

		try {
			LOG.debug("service state {} reported for {}", serviceState.name(),
					machineId);
			// set serviceState as tag on machine instance
			Map<String, String> tags = ImmutableMap.of(
					Constants.SERVICE_STATE_TAG, serviceState.name());
			this.client.tagServer(machineId, tags);
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
			LOG.debug("membership status {} reported for {}", membershipStatus,
					machineId);
			// set serviceState as tag on machine instance
			Map<String, String> tags = ImmutableMap.of(
					Constants.MEMBERSHIP_STATUS_TAG,
					JsonUtils.toString(JsonUtils.toJson(membershipStatus)));
			this.client.tagServer(machineId, tags);
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, CloudPoolDriverException.class);
			String message = format(
					"failed to tag membership status on server \"%s\": %s",
					machineId, e.getMessage());
			throw new CloudPoolDriverException(message, e);
		}
	}

	@Override
	public String getPoolName() {
		checkState(isConfigured(), "attempt to use unconfigured driver");

		return this.poolName.get();
	}

	/**
	 * Retrieves a particular member machine from the scaling group or throws an
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
				"no machine with id '%s' found in scaling group", machineId));
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

	boolean isConfigured() {
		return config() != null;
	}

	OpenStackPoolDriverConfig config() {
		return this.config.get();
	}
}
