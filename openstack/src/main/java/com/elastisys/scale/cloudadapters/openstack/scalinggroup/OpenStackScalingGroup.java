package com.elastisys.scale.cloudadapters.openstack.scalinggroup;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.transform;
import static java.lang.String.format;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.NotFoundException;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.ServiceState;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapter;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScalingGroupConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroup;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.StartMachinesException;
import com.elastisys.scale.cloudadapters.openstack.functions.ServerToMachine;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.client.OpenstackClient;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.schema.JsonValidator;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Atomics;
import com.google.gson.JsonObject;

/**
 * A {@link ScalingGroup} implementation that operates against the OpenStack
 * cloud API.
 *
 * @see BaseCloudAdapter
 *
 *
 *
 */
public class OpenStackScalingGroup implements ScalingGroup {

	/**
	 * JSON Schema describing valid {@link OpenStackScalingGroupConfig}
	 * instances.
	 */
	private static final JsonObject CONFIG_SCHEMA = JsonUtils
			.parseJsonResource("openstack-scaling-group-schema.json");

	static Logger LOG = LoggerFactory.getLogger(OpenStackScalingGroup.class);

	/** OpenStack client API configuration. */
	private final AtomicReference<OpenStackScalingGroupConfig> config;
	/** Name of the managed scaling group. */
	private final AtomicReference<String> scalingGroupName;

	/** Client used to communicate with the OpenStack API. */
	private final OpenstackClient client;

	/**
	 * Creates a new {@link OpenStackScalingGroup}. Needs to be configured
	 * before use.
	 *
	 * @param client
	 *            The client to be used to communicate with the OpenStack API.
	 */
	public OpenStackScalingGroup(OpenstackClient client) {
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
			JsonValidator.validate(CONFIG_SCHEMA,
					scalingGroupConfig.getConfig());
			// parse and validate cloud login configuration
			OpenStackScalingGroupConfig config = JsonUtils.toObject(
					scalingGroupConfig.getConfig(),
					OpenStackScalingGroupConfig.class);
			config.validate();
			this.config.set(config);
			this.scalingGroupName.set(scalingGroupConfig.getName());
			this.client.configure(config);
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, ScalingGroupException.class);
			throw new ScalingGroupException(String.format(
					"failed to apply configuration: %s", e.getMessage()), e);
		}
	}

	@Override
	public List<Machine> listMachines() throws ScalingGroupException {
		checkState(isConfigured(), "attempt to use unconfigured ScalingGroup");

		try {
			List<Server> servers = this.client.getServers(
					Constants.SCALING_GROUP_TAG, getScalingGroupName());
			return transform(servers, new ServerToMachine());
		} catch (Exception e) {
			throw new ScalingGroupException(format(
					"failed to retrieve machines in scaling group \"%s\": %s",
					this.scalingGroupName, e.getMessage()), e);
		}
	}

	@Override
	public List<Machine> startMachines(int count, ScaleUpConfig scaleUpConfig)
			throws StartMachinesException {
		checkState(isConfigured(), "attempt to use unconfigured ScalingGroup");

		List<Machine> startedMachines = Lists.newArrayList();
		try {
			for (int i = 0; i < count; i++) {
				// tag new server with scaling group membership
				Map<String, String> tags = ImmutableMap.of(
						Constants.SCALING_GROUP_TAG, getScalingGroupName());
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
	public void terminateMachine(String machineId) throws ScalingGroupException {
		checkState(isConfigured(), "attempt to use unconfigured ScalingGroup");

		// verify that machine exists in group
		getMachineOrFail(machineId);

		try {
			this.client.terminateServer(machineId);
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, ScalingGroupException.class);
			String message = format("failed to terminate server \"%s\": %s",
					machineId, e.getMessage());
			throw new ScalingGroupException(message, e);
		}
	}

	@Override
	public void attachMachine(String machineId) throws NotFoundException,
	ScalingGroupException {
		checkState(isConfigured(), "attempt to use unconfigured ScalingGroup");

		Map<String, String> tags = ImmutableMap.of(Constants.SCALING_GROUP_TAG,
				getScalingGroupName());
		try {
			this.client.tagServer(machineId, tags);
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, NotFoundException.class);
			Throwables.propagateIfInstanceOf(e, ScalingGroupException.class);
			String message = format("failed to attach server \"%s\": %s",
					machineId, e.getMessage());
			throw new ScalingGroupException(message, e);
		}

	}

	@Override
	public void detachMachine(String machineId) throws NotFoundException,
	ScalingGroupException {
		checkState(isConfigured(), "attempt to use unconfigured ScalingGroup");

		// verify that machine exists in group
		getMachineOrFail(machineId);

		try {
			List<String> tagKeys = Arrays.asList(Constants.SCALING_GROUP_TAG);
			this.client.untagServer(machineId, tagKeys);
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, ScalingGroupException.class);
			String message = format("failed to detach server \"%s\": %s",
					machineId, e.getMessage());
			throw new ScalingGroupException(message, e);
		}
	}

	@Override
	public void setServiceState(String machineId, ServiceState serviceState)
			throws NotFoundException {
		checkState(isConfigured(), "attempt to use unconfigured ScalingGroup");

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
			Throwables.propagateIfInstanceOf(e, ScalingGroupException.class);
			String message = format(
					"failed to tag service state on server \"%s\": %s",
					machineId, e.getMessage());
			throw new ScalingGroupException(message, e);
		}
	}

	@Override
	public String getScalingGroupName() {
		checkState(isConfigured(), "attempt to use unconfigured ScalingGroup");

		return this.scalingGroupName.get();
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
		String prefix = getScalingGroupName();
		String suffix = UUID.randomUUID().toString();
		return String.format("%s-%s", prefix, suffix);
	}

	boolean isConfigured() {
		return config() != null;
	}

	OpenStackScalingGroupConfig config() {
		return this.config.get();
	}
}
