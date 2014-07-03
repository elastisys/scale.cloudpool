package com.elastisys.scale.cloudadapters.openstack.scalinggroup;

import java.util.List;
import java.util.Map;

import org.jclouds.openstack.nova.v2_0.domain.Server;

import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;

/**
 * A {@link FakeOpenstackClient} that only allows a limited number of
 * {@link #launchInstance} calls before an error is raised.
 *
 * 
 *
 */
public class FailingFakeOpenstackClient extends FakeOpenstackClient {
	private final int numLaunchesBeforeFailure;
	private int numLaunches;

	public FailingFakeOpenstackClient(List<Server> servers,
			int numLaunchesBeforeFailure) {
		super(servers);
		this.numLaunchesBeforeFailure = numLaunchesBeforeFailure;
		this.numLaunches = 0;
	}

	@Override
	public Server launchServer(String name, ScaleUpConfig provisioningDetails,
			Map<String, String> tags) {
		this.numLaunches++;
		if (this.numLaunches > this.numLaunchesBeforeFailure) {
			throw new RuntimeException("failed to launch instance");
		}
		return super.launchServer(name, provisioningDetails, tags);
	}
}
