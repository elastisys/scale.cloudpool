package com.elastisys.scale.cloudpool.openstack.requests;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeFloatingIPService;
import org.openstack4j.api.compute.ServerService;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.compute.FloatingIP;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.openstack.driver.config.OpenStackPoolDriverConfig;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.google.common.base.Predicates;

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
	 * @param account
	 *            Account login credentials for a particular OpenStack Nova
	 *            endpoint.
	 * @param victimId
	 *            The identifier (UUID) of the server instance to delete.
	 */
	public DeleteServerRequest(OpenStackPoolDriverConfig account,
			String victimId) {
		super(account);
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
							+ "in region %s", this.victimId, getAccessConfig()
							.getRegion()));
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
				getAccessConfig(), serverId);
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

		for (FloatingIP floatingIp : floatingIpApi.list()) {
			String assignedTo = floatingIp.getInstanceId();
			if (assignedTo != null && assignedTo.equals(server.getId())) {
				LOG.debug("releasing floating IP {} from '{}'",
						floatingIp.getFloatingIpAddress(), server.getName());
				floatingIpApi.removeFloatingIP(server,
						floatingIp.getFloatingIpAddress());
				floatingIpApi.deallocateIP(floatingIp.getId());
			}
		}
	}
}
