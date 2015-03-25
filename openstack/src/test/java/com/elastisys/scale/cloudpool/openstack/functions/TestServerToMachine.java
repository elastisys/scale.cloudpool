package com.elastisys.scale.cloudpool.openstack.functions;

import static com.elastisys.scale.cloudpool.openstack.driver.Constants.SERVICE_STATE_TAG;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.openstack.compute.domain.NovaAddresses;
import org.openstack4j.openstack.compute.domain.NovaAddresses.NovaAddress;
import org.openstack4j.openstack.compute.domain.NovaServer;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.openstack.driver.Constants;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.ImmutableMap;

/**
 * Exercises {@link ServerToMachine}.
 */
public class TestServerToMachine {

	@Test
	public void convertServerWithPublicAndPrivateIpAddresses() {
		DateTime now = UtcTime.now();

		NovaAddresses addresses = new NovaAddresses();
		addresses.add("Default network", novaAddress("10.11.12.2", "fixed"));
		addresses.add("Default network",
				novaAddress("130.239.48.193", "floating"));
		Server server = server(Status.ACTIVE, now, addresses);

		Machine machine = new ServerToMachine().apply(server);
		assertThat(machine.getId(), is(server.getId()));
		assertThat(machine.getMachineState(), is(MachineState.RUNNING));
		assertThat(machine.getMembershipStatus(),
				is(MembershipStatus.defaultStatus()));
		assertThat(machine.getServiceState(), is(ServiceState.UNKNOWN));
		assertThat(machine.getLaunchtime().toDate(), is(server.getCreated()));
		assertThat(machine.getPublicIps(), is(asList("130.239.48.193")));
		assertThat(machine.getPrivateIps(), is(asList("10.11.12.2")));
		assertThat(machine.getMetadata(), is(JsonUtils.toJson(server)));
	}

	@Test
	public void convertServerWithoutPublicIpAddress() {
		DateTime now = UtcTime.now();

		NovaAddresses addresses = new NovaAddresses();
		addresses.add("private", novaAddress("10.11.12.2", "fixed"));
		Server server = server(Status.ACTIVE, now, addresses);

		Machine machine = new ServerToMachine().apply(server);
		assertThat(machine.getId(), is(server.getId()));
		assertThat(machine.getMachineState(), is(MachineState.RUNNING));
		assertThat(machine.getMembershipStatus(),
				is(MembershipStatus.defaultStatus()));
		assertThat(machine.getServiceState(), is(ServiceState.UNKNOWN));
		assertThat(machine.getLaunchtime().toDate(), is(server.getCreated()));
		List<String> empty = asList();
		assertThat(machine.getPublicIps(), is(empty));
		assertThat(machine.getPrivateIps(), is(asList("10.11.12.2")));
		assertThat(machine.getMetadata(), is(JsonUtils.toJson(server)));
	}

	@Test
	public void convertServerWithServiceStateTag() {
		DateTime now = UtcTime.now();

		NovaAddresses addresses = new NovaAddresses();
		addresses.add("private", novaAddress("10.11.12.2", "fixed"));
		addresses.add("private", novaAddress("130.239.48.193", "floating"));
		Map<String, String> tags = ImmutableMap.of(SERVICE_STATE_TAG,
				ServiceState.OUT_OF_SERVICE.name());
		Server server = serverWithMetadata(Status.ACTIVE, now, addresses, tags);

		Machine machine = new ServerToMachine().apply(server);
		assertThat(machine.getServiceState(), is(ServiceState.OUT_OF_SERVICE));
	}

	@Test
	public void convertServerWithMembershipStatusTag() {
		DateTime now = UtcTime.now();

		NovaAddresses addresses = new NovaAddresses();
		addresses.add("private", novaAddress("10.11.12.2", "fixed"));
		addresses.add("private", novaAddress("130.239.48.193", "floating"));
		MembershipStatus status = MembershipStatus.blessed();
		String statusAsJson = JsonUtils.toString(JsonUtils.toJson(status));
		Map<String, String> tags = ImmutableMap.of(
				Constants.MEMBERSHIP_STATUS_TAG, statusAsJson);
		Server server = serverWithMetadata(Status.ACTIVE, now, addresses, tags);

		Machine machine = new ServerToMachine().apply(server);
		assertThat(machine.getMembershipStatus(), is(status));
	}

	private Server server(Status status, DateTime launchTime,
			NovaAddresses ipAddresses) {

		NovaServer server = new NovaServer();
		server.id = "serverId";
		server.created = launchTime.toDate();
		server.addresses = ipAddresses;
		server.status = status;
		server.metadata = new HashMap<String, String>();
		return server;
	}

	private Server serverWithMetadata(Status status, DateTime launchTime,
			NovaAddresses ipAddresses, Map<String, String> metadata) {

		NovaServer server = new NovaServer();
		server.id = "serverId";
		server.created = launchTime.toDate();
		server.addresses = ipAddresses;
		server.status = status;
		server.metadata = metadata;
		return server;
	}

	/**
	 * Returns a {@link NovaAddress} object representing a given IP address.
	 *
	 * @param ip
	 *            The IP address
	 * @param type
	 *            The type of IP address. Can be either "fixed" (i.e. private)
	 *            or "floating" (i.e. public).
	 * @return
	 */
	private NovaAddress novaAddress(String ip, String type) {
		// Since the NovaAddress class has no constructor/setters and private
		// members, we generate an instance from JSON.
		String dummyMacAddr = "01:23:45:67:89:ab";
		int version = 4;
		String asJson = String.format("{\"macAddr\": \"%s\", "
				+ "\"version\": %d, " + "\"addr\": \"%s\", "
				+ "\"type\": \"%s\"}", dummyMacAddr, version, ip, type);
		return JsonUtils.toObject(JsonUtils.parseJsonString(asJson),
				NovaAddress.class);
	}
}
