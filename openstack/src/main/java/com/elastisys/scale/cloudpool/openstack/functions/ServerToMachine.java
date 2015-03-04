package com.elastisys.scale.cloudpool.openstack.functions;

import java.util.Collection;
import java.util.List;

import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.util.InetAddresses2.IsPrivateIPAddress;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.openstack.driver.Constants;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

/**
 * Translates a {@link Server} from the OpenStack API to its {@link Machine}
 * counterpart.
 *
 *
 *
 */
public class ServerToMachine implements Function<Server, Machine> {

	/**
	 * Converts a {@link Server} to its {@link Machine} representation.
	 *
	 * @param server
	 * @return
	 */
	public static Machine convert(Server server) {
		return new ServerToMachine().apply(server);
	}

	/**
	 * Converts a {@link Server} from the OpenStack API to its {@link Machine}
	 * representation.
	 *
	 * @param server
	 *            The {@link Server} representation.
	 * @return The corresponding {@link Machine} representation.
	 */
	@Override
	public Machine apply(Server server) {
		return asMachine(server);
	}

	/**
	 * Converts a {@link Server} from the OpenStack API to its {@link Machine}
	 * representation.
	 */
	private static Machine asMachine(Server server) {
		MachineState machineState = new StatusToMachineState().apply(server
				.getStatus());
		DateTime launchTime = new DateTime(server.getCreated(),
				DateTimeZone.UTC);

		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();

		Collection<Address> ipAddresses = server.getAddresses().values();
		for (Address ipAddress : ipAddresses) {
			String ip = ipAddress.getAddr();
			if (IsPrivateIPAddress.INSTANCE.apply(ip)) {
				privateIps.add(ip);
			} else {
				publicIps.add(ip);
			}
		}
		ServiceState serviceState = ServiceState.UNKNOWN;
		if (server.getMetadata().containsKey(Constants.SERVICE_STATE_TAG)) {
			serviceState = ServiceState.valueOf(server.getMetadata().get(
					Constants.SERVICE_STATE_TAG));
		}
		JsonObject metadata = JsonUtils.toJson(server).getAsJsonObject();
		return new Machine(server.getId(), machineState, serviceState,
				launchTime, publicIps, privateIps, metadata);
	}

}
