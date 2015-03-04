package com.elastisys.scale.cloudpool.openstack.requests;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.List;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.openstack.driver.OpenStackPoolDriverConfig;

/**
 * An Openstack Nova (compute) request that, when executed, deletes selected
 * tags from the server's metadata.
 */
public class DeleteServerMetadataRequest extends AbstractNovaRequest<Void> {

	/** The id of the server whose metadata is to be updated. */
	private final String serverId;
	/**
	 * Meta data tags to be removed from the server.
	 */
	private final List<String> metadataKeysToDelete;

	/**
	 * Constructs a {@link DeleteServerMetadataRequest}.
	 *
	 * @param account
	 *            Account login credentials for a particular OpenStack Nova
	 *            endpoint.
	 * @param serverId
	 *            The server whose metadata is to be updated.
	 * @param metadataKeysToDelete
	 *            Meta data tags to be removed from the server.
	 */
	public DeleteServerMetadataRequest(OpenStackPoolDriverConfig account,
			String serverId, List<String> metadataKeysToDelete) {
		super(account);

		checkNotNull(serverId, "server id cannot be null");
		checkNotNull(metadataKeysToDelete, "metadata keys cannot be null");

		this.serverId = serverId;
		this.metadataKeysToDelete = metadataKeysToDelete;
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
		// delete tags
		for (String metadataKey : this.metadataKeysToDelete) {
			serverApi.deleteMetadata(this.serverId, metadataKey);
		}

		return null;
	}
}
