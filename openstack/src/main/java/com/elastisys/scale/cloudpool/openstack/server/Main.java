package com.elastisys.scale.cloudpool.openstack.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolOptions;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.api.types.CloudProviders;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.openstack.driver.OpenStackPoolDriver;
import com.elastisys.scale.cloudpool.openstack.driver.client.StandardOpenstackClient;

/**
 * Main class that starts a REST API {@link CloudPoolServer} for an OpenStack
 * {@link CloudPool}.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        CloudPoolOptions options = CloudPoolServer.parseArgs(args);
        StateStorage stateStorage = StateStorage.builder(options.storageDir).build();
        CloudPoolDriver openstackDriver = new OpenStackPoolDriver(new StandardOpenstackClient(),
                CloudProviders.OPENSTACK);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

        CloudPoolServer.main(new BaseCloudPool(stateStorage, openstackDriver, executor), args);
    }
}
