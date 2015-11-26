package com.elastisys.scale.cloudpool.openstack.requests.lab;

import java.util.List;

import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.openstack.driver.client.OSClientFactory;
import com.elastisys.scale.cloudpool.openstack.requests.ListServersWithTagRequest;

public class ListServersWithTagMain {
	private static Logger LOG = LoggerFactory
			.getLogger(ListServersWithTagMain.class);

	public static void main(String[] args) {
		List<Server> taggedServers = new ListServersWithTagRequest(
				new OSClientFactory(DriverConfigLoader.loadDefault()), "key1",
				"value1").call();
		LOG.info("{} tagged server(s) found", taggedServers.size());
		for (Server server : taggedServers) {
			LOG.info("server: {}", server);
		}
	}
}
