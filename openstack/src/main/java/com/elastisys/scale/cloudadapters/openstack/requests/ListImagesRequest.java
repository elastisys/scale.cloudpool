package com.elastisys.scale.cloudadapters.openstack.requests;

import java.util.List;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Image;
import org.jclouds.openstack.nova.v2_0.features.ImageApi;

import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroupConfig;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * An OpenStack request task that, when executed, retrieves all available
 * images.
 *
 * 
 *
 */
public class ListImagesRequest extends AbstractNovaRequest<List<Image>> {

	/**
	 * Constructs a new {@link ListImagesRequest} task.
	 *
	 * @param account
	 *            Account login credentials for a particular OpenStack endpoint.
	 *
	 */
	public ListImagesRequest(OpenStackScalingGroupConfig account) {
		super(account);
	}

	@Override
	public List<Image> doRequest(NovaApi api) {
		List<Image> images = Lists.newArrayList();
		ImageApi imageApi = api.getImageApiForZone(getAccount().getRegion());
		Iterable<? extends Image> regionImages = imageApi.listInDetail()
				.concat();
		Iterables.addAll(images, regionImages);
		return images;
	}
}
