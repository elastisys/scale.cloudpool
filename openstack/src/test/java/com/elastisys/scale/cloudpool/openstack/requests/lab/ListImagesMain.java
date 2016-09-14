package com.elastisys.scale.cloudpool.openstack.requests.lab;

import java.util.List;

import org.openstack4j.model.compute.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.openstack.requests.ListImagesRequest;
import com.elastisys.scale.commons.openstack.OSClientFactory;

public class ListImagesMain {
    private static Logger LOG = LoggerFactory.getLogger(ListImagesMain.class);

    public static void main(String[] args) {
        List<Image> images = new ListImagesRequest(
                new OSClientFactory(DriverConfigLoader.loadDefault().toApiAccessConfig())).call();
        LOG.info("{} server image(s) found", images.size());
        for (Image image : images) {
            LOG.info("image: {}", image);
        }
    }
}
