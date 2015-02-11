package com.elastisys.scale.cloudadapters.openstack.requests;

import java.util.List;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Flavor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroupConfig;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * An OpenStack request task that, when executed, retrieves all available
 * instance sizes, (or "server flavors" ).
 */
public class ListSizesRequest extends AbstractNovaRequest<List<Flavor>> {
	static Logger LOG = LoggerFactory.getLogger(ListSizesRequest.class);

	/**
	 * Constructs a new {@link ListSizesRequest} task.
	 *
	 * @param account
	 *            Account login credentials for a particular OpenStack endpoint.
	 *
	 */
	public ListSizesRequest(OpenStackScalingGroupConfig account) {
		super(account);
	}

	@Override
	public List<Flavor> doRequest(NovaApi api) {
		List<Flavor> flavors = Lists.newArrayList();
		Iterable<? extends Flavor> regionFlavors = api
				.getFlavorApiForZone(getAccount().getRegion()).listInDetail()
				.concat();
		Iterables.addAll(flavors, regionFlavors);
		return flavors;
	}
}
