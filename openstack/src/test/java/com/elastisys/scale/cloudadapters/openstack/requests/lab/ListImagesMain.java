package com.elastisys.scale.cloudadapters.openstack.requests.lab;

import java.util.List;

import org.jclouds.openstack.nova.v2_0.domain.Image;

import com.elastisys.scale.cloudadapters.openstack.requests.ListImagesRequest;

public class ListImagesMain extends AbstractClient {
	public static void main(String[] args) throws Exception {

		ListImagesRequest request = new ListImagesRequest(getAccountConfig());
		List<Image> images = request.call();
		logger.info("{} available images", images.size());
		for (Image image : images) {
			logger.info("flavor: {}", image);
		}
	}
}
