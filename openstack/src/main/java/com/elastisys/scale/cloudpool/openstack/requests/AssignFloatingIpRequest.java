package com.elastisys.scale.cloudpool.openstack.requests;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeFloatingIPService;
import org.openstack4j.model.compute.Addresses;
import org.openstack4j.model.compute.FloatingIP;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.openstack.tasks.ServerIpAddressRequester;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.elastisys.scale.commons.openstack.OSClientFactory;
import com.google.common.base.Predicate;

/**
 * A request that, when called, tries to assign a floating IP address to a
 * {@link Server}. The return value of the task is the assigned IP address.
 * <p/>
 * If no IP address could be assigned, a {@link CloudPoolDriverException} is
 * thrown.
 */
public class AssignFloatingIpRequest extends AbstractOpenstackRequest<String> {
    static final Logger LOG = LoggerFactory.getLogger(AssignFloatingIpRequest.class);

    private final Server server;

    /**
     * Creates a new {@link AssignFloatingIpRequest}.
     *
     * @param clientFactory
     *            OpenStack API client factory.
     * @param server
     */
    public AssignFloatingIpRequest(OSClientFactory clientFactory, Server server) {
        super(clientFactory);
        this.server = server;
    }

    @Override
    public String doRequest(OSClient api) throws CloudPoolDriverException {
        try {
            return assignFloatingIp(api, this.server);
        } catch (Exception e) {
            throw new CloudPoolDriverException(format("failed to assign floating IP " + "address to server \"%s\": %s",
                    this.server.getId(), e.getMessage()), e);
        }
    }

    private String assignFloatingIp(OSClient api, Server server) throws Exception {
        LOG.debug("assigning a floating IP address to {} ...", server.getId());
        // It appears as though a server must have a private IP address before a
        // floating IP can be assigned
        waitForPrivateIpAddress(api, server.getId());

        ComputeFloatingIPService floatingIpApi = api.compute().floatingIps();
        FloatingIP floatingIp = acquireFloatingIp(floatingIpApi);
        String ipAddress = floatingIp.getFloatingIpAddress();
        LOG.debug("assigning floating IP {} to server {}", ipAddress, server.getId());
        floatingIpApi.addFloatingIP(server, ipAddress);
        return ipAddress;
    }

    /**
     * Tries to allocate a free floating IP address. Throws a
     * {@link CloudPoolDriverException} on failure to do so.
     *
     * @param floatingIpApi
     * @return
     */
    private FloatingIP acquireFloatingIp(ComputeFloatingIPService floatingIpApi) throws CloudPoolDriverException {
        for (String floatingIpPool : floatingIpApi.getPoolNames()) {
            LOG.debug("checking floating IP pool {}", floatingIpPool);
            try {
                return floatingIpApi.allocateIP(floatingIpPool);
            } catch (Exception e) {
                LOG.debug("failed to allocate floating IP from {}: {}", floatingIpPool, e.getMessage());
            }
        }
        throw new CloudPoolDriverException("failed to allocate floating IP address for server");
    }

    /**
     * Waits for a given server to be assigned a private IP address.
     *
     * @param api
     * @param serverId
     * @return The IP address(es) of the server.
     * @throws Exception
     */
    private Addresses waitForPrivateIpAddress(OSClient api, String serverId) throws Exception {
        String taskName = String.format("ip-address-waiter{%s}", serverId);
        ServerIpAddressRequester serverIpRequester = new ServerIpAddressRequester(api, serverId);
        int fixedDelay = 6;
        int maxRetries = 10;
        Retryable<Addresses> retryer = Retryers.fixedDelayRetryer(taskName, serverIpRequester, fixedDelay, SECONDS,
                maxRetries, ipAddressAssigned());

        return retryer.call();
    }

    /**
     * Returns a {@link Predicate} that is satisfied by any {@link Addresses}
     * object that contains at least one IP address.
     *
     * @return
     */
    private Predicate<Addresses> ipAddressAssigned() {
        return addresses -> !addresses.getAddresses().isEmpty();
    }
}
