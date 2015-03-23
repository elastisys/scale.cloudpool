package com.elastisys.scale.cloudpool.openstack.requests;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.List;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ServerService;
import org.openstack4j.model.compute.Server;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.openstack.driver.config.OpenStackPoolDriverConfig;

/**
 * An Openstack request that, when executed, deletes selected tags from a
 * {@link Server}'s metadata.
 */
public class DeleteServerMetadataRequest extends AbstractOpenstackRequest<Void> {

	/** The id of the server whose metadata is to be updated. */
	private final String serverId;
	/**
	 * Meta data tags to be removed from the server.
	 */
	private final List<String> metadataKeysToDelete;

	/**
	 * Constructs a {@link DeleteServerMetadataRequest}.
	 *
	 * @param accessConfig
	 *            Account login credentials for a particular OpenStack Nova
	 *            endpoint.
	 * @param serverId
	 *            The server whose metadata is to be updated.
	 * @param metadataKeysToDelete
	 *            Meta data tags to be removed from the server.
	 */
	public DeleteServerMetadataRequest(OpenStackPoolDriverConfig accessConfig,
			String serverId, List<String> metadataKeysToDelete) {
		super(accessConfig);

		checkNotNull(serverId, "server id cannot be null");
		checkNotNull(metadataKeysToDelete, "metadata keys cannot be null");

		this.serverId = serverId;
		this.metadataKeysToDelete = metadataKeysToDelete;
	}

	@Override
	public Void doRequest(OSClient api) throws NotFoundException {
		ServerService serverApi = api.compute().servers();
		Server server = serverApi.get(this.serverId);
		if (server == null) {
			throw new NotFoundException(format(
					"failed to update meta data on server '%s': "
							+ "server not found", this.serverId));
		}
		// delete tags
		for (String metadataKey : this.metadataKeysToDelete) {
			serverApi.deleteMetadataItem(this.serverId, metadataKey);
		}

		return null;
	}
}
