package com.elastisys.scale.cloudpool.openstack.requests;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ServerService;
import org.openstack4j.model.compute.Server;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.commons.openstack.OSClientFactory;

/**
 * An request that, when executed, updates the metadata tags of a {@link Server}
 * instance.
 */
public class UpdateServerMetadataRequest extends AbstractOpenstackRequest<Void> {

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
     * @param clientFactory
     *            OpenStack API client factory.
     * @param serverId
     *            The server whose metadata is to be updated.
     * @param metadata
     *            Meta data tags to be copied to the server. Any meta data keys
     *            that already exist on the node will be overwritten.
     */
    public UpdateServerMetadataRequest(OSClientFactory clientFactory, String serverId, Map<String, String> metadata) {
        super(clientFactory);

        checkNotNull(serverId, "server id cannot be null");
        checkNotNull(metadata, "metadata map cannot be null");

        this.serverId = serverId;
        this.metadata = metadata;
    }

    @Override
    public Void doRequest(OSClient api) throws NotFoundException {
        ServerService serverApi = api.compute().servers();
        Server server = serverApi.get(this.serverId);
        if (server == null) {
            throw new NotFoundException(
                    format("failed to update meta data on server '%s': " + "server not found", this.serverId));
        }
        // set tags
        Map<String, String> tags = new HashMap<>(server.getMetadata());
        tags.putAll(this.metadata);
        serverApi.updateMetadata(this.serverId, tags);
        return null;
    }
}
