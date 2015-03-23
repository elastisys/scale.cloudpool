package com.elastisys.scale.cloudpool.openstack.requests;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.openstack.driver.config.OpenStackPoolDriverConfig;

/**
 * A request that, when called, requests meta data about a particular
 * {@link Server} instance.
 * <p/>
 * If the requested {@link Server} cannot be found, an
 * {@link IllegalArgumentException} is raised.
 */
public class GetServerRequest extends AbstractOpenstackRequest<Server> {

	/** The identifier of the server to get. */
	private String serverId;

	public GetServerRequest(OpenStackPoolDriverConfig account, String serverId) {
		super(account);
		this.serverId = serverId;
	}

	@Override
	public Server doRequest(OSClient api) throws NotFoundException {
		Server server = api.compute().servers().get(this.serverId);
		if (server == null) {
			throw new NotFoundException(String.format("failed to retrieve "
					+ "server '%s' in region %s", this.serverId,
					getAccessConfig().getRegion()));
		}
		return server;
	}
}
