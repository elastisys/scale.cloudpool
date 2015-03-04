package com.elastisys.scale.cloudpool.openstack.requests;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.openstack.driver.OpenStackPoolDriverConfig;

/**
 * An Openstack Nova (compute) request that, when executed, updates the metadata
 * tags of a server instance.
 */
public class UpdateServerMetadataRequest extends AbstractNovaRequest<Void> {

	/** The id of the server whose metadata is to be updated. */
	private final String serverId;
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
	 * @param serverId
	 *            The server whose metadata is to be updated.
	 * @param metadata
	 *            Meta data tags to be copied to the server. Any meta data keys
	 *            that already exist on the node will be overwritten.
	 */
	public UpdateServerMetadataRequest(OpenStackPoolDriverConfig account,
			String serverId, Map<String, String> metadata) {
		super(account);

		checkNotNull(serverId, "server id cannot be null");
		checkNotNull(metadata, "metadata map cannot be null");

		this.serverId = serverId;
		this.metadata = metadata;
	}

	@Override
	public Void doRequest(NovaApi api) throws NotFoundException {
		ServerApi serverApi = api.getServerApiForZone(getAccount().getRegion());
		Server server = serverApi.get(this.serverId);
		if (server == null) {
			throw new NotFoundException(format(
					"failed to update meta data on server '%s': "
							+ "server not found", this.serverId));
		}
		// set tags
		Map<String, String> tags = new HashMap<>(server.getMetadata());
		tags.putAll(this.metadata);
		serverApi.setMetadata(this.serverId, tags);
		return null;
	}
}
