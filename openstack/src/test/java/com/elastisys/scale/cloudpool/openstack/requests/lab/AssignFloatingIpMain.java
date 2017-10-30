package com.elastisys.scale.cloudpool.openstack.requests.lab;

import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.openstack.requests.AssignFloatingIpRequest;
import com.elastisys.scale.cloudpool.openstack.requests.GetServerRequest;
import com.elastisys.scale.commons.openstack.OSClientFactory;

public class AssignFloatingIpMain {
    private static Logger LOG = LoggerFactory.getLogger(AssignFloatingIpMain.class);

    /** TODO: set to server uuid */
    private static final String serverId = "47d3376e-e6e0-4ebd-8ba5-add5d67a6c8e";

    public static void main(String[] args) throws Exception {
        Server server = new GetServerRequest(new OSClientFactory(DriverConfigLoader.loadDefault()), serverId).call();
        String floatingIp = new AssignFloatingIpRequest(new OSClientFactory(DriverConfigLoader.loadDefault()), server)
                .call();
        LOG.info("assigned floating IP {} to server {}", floatingIp, serverId);
    }
}
