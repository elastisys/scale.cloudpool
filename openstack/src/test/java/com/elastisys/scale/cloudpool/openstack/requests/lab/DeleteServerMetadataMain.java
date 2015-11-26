package com.elastisys.scale.cloudpool.openstack.requests.lab;

import java.util.List;

import com.elastisys.scale.cloudpool.openstack.driver.client.OSClientFactory;
import com.elastisys.scale.cloudpool.openstack.requests.DeleteServerMetadataRequest;
import com.google.common.collect.ImmutableList;

public class DeleteServerMetadataMain {

	/** TODO: set to server uuid */
	private static final String serverId = "47d3376e-e6e0-4ebd-8ba5-add5d67a6c8e";

	public static void main(String[] args) {
		List<String> metadataKeys = ImmutableList.of("key1");
		new DeleteServerMetadataRequest(
				new OSClientFactory(DriverConfigLoader.loadDefault()), serverId,
				metadataKeys).call();
	}
}
