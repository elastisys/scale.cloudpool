package com.elastisys.scale.cloudadapters.openstack.scalinggroup;

import java.util.List;
import java.util.Map;

import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.Server.Status;
import org.jclouds.openstack.v2_0.domain.Resource;

import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.client.OpenstackClient;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

/**
 * Fake {@link OpenstackClient} that manages instances for a phony OpenStack
 * account.
 *
 * 
 *
 */
public class FakeOpenstackClient implements OpenstackClient {
	private int idSequencer = 0;
	private int floatingIpSequencer = 1;
	private List<Server> servers;

	public FakeOpenstackClient(List<Server> servers) {
		this.servers = servers;
		this.idSequencer = this.servers.size();
	}

	@Override
	public void configure(OpenStackScalingGroupConfig configuration) {
	}

	@Override
	public List<Server> getServers(String tag, String tagValue) {
		return Lists.newArrayList(this.servers);
	}

	@Override
	public Server getServer(String serverId) {
		int index = serverIndex(serverId);
		return this.servers.get(index);
	}

	@Override
	public Server launchServer(String name, ScaleUpConfig provisioningDetails,
			Map<String, String> tags) {
		int idNum = ++this.idSequencer;

		Resource image = Resource.builder().id(provisioningDetails.getImage())
				.build();
		Resource flavor = Resource.builder().id(provisioningDetails.getSize())
				.build();
		ListMultimap<String, Address> ipAddresses = ArrayListMultimap.create();
		ipAddresses.put("private", Address.createV4("10.1.2.3"));

		Server launchedServer = Server.builder().id("i-" + idNum)
				.userId("clouduser").tenantId("tenantId")
				.created(UtcTime.now().toDate()).status(Status.ACTIVE)
				.flavor(flavor).image(image).addresses(ipAddresses)
				.metadata(tags).build();
		this.servers.add(launchedServer);
		return launchedServer;
	}

	@Override
	public String assignFloatingIp(String serverId) {
		int index = serverIndex(serverId);
		Server server = this.servers.get(index);

		this.floatingIpSequencer++;
		String floatingIp = "200.1.2." + this.floatingIpSequencer;

		ListMultimap<String, Address> updatedAddresses = ArrayListMultimap
				.create(server.getAddresses());
		updatedAddresses.put("private", Address.createV4(floatingIp));

		// overwrite old server
		Server updatedServer = Server.builder().fromServer(server)
				.addresses(updatedAddresses).build();
		this.servers.set(index, updatedServer);

		return floatingIp;
	}

	@Override
	public void terminateServer(String serverId) {
		Server server = getServer(serverId);
		this.servers.remove(server);
	}

	private int serverIndex(String serverId) {
		for (int i = 0; i < this.servers.size(); i++) {
			if (this.servers.get(i).getId().equals(serverId)) {
				return i;
			}
		}
		throw new IllegalArgumentException(String.format(
				"no server with id %s exists", serverId));
	}
}
