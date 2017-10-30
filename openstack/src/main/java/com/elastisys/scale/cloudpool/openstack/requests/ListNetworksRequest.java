package com.elastisys.scale.cloudpool.openstack.requests;

import java.util.ArrayList;
import java.util.List;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.model.network.Network;

import com.elastisys.scale.commons.openstack.OSClientFactory;

/**
 * An OpenStack request task that, when executed, retrieves all available
 * networks.
 */
public class ListNetworksRequest extends AbstractOpenstackRequest<List<Network>> {

    /**
     * Constructs a new {@link ListNetworksRequest} task.
     *
     * @param clientFactory
     *            OpenStack API client factory.
     *
     */
    public ListNetworksRequest(OSClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public List<Network> doRequest(OSClient api) throws ResponseException {
        List<Network> networks = new ArrayList<>();
        networks.addAll(api.networking().network().list());
        return networks;
    }

}
