package com.elastisys.scale.cloudadapters.openstack.tasks;

import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;

import com.elastisys.scale.commons.net.retryable.Requester;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;
import com.google.common.collect.Multimap;

/**
 * A {@link Requester} that returns the IP addresses for a given Openstack
 * server instance.
 * <p/>
 * It can, for example, be used in a {@link RetryableRequest} in concert with
 * the {@link RetryUntilAssignedIpAddress} to wait for a server to be assigned a
 * (private) IP address.
 * 
 * @see RetryableRequest
 * @see RetryUntilAssignedIpAddress
 * 
 * 
 * 
 */
public class ServerIpAddressRequester implements
		Requester<Multimap<String, Address>> {

	private final ServerApi serverApi;
	private final String serverId;

	/**
	 * Constructs a new {@link ServerIpAddressRequester} task.
	 * 
	 * @param serverApi
	 *            The Openstack Nova {@link ServerApi}.
	 * @param serverId
	 *            The identifier of the server instance whose IP addresses are
	 *            to be retrieved.
	 */
	public ServerIpAddressRequester(ServerApi serverApi, String serverId) {
		this.serverApi = serverApi;
		this.serverId = serverId;
	}

	@Override
	public Multimap<String, Address> call() throws Exception {
		return this.serverApi.get(this.serverId).getAddresses();
	}
}
