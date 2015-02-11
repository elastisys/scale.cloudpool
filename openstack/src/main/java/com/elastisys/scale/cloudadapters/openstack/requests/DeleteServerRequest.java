package com.elastisys.scale.cloudadapters.openstack.requests;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.List;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.NotFoundException;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroupConfig;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.google.common.base.Predicates;

/**
 * OpenStack task that, when executed, deletes a server instance, releases any
 * allocated floating IP addresses and waits for the server to be terminated
 * (since this may not be immediate due to eventual consistency).
 */
public class DeleteServerRequest extends AbstractNovaRequest<Void> {
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
	public DeleteServerRequest(OpenStackScalingGroupConfig account,
			String victimId) {
		super(account);
		this.victimId = victimId;
	}

	@Override
	public Void doRequest(NovaApi api) throws NotFoundException {
		// look for victim server in all regions
		ServerApi serverApi = api.getServerApiForZone(getAccount().getRegion());
		Server victimServer = serverApi.get(this.victimId);

		if (victimServer == null) {
			throw new NotFoundException(format(
					"a victim server with id '%s' could not be found "
							+ "in region %s", this.victimId, getAccount()
							.getRegion()));
		}

		releaseFloatingIps(api, getAccount().getRegion(), victimServer);
		boolean wasDeleted = serverApi.delete(this.victimId);
		if (!wasDeleted) {
			throw new ScalingGroupException("failed to delete victim server "
					+ this.victimId);
		}

		try {
			awaitTermination(victimServer.getId());
		} catch (Exception e) {
			throw new ScalingGroupException(String.format(
					"timed out waiting for server %s to be terminated",
					e.getMessage()), e);
		}
		return null;
	}

	private void awaitTermination(String serverId) throws Exception {
		String taskName = String.format("termination-waiter{%s}", serverId);

		ServerExistsRequest serverExistsRequester = new ServerExistsRequest(
				getAccount(), serverId);
		int fixedDelay = 5;
		int maxRetries = 12;
		Retryable<Boolean> awaitTermination = Retryers.fixedDelayRetryer(
				taskName, serverExistsRequester, fixedDelay, SECONDS,
				maxRetries, Predicates.equalTo(false));

		awaitTermination.call();
	}

	/**
	 * Attempts to release any floating IP addresses associated with a server.
	 *
	 * @param api
	 *            The {@link NovaApi}.
	 * @param region
	 *            The region that the server is located in.
	 * @param server
	 *            The server for which to release any assigned floating IP
	 *            address(es).
	 */
	private void releaseFloatingIps(NovaApi api, String region, Server server) {
		LOG.debug("releasing any floating IP addresses associated with '{}'",
				server.getName());
		if (!api.getFloatingIPExtensionForZone(region).isPresent()) {
			LOG.debug("no floating IP API in region '{}', "
					+ "not attempting to release floating IPs.", region);
			return;
		}
		// attempt to release any floating IP addresses associated with
		// server
		FloatingIPApi floatingIpApi = api.getFloatingIPExtensionForZone(region)
				.get();
		List<FloatingIP> floatingIps = newArrayList(floatingIpApi.list());
		for (FloatingIP floatingIP : floatingIps) {
			if (server.getId().equals(floatingIP.getInstanceId())) {
				LOG.debug("releasing floating IP {} from '{}'",
						floatingIP.getIp(), server.getName());
				floatingIpApi.removeFromServer(floatingIP.getIp(),
						server.getId());
				floatingIpApi.delete(floatingIP.getId());
			}
		}
	}
}
