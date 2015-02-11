package com.elastisys.scale.cloudadapters.openstack.requests;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;

import com.elastisys.scale.cloudadapers.api.NotFoundException;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroupConfig;

/**
 * A request that, when called, requests meta data about a particular
 * {@link Server} instance.
 * <p/>
 * If the requested {@link Server} cannot be found, an
 * {@link IllegalArgumentException} is raised.
 *
 *
 *
 */
public class GetServerRequest extends AbstractNovaRequest<Server> {

	/** The identifier of the server to get. */
	private String serverId;

	public GetServerRequest(OpenStackScalingGroupConfig account, String serverId) {
		super(account);
		this.serverId = serverId;
	}

	@Override
	public Server doRequest(NovaApi api) throws NotFoundException {
		ServerApi serverApi = api.getServerApiForZone(getAccount().getRegion());
		Server server = serverApi.get(this.serverId);
		if (server == null) {
			throw new NotFoundException(String.format("failed to retrieve "
					+ "server '%s' in region %s", this.serverId, getAccount()
					.getRegion()));
		}
		return server;
	}
}
