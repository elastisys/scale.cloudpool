package com.elastisys.scale.cloudadapters.openstack.requests;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;

import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroupConfig;

/**
 * A request that, when called, determines if a particular {@link Server}
 * exists.
 *
 * 
 *
 */
public class ServerExistsRequest extends AbstractNovaRequest<Boolean> {
	/** The identifier of the server whose existence is to be checked. */
	private String serverId;

	public ServerExistsRequest(OpenStackScalingGroupConfig account,
			String serverId) {
		super(account);
		this.serverId = serverId;
	}

	@Override
	public Boolean doRequest(NovaApi api) {
		try {
			Server server = new GetServerRequest(getAccount(), this.serverId)
					.call();
			return server != null;
		} catch (IllegalArgumentException e) {
			// GetServerRequest throws an IllegalArgumentException if the server
			// doesn't exist.
			return false;
		}
	}
}
