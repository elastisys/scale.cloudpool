package com.elastisys.scale.cloudpool.openstack.requests;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;

import com.elastisys.scale.cloudpool.openstack.driver.config.OpenStackPoolDriverConfig;

/**
 * A request that, when called, determines if a particular {@link Server}
 * exists.
 */
public class ServerExistsRequest extends AbstractOpenstackRequest<Boolean> {
	/** The identifier of the server whose existence is to be checked. */
	private String serverId;

	public ServerExistsRequest(OpenStackPoolDriverConfig accessConfig,
			String serverId) {
		super(accessConfig);
		this.serverId = serverId;
	}

	@Override
	public Boolean doRequest(OSClient api) {
		Server server = api.compute().servers().get(this.serverId);
		return server != null;
	}
}
