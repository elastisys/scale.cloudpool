package com.elastisys.scale.cloudadapters.openstack.requests.lab;

import java.util.List;

import org.jclouds.openstack.nova.v2_0.domain.Flavor;

import com.elastisys.scale.cloudadapters.openstack.requests.ListSizesRequest;

public class ListSizesMain extends AbstractClient {
	public static void main(String[] args) throws Exception {

		ListSizesRequest request = new ListSizesRequest(getAccountConfig());
		List<Flavor> flavors = request.call();
		logger.info("{} available flavors", flavors.size());
		for (Flavor flavor : flavors) {
			logger.info("flavor: {}", flavor);
		}
	}
}
