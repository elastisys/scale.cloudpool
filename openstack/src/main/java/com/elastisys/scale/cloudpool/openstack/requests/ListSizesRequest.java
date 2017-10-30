package com.elastisys.scale.cloudpool.openstack.requests;

import java.util.List;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.model.compute.Flavor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.commons.openstack.OSClientFactory;
import com.google.common.collect.ImmutableList;

/**
 * An OpenStack request task that, when executed, retrieves all available
 * instance sizes (or "server flavors" in OpenStack lingo).
 */
public class ListSizesRequest extends AbstractOpenstackRequest<List<Flavor>> {
    static Logger LOG = LoggerFactory.getLogger(ListSizesRequest.class);

    /**
     * Constructs a new {@link ListSizesRequest} task.
     *
     * @param clientFactory
     *            OpenStack API client factory.
     */
    public ListSizesRequest(OSClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public List<Flavor> doRequest(OSClient api) throws ResponseException {
        List<? extends Flavor> flavors = api.compute().flavors().list();
        return ImmutableList.copyOf(flavors);
    }
}
