package com.elastisys.scale.cloudpool.citycloud.server;

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
 * Main class for starting the REST API server for an OpenStack
 * {@link CloudPool}.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        CloudPoolOptions options = CloudPoolServer.parseArgs(args);
        StateStorage stateStorage = StateStorage.builder(options.storageDir).build();
        CloudPoolDriver openstackDriver = new OpenStackPoolDriver(new StandardOpenstackClient(),
                CloudProviders.CITYCLOUD);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

        CloudPoolServer.main(new BaseCloudPool(stateStorage, openstackDriver, executor), args);
    }
}
