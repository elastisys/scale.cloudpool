package com.elastisys.scale.cloudadapters.openstack.scalinggroup.client;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.scriptbuilder.domain.OsFamily;

import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.openstack.requests.AssignFloatingIpRequest;
import com.elastisys.scale.cloudadapters.openstack.requests.CreateServerRequest;
import com.elastisys.scale.cloudadapters.openstack.requests.DeleteServerRequest;
import com.elastisys.scale.cloudadapters.openstack.requests.GetServerRequest;
import com.elastisys.scale.cloudadapters.openstack.requests.ListServersWithTagRequest;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroupConfig;
import com.elastisys.scale.cloudadapters.openstack.utils.jclouds.ScriptUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Atomics;

/**
 * Standard implementation of the {@link OpenstackClient} interface.
 *
 * 
 *
 */
public class StandardOpenstackClient implements OpenstackClient {

	/** Configuration with connection details for the OpenStack API. */
	private final AtomicReference<OpenStackScalingGroupConfig> config;

	public StandardOpenstackClient() {
		this.config = Atomics.newReference();
	}

	@Override
	public void configure(OpenStackScalingGroupConfig configuration) {
		checkArgument(configuration != null, "null configuration");

		this.config.set(configuration);
	}

	@Override
	public List<Server> getServers(String tag, String tagValue) {
		checkArgument(isConfigured(), "can't use client before it's configured");

		return new ListServersWithTagRequest(config(), tag, tagValue).call();
	}

	@Override
	public Server getServer(String serverId) {
		checkArgument(isConfigured(), "can't use client before it's configured");

		return new GetServerRequest(config(), serverId).call();
	}

	@Override
	public Server launchServer(String serverName,
			ScaleUpConfig provisioningDetails, Map<String, String> tags) {
		checkArgument(isConfigured(), "can't use client before it's configured");

		String userData = ScriptUtils.renderScript(
				Joiner.on("\n").join(provisioningDetails.getBootScript()),
				OsFamily.UNIX);
		CreateServerRequest request = new CreateServerRequest(config(),
				serverName, provisioningDetails.getSize(),
				provisioningDetails.getImage(),
				provisioningDetails.getKeyPair(),
				provisioningDetails.getSecurityGroups(), Optional.of(userData),
				tags);
		return request.call();
	}

	@Override
	public String assignFloatingIp(String serverId) {
		Server server = getServer(serverId);
		return new AssignFloatingIpRequest(config(), server).call();
	}

	@Override
	public void terminateServer(String serverId) {
		checkArgument(isConfigured(), "can't use client before it's configured");

		new DeleteServerRequest(config(), serverId).call();
	}

	private boolean isConfigured() {
		return config() != null;
	}

	private OpenStackScalingGroupConfig config() {
		return this.config.get();
	}

}
