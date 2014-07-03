package com.elastisys.scale.cloudadapters.openstack.requests;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;

import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroupConfig;

/**
 * An Openstack Nova (compute) request that, when executed, updates the metadata
 * tags of a server instance.
 *
 * 
 *
 */
public class UpdateServerMetadataRequest extends AbstractNovaRequest<Void> {

	/** The server whose metadata is to be updated. */
	private final Server server;
	/**
	 * Meta data tags to be copied to the server. Any meta data keys that
	 * already exist on the node will be overwritten.
	 */
	private final Map<String, String> metadata;

	/**
	 * Constructs a {@link UpdateServerMetadataRequest}.
	 *
	 * @param account
	 *            Account login credentials for a particular OpenStack Nova
	 *            endpoint.
	 * @param server
	 *            The server whose metadata is to be updated.
	 * @param metadata
	 *            Meta data tags to be copied to the server. Any meta data keys
	 *            that already exist on the node will be overwritten.
	 */
	public UpdateServerMetadataRequest(OpenStackScalingGroupConfig account,
			Server server, Map<String, String> metadata) {
		super(account);

		checkNotNull(server, "server name cannot be null");
		checkNotNull(metadata, "metadata map cannot be null");

		this.server = server;
		this.metadata = metadata;
	}

	@Override
	public Void doRequest(NovaApi api) {
		ServerApi serverApi = api.getServerApiForZone(getAccount().getRegion());
		Server server = serverApi.get(this.server.getId());
		if (server == null) {
			throw new ScalingGroupException(format(
					"failed to update meta data on server '%s': "
							+ "server not found", this.server.getName()));
		}
		// set tags
		Map<String, String> tags = new HashMap<>(server.getMetadata());
		tags.putAll(this.metadata);
		serverApi.setMetadata(server.getId(), tags);
		return null;
	}
}
