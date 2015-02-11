package com.elastisys.scale.cloudadapters.openstack.requests.lab;

import org.jclouds.openstack.nova.v2_0.domain.Server;

import com.elastisys.scale.cloudadapters.openstack.requests.GetServerRequest;

public class GetServerMain extends AbstractClient {

	// TODO: set to the identifier (UUID) of the server to get
	private static final String serverId = "2633184d-15cd-4899-9b9c-9019cb40a77b";

	public static void main(String[] args) throws Exception {
		logger.info("getting server: {}", serverId);
		GetServerRequest request = new GetServerRequest(getAccountConfig(),
				serverId);
		Server server = request.call();
		logger.info("got server: {}", server);
	}
}
