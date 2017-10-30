package com.elastisys.scale.cloudpool.openstack.requests;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.model.compute.Server;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.commons.openstack.OSClientFactory;

/**
 * A request that, when called, requests meta data about a particular
 * {@link Server}.
 */
public class GetServerRequest extends AbstractOpenstackRequest<Server> {

    /** The identifier of the server to get. */
    private String serverId;

    /**
     * Creates a new {@link GetServerRequest}.
     *
     * @param clientFactory
     *            OpenStack API client factory.
     * @param serverId
     *            The id of the server to get.
     */
    public GetServerRequest(OSClientFactory clientFactory, String serverId) {
        super(clientFactory);
        this.serverId = serverId;
    }

    @Override
    public Server doRequest(OSClient api) throws NotFoundException, ResponseException {
        Server server = api.compute().servers().get(this.serverId);
        if (server == null) {
            throw new NotFoundException(String.format("failed to retrieve " + "server '%s' in region %s", this.serverId,
                    getApiAccessConfig().getRegion()));
        }
        return server;
    }
}
