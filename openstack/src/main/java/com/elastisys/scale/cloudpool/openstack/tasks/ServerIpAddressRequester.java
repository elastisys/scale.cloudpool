package com.elastisys.scale.cloudpool.openstack.tasks;

import java.util.concurrent.Callable;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Addresses;

import com.elastisys.scale.commons.net.retryable.Retryable;

/**
 * A {@link Requester} that returns the IP addresses for a given Openstack
 * server instance.
 * <p/>
 * It can, for example, be used in a {@link Retryable} to wait for a server to
 * be assigned a (private) IP address.
 *
 * @see Retryable
 * @see RetryUntilAssignedIpAddress
 */
public class ServerIpAddressRequester implements Callable<Addresses> {

    private final OSClient api;
    private final String serverId;

    /**
     * Constructs a new {@link ServerIpAddressRequester} task.
     *
     * @param api
     *            An Openstack API client.
     * @param serverId
     *            The identifier of the server instance whose IP addresses are
     *            to be retrieved.
     */
    public ServerIpAddressRequester(OSClient api, String serverId) {
        this.api = api;
        this.serverId = serverId;
    }

    @Override
    public Addresses call() throws Exception {
        return this.api.compute().servers().get(this.serverId).getAddresses();
    }
}
