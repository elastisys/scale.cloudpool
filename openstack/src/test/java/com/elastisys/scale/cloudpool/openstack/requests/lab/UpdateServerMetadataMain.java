package com.elastisys.scale.cloudpool.openstack.requests.lab;

import java.util.Map;

import com.elastisys.scale.cloudpool.openstack.requests.UpdateServerMetadataRequest;
import com.google.common.collect.ImmutableMap;

public class UpdateServerMetadataMain {

	/** TODO: set to server uuid */
	private static final String serverId = "47d3376e-e6e0-4ebd-8ba5-add5d67a6c8e";

	public static void main(String[] args) {
		Map<String, String> metadata = ImmutableMap.of("key1", "value1");
		new UpdateServerMetadataRequest(DriverConfigLoader.loadDefault(),
				serverId, metadata).call();
	}
}
