package com.elastisys.scale.cloudadapters.openstack.requests;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.List;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapters.openstack.faults.FloatingIpAddressException;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroupConfig;
import com.elastisys.scale.cloudadapters.openstack.tasks.ServerIpAddressRequester;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * A request that, when called, tries to assign a floating IP address to a
 * {@link Server}. The return value of the task is the assigned IP address.
 * <p/>
 * If no IP address could be assigned, a {@link FloatingIpAddressException} is
 * thrown.
 *
 *
 *
 */
public class AssignFloatingIpRequest extends AbstractNovaRequest<String> {
	static final Logger LOG = LoggerFactory
			.getLogger(AssignFloatingIpRequest.class);

	private final Server server;

	public AssignFloatingIpRequest(OpenStackScalingGroupConfig account,
			Server server) {
		super(account);
		this.server = server;
	}

	@Override
	public String doRequest(NovaApi api) throws FloatingIpAddressException {
		try {
			return assignFloatingIp(api, this.server);
		} catch (Exception e) {
			throw new FloatingIpAddressException(format(
					"failed to assign floating IP "
							+ "address to server \"%s\": %s",
					this.server.getId(), e.getMessage()), e);
		}
	}

	private String assignFloatingIp(NovaApi api, Server server)
			throws Exception {
		LOG.debug("assigning a floating IP address to {} ...", server.getId());
		ServerApi serverApi = api.getServerApiForZone(getAccount().getRegion());
		// In OpenStack Grizzly, it appears like server must have a private IP
		// address before a floating IP can be assigned
		waitForPrivateIpAddress(serverApi, server);

		FloatingIPApi floatingIPApi = api.getFloatingIPExtensionForZone(
				getAccount().getRegion()).get();
		List<FloatingIP> floatingIps = newArrayList(floatingIPApi.list());
		List<FloatingIP> freeFloatingIps = getFreeFloatingIps(floatingIps);
		if (freeFloatingIps.isEmpty()) {
			freeFloatingIps.add(floatingIPApi.create());
		}
		if (freeFloatingIps.isEmpty()) {
			throw new FloatingIpAddressException(
					"no floating IP address(es) available");
		}
		FloatingIP ipToAllocate = Iterables.getLast(freeFloatingIps);
		String ip = ipToAllocate.getIp();
		LOG.debug("assigning floating ip {} to server {}", ip, server.getId());
		floatingIPApi.addToServer(ip, server.getId());
		return ip;
	}

	/**
	 * Waits for a given server to be assigned a private IP address.
	 *
	 * @param serverApi
	 * @param server
	 * @return The IP address(es) of the server.
	 * @throws Exception
	 */
	private Multimap<String, Address> waitForPrivateIpAddress(
			ServerApi serverApi, Server server) throws Exception {
		String taskName = String
				.format("ip-address-waiter{%s}", server.getId());
		ServerIpAddressRequester serverIpRequester = new ServerIpAddressRequester(
				serverApi, server.getId());
		int fixedDelay = 6;
		int maxRetries = 10;
		Retryable<Multimap<String, Address>> retryer = Retryers
				.fixedDelayRetryer(taskName, serverIpRequester, fixedDelay,
						SECONDS, maxRetries, ipAddressAssigned());

		return retryer.call();
	}

	private Predicate<Multimap<String, Address>> ipAddressAssigned() {
		return new Predicate<Multimap<String, Address>>() {
			@Override
			public boolean apply(Multimap<String, Address> addresses) {
				return !addresses.isEmpty();
			}
		};
	}

	/**
	 * Filters out all allocated addresses from a collection of floating IPs.
	 *
	 * @param floatingIps
	 *            A list of floating IP addresses.
	 * @return All unassigned floating IP addresses from the list.
	 */
	private List<FloatingIP> getFreeFloatingIps(List<FloatingIP> floatingIps) {
		List<FloatingIP> unassigned = Lists.newArrayList();
		for (FloatingIP floatingIP : floatingIps) {
			if (floatingIP.getInstanceId() == null) {
				unassigned.add(floatingIP);
			}
		}
		return unassigned;
	}
}
