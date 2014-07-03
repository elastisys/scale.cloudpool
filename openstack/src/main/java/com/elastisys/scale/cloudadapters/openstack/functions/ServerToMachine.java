package com.elastisys.scale.cloudadapters.openstack.functions;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.ACTIVE;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.BUILD;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.ERROR;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.HARD_REBOOT;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.PASSWORD;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.REBOOT;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.REBUILD;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.RESIZE;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.REVERT_RESIZE;

import java.util.Collection;
import java.util.List;

import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.Server.Status;
import org.jclouds.util.InetAddresses2.IsPrivateIPAddress;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

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
		MachineState state = convertState(server.getStatus());
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
		JsonElement metadata = JsonUtils.toJson(server);
		return new Machine(server.getId(), state, launchTime, publicIps,
				privateIps, metadata);
	}

	private static MachineState convertState(Status state) {
		if (asList(ACTIVE, ERROR, PASSWORD).contains(state)) {
			return MachineState.RUNNING;
		} else if (asList(BUILD, REBUILD, REBOOT, HARD_REBOOT, RESIZE,
				REVERT_RESIZE, Status.VERIFY_RESIZE).contains(state)) {
			return MachineState.PENDING;
		} else if (asList(Status.STOPPED, Status.PAUSED, Status.SUSPENDED,
				Status.DELETED).contains(state)) {
			return MachineState.TERMINATED;
		} else {
			throw new IllegalArgumentException(format(
					"unrecognized instance state \"%s\"", state));
		}
	}
}
