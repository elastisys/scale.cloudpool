package com.elastisys.scale.cloudpool.openstack.requests.lab;

import java.util.List;

import org.openstack4j.model.compute.Flavor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.openstack.requests.ListSizesRequest;
import com.elastisys.scale.commons.openstack.OSClientFactory;

public class ListSizesMain {
    private static Logger LOG = LoggerFactory.getLogger(ListSizesMain.class);

    public static void main(String[] args) {
        try {
            List<Flavor> flavors = new ListSizesRequest(new OSClientFactory(DriverConfigLoader.loadDefault())).call();
            LOG.info("{} server flavor(s) found", flavors.size());
            for (Flavor flavor : flavors) {
                LOG.info("flavor: {}", flavor);
            }
        } catch (Exception e) {
            LOG.error("error type: {}", e.getClass().getName());
            LOG.error("error: {}", e.getMessage());
        }
    }
}
