package com.elastisys.scale.cloudpool.openstack.requests.lab;

import com.elastisys.scale.cloudpool.openstack.requests.DeleteServerRequest;
import com.elastisys.scale.commons.openstack.OSClientFactory;

public class DeleteServerMain {

	/** TODO: set to server uuid */
	private static final String serverId = "47d3376e-e6e0-4ebd-8ba5-add5d67a6c8e";

	public static void main(String[] args) {
		new DeleteServerRequest(new OSClientFactory(DriverConfigLoader.loadDefault().toApiAccessConfig()), serverId)
				.call();
	}
}
