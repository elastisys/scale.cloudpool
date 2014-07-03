package com.elastisys.scale.cloudadapters.openstack.requests.lab;

import com.elastisys.scale.cloudadapters.openstack.requests.DeleteServerRequest;

public class DeleteServerMain extends AbstractClient {

	// TODO: set to the identifier (UUID) of the server to delete
	private static final String serverId = "5ef972e9-6420-41c2-b25e-3cfbefe01148";

	public static void main(String[] args) throws Exception {
		logger.info("deleting server: {}", serverId);
		DeleteServerRequest request = new DeleteServerRequest(
				getAccountConfig(), serverId);
		request.call();
		logger.info("deleted server: {}", serverId);
	}
}
