package com.elastisys.scale.cloudpool.openstack.functions;

import static com.elastisys.scale.cloudpool.openstack.driver.Constants.SERVICE_STATE_TAG;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import jersey.repackaged.com.google.common.collect.Lists;

import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.Server.Status;
import org.jclouds.openstack.v2_0.domain.Resource;
import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.openstack.functions.ServerToMachine;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * Exercises {@link ServerToMachine}.
 */
public class TestServerToMachine {

	@Test
	public void convertServerWithPublicAndPrivateIpAddresses() {
		DateTime now = UtcTime.now();

		Address privateIp = Address.createV4("10.11.12.2");
		Address publicIp = Address.createV4("130.239.48.193");
		List<Address> ipAddresses = Lists.newArrayList(privateIp, publicIp);
		Server server = server(Status.ACTIVE, now, ipAddresses);

		Machine machine = new ServerToMachine().apply(server);
		assertThat(machine.getId(), is(server.getId()));
		assertThat(machine.getMachineState(), is(MachineState.RUNNING));
		assertThat(machine.getServiceState(), is(ServiceState.UNKNOWN));
		assertThat(machine.getLaunchtime().toDate(), is(server.getCreated()));
		assertThat(machine.getPublicIps(), is(asList(publicIp.getAddr())));
		assertThat(machine.getPrivateIps(), is(asList(privateIp.getAddr())));
		assertThat(machine.getMetadata(), is(JsonUtils.toJson(server)));
	}

	@Test
	public void convertServerWithoutPublicIpAddress() {
		DateTime now = UtcTime.now();

		Address privateIp = Address.createV4("10.11.12.2");
		List<Address> ipAddresses = Lists.newArrayList(privateIp);
		Server server = server(Status.ACTIVE, now, ipAddresses);

		Machine machine = new ServerToMachine().apply(server);
		assertThat(machine.getId(), is(server.getId()));
		assertThat(machine.getMachineState(), is(MachineState.RUNNING));
		assertThat(machine.getServiceState(), is(ServiceState.UNKNOWN));
		assertThat(machine.getLaunchtime().toDate(), is(server.getCreated()));
		List<String> empty = asList();
		assertThat(machine.getPublicIps(), is(empty));
		assertThat(machine.getPrivateIps(), is(asList(privateIp.getAddr())));
		assertThat(machine.getMetadata(), is(JsonUtils.toJson(server)));
	}

	@Test
	public void convertServerWithServiceStateTag() {
		DateTime now = UtcTime.now();

		Address privateIp = Address.createV4("10.11.12.2");
		Address publicIp = Address.createV4("130.239.48.193");
		List<Address> ipAddresses = Lists.newArrayList(privateIp, publicIp);
		Map<String, String> tags = ImmutableMap.of(SERVICE_STATE_TAG,
				ServiceState.OUT_OF_SERVICE.name());
		Server server = serverWithMetadata(Status.ACTIVE, now, ipAddresses,
				tags);

		Machine machine = new ServerToMachine().apply(server);
		assertThat(machine.getServiceState(), is(ServiceState.OUT_OF_SERVICE));
	}

	private Server server(Status status, DateTime launchTime,
			Collection<Address> ipAddresses) {
		Multimap<String, Address> ips = ArrayListMultimap.create();
		// it seems as though jclouds set both private and floating IP addresses
		// under the 'private' key
		ips.putAll("private", ipAddresses);

		return Server.builder().tenantId("tenantId").id("serverId")
				.userId("userId").created(launchTime.toDate()).addresses(ips)
				.image(Resource.builder().id("imageId").build())
				.flavor(Resource.builder().id("flavorId").build())
				.status(status).build();
	}

	private Server serverWithMetadata(Status status, DateTime launchTime,
			Collection<Address> ipAddresses, Map<String, String> metadata) {
		Multimap<String, Address> ips = ArrayListMultimap.create();
		// it seems as though jclouds set both private and floating IP addresses
		// under the 'private' key
		ips.putAll("private", ipAddresses);

		return Server.builder().tenantId("tenantId").id("serverId")
				.userId("userId").created(launchTime.toDate()).addresses(ips)
				.image(Resource.builder().id("imageId").build())
				.flavor(Resource.builder().id("flavorId").build())
				.status(status).metadata(metadata).build();
	}
}
