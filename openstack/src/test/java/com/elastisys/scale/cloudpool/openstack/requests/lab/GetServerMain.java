package com.elastisys.scale.cloudpool.openstack.requests.lab;

import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.openstack.requests.GetServerRequest;
import com.elastisys.scale.commons.openstack.OSClientFactory;

public class GetServerMain {
	private static Logger LOG = LoggerFactory.getLogger(GetServerMain.class);

	/** TODO: set to server uuid */
	private static final String serverId = "47d3376e-e6e0-4ebd-8ba5-add5d67a6c8e";

	public static void main(String[] args) {
		Server server = new GetServerRequest(new OSClientFactory(DriverConfigLoader.loadDefault().toApiAccessConfig()),
				serverId).call();
		LOG.info("got server: {}", server);
	}
}
