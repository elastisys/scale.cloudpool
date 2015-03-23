package com.elastisys.scale.cloudpool.openstack.requests;

import java.util.List;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.openstack.driver.config.OpenStackPoolDriverConfig;
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
	 * @param account
	 *            Account login credentials for a particular OpenStack endpoint.
	 *
	 */
	public ListSizesRequest(OpenStackPoolDriverConfig account) {
		super(account);
	}

	@Override
	public List<Flavor> doRequest(OSClient api) {
		List<? extends Flavor> flavors = api.compute().flavors().list();
		return ImmutableList.copyOf(flavors);
	}
}
