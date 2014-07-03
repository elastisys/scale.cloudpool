package com.elastisys.scale.cloudadapters.openstack.requests.lab;

import org.jclouds.openstack.nova.v2_0.domain.Server;

import com.elastisys.scale.cloudadapters.openstack.requests.GetServerRequest;

public class GetServerMain extends AbstractClient {

	// TODO: set to the identifier (UUID) of the server to get
	private static final String serverId = "dd6d7539-3c51-4b40-9697-88b05ffa6f65";

	public static void main(String[] args) throws Exception {
		logger.info("getting server: {}", serverId);
		GetServerRequest request = new GetServerRequest(getAccountConfig(),
				serverId);
		Server server = request.call();
		logger.info("got server: {}", server);
	}
}
