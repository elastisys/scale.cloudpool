package com.elastisys.scale.cloudpool.openstack.requests;

import java.util.List;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Image;

import com.elastisys.scale.commons.openstack.OSClientFactory;
import com.google.common.collect.Lists;

/**
 * An OpenStack request task that, when executed, retrieves all available
 * images.
 */
public class ListImagesRequest extends AbstractOpenstackRequest<List<Image>> {

	/**
	 * Constructs a new {@link ListImagesRequest} task.
	 *
	 * @param clientFactory
	 *            OpenStack API client factory.
	 *
	 */
	public ListImagesRequest(OSClientFactory clientFactory) {
		super(clientFactory);
	}

	@Override
	public List<Image> doRequest(OSClient api) {
		List<Image> images = Lists.newArrayList(api.compute().images().list(true));
		return images;
	}
}
