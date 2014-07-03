package com.elastisys.scale.cloudadapters.openstack.requests.lab;

import java.util.List;

import org.jclouds.openstack.nova.v2_0.domain.Server;

import com.elastisys.scale.cloudadapters.openstack.requests.ListServersWithTagRequest;

public class ListServersWithTagMain extends AbstractClient {

	// TODO: set to tag value that must be present on returned servers
	private static final String tag = "elastic-pool";
	// TODO: set to tag that must be present on returned servers
	private static final String tagValue = "mypool";

	public static void main(String[] args) throws Exception {
		ListServersWithTagRequest task = new ListServersWithTagRequest(
				getAccountConfig(), tag, tagValue);
		List<Server> servers = task.call();
		for (Server server : servers) {
			System.out.println(server);
			System.out.println("  created at: " + server.getCreated());
			System.out.println("  metadata: " + server.getMetadata());
			System.out.println("  access ip: " + server.getAccessIPv4());
			System.out.println("  ip: " + server.getAddresses());
		}
	}
}
