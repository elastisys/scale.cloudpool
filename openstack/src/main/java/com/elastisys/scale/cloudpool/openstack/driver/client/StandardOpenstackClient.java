package com.elastisys.scale.cloudpool.openstack.driver.client;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.openstack4j.model.compute.Server;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;
import com.elastisys.scale.cloudpool.openstack.driver.config.OpenStackPoolDriverConfig;
import com.elastisys.scale.cloudpool.openstack.requests.AssignFloatingIpRequest;
import com.elastisys.scale.cloudpool.openstack.requests.CreateServerRequest;
import com.elastisys.scale.cloudpool.openstack.requests.DeleteServerMetadataRequest;
import com.elastisys.scale.cloudpool.openstack.requests.DeleteServerRequest;
import com.elastisys.scale.cloudpool.openstack.requests.GetServerRequest;
import com.elastisys.scale.cloudpool.openstack.requests.ListServersWithTagRequest;
import com.elastisys.scale.cloudpool.openstack.requests.UpdateServerMetadataRequest;
import com.google.common.util.concurrent.Atomics;

/**
 * Standard implementation of the {@link OpenstackClient} interface.
 */
public class StandardOpenstackClient implements OpenstackClient {

	/**
	 * {@link OSClientFactory} configured to use the latest set
	 * {@link OpenStackPoolDriverConfig}.
	 */
	private final AtomicReference<OSClientFactory> clientFactory;

	public StandardOpenstackClient() {
		this.clientFactory = Atomics.newReference();
	}

	@Override
	public void configure(OpenStackPoolDriverConfig configuration) {
		checkArgument(configuration != null, "null configuration");

		this.clientFactory.set(new OSClientFactory(configuration));

	}

	@Override
	public List<Server> getServers(String tag, String tagValue) {
		checkArgument(isConfigured(), "can't use client before it's configured");

		return new ListServersWithTagRequest(clientFactory(), tag, tagValue).call();
	}

	@Override
	public Server getServer(String serverId) throws NotFoundException {
		checkArgument(isConfigured(), "can't use client before it's configured");

		return new GetServerRequest(clientFactory(), serverId).call();
	}

	@Override
	public Server launchServer(String serverName, ScaleOutConfig provisioningDetails, Map<String, String> tags) {
		checkArgument(isConfigured(), "can't use client before it's configured");

		CreateServerRequest request = new CreateServerRequest(clientFactory(), serverName,
				provisioningDetails.getSize(), provisioningDetails.getImage(), provisioningDetails.getKeyPair(),
				provisioningDetails.getSecurityGroups(), config().getNetworks(),
				provisioningDetails.getEncodedUserData(), tags);
		return request.call();
	}

	@Override
	public String assignFloatingIp(String serverId) throws NotFoundException {
		Server server = getServer(serverId);
		return new AssignFloatingIpRequest(clientFactory(), server).call();
	}

	@Override
	public void terminateServer(String serverId) throws NotFoundException {
		checkArgument(isConfigured(), "can't use client before it's configured");
		new DeleteServerRequest(clientFactory(), serverId).call();
	}

	@Override
	public void tagServer(String serverId, Map<String, String> tags) {
		checkArgument(isConfigured(), "can't use client before it's configured");

		new UpdateServerMetadataRequest(clientFactory(), serverId, tags).call();
	}

	@Override
	public void untagServer(String serverId, List<String> tagKeys) {
		checkArgument(isConfigured(), "can't use client before it's configured");

		new DeleteServerMetadataRequest(clientFactory(), serverId, tagKeys).call();
	}

	private boolean isConfigured() {
		return config() != null;
	}

	public OSClientFactory clientFactory() {
		return this.clientFactory.get();
	}

	private OpenStackPoolDriverConfig config() {
		return this.clientFactory.get().getApiAccessConfig();
	}

}
