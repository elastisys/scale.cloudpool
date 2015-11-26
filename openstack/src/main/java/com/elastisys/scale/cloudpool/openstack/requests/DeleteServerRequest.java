package com.elastisys.scale.cloudpool.openstack.requests;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeFloatingIPService;
import org.openstack4j.api.compute.ServerService;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.FloatingIP;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.openstack.driver.client.OSClientFactory;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * OpenStack task that, when executed, deletes a {@link Server}, releases any
 * allocated floating IP addresses and waits for the server to be terminated
 * (since this may not be immediate due to eventual consistency).
 */
public class DeleteServerRequest extends AbstractOpenstackRequest<Void> {
	static final Logger LOG = LoggerFactory
			.getLogger(DeleteServerRequest.class);

	/** The identifier (UUID) of the server instance to delete. */
	private final String victimId;

	/**
	 * Creates a new {@link DeleteServerRequest}.
	 *
	 * @param clientFactory
	 *            OpenStack API client factory.
	 * @param victimId
	 *            The identifier (UUID) of the server instance to delete.
	 */
	public DeleteServerRequest(OSClientFactory clientFactory, String victimId) {
		super(clientFactory);
		this.victimId = victimId;
	}

	@Override
	public Void doRequest(OSClient api) throws NotFoundException {
		// look for victim server in all regions
		ServerService serverApi = api.compute().servers();
		Server victimServer = serverApi.get(this.victimId);
		if (victimServer == null) {
			throw new NotFoundException(format(
					"a victim server with id '%s' could not be found "
							+ "in region %s",
					this.victimId, getApiAccessConfig().getRegion()));
		}

		releaseFloatingIps(api, victimServer);
		ActionResponse response = serverApi.delete(this.victimId);
		if (!response.isSuccess()) {
			throw new CloudPoolDriverException(
					"failed to delete victim server " + this.victimId);
		}

		try {
			awaitTermination(victimServer.getId());
		} catch (Exception e) {
			throw new CloudPoolDriverException(String.format(
					"timed out waiting for server %s to be terminated",
					e.getMessage()), e);
		}
		return null;
	}

	private void awaitTermination(String serverId) throws Exception {
		String taskName = String.format("termination-waiter{%s}", serverId);

		ServerExistsRequest serverExistsRequester = new ServerExistsRequest(
				getClientFactory(), serverId);
		int fixedDelay = 5;
		int maxRetries = 12;
		Retryable<Boolean> awaitTermination = Retryers.fixedDelayRetryer(
				taskName, serverExistsRequester, fixedDelay, SECONDS,
				maxRetries, Predicates.equalTo(false));

		awaitTermination.call();
	}

	/**
	 * Releases any floating IP addresses associated with a {@link Server}.
	 *
	 * @param api
	 *            An OpenStack API client.
	 * @param server
	 *            The server for which to release any assigned floating IP
	 *            address(es).
	 */
	private void releaseFloatingIps(OSClient api, Server server) {
		LOG.debug("releasing floating IP addresses associated with {}",
				server.getName());
		ComputeFloatingIPService floatingIpApi = api.compute().floatingIps();
		Map<String, FloatingIP> tenantFloatingIps = tenantFloatingIps(
				floatingIpApi);
		Collection<String> serverIps = serverIps(server);
		for (String serverIp : serverIps) {
			if (tenantFloatingIps.containsKey(serverIp)) {
				FloatingIP floatingIp = tenantFloatingIps.get(serverIp);
				LOG.debug("releasing floating IP {} from '{}'",
						floatingIp.getFloatingIpAddress(), server.getName());
				floatingIpApi.removeFloatingIP(server,
						floatingIp.getFloatingIpAddress());
				floatingIpApi.deallocateIP(floatingIp.getId());
			}
		}
	}

	/**
	 * Returns the collection of IP addresses (both public and private)
	 * associated with a {@link Server}.
	 *
	 * @param server
	 * @return
	 */
	private Collection<String> serverIps(Server server) {
		List<String> serverIps = Lists.newLinkedList();
		for (List<? extends Address> addressGroup : server.getAddresses()
				.getAddresses().values()) {
			for (Address address : addressGroup) {
				serverIps.add(address.getAddr());
			}
		}
		return serverIps;
	}

	/**
	 * Returns all floating IP addresses allocated to the tenant.
	 *
	 * @param floatingIpApi
	 * @return A {@link Map} of floating IP addresses, where keys are IP
	 *         addresses and values are {@link FloatingIP} objects.
	 */
	private Map<String, FloatingIP> tenantFloatingIps(
			ComputeFloatingIPService floatingIpApi) {
		Map<String, FloatingIP> ipToFloatingIp = Maps.newHashMap();
		List<? extends FloatingIP> floatingIps = floatingIpApi.list();
		for (FloatingIP floatingIP : floatingIps) {
			ipToFloatingIp.put(floatingIP.getFloatingIpAddress(), floatingIP);
		}
		return ipToFloatingIp;
	}
}
