package com.elastisys.scale.cloudpool.vsphere.server;

import com.elastisys.scale.cloudpool.api.server.CloudPoolOptions;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.vsphere.client.impl.StandardVsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.VspherePoolDriver;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Main {
    public static void main(String[] args) throws Exception {
        CloudPoolOptions options = CloudPoolServer.parseArgs(args);
        StateStorage stateStorage = StateStorage.builder(options.storageDir).build();
        CloudPoolDriver driver = new VspherePoolDriver(new StandardVsphereClient());
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

        CloudPoolServer.main(new BaseCloudPool(stateStorage, driver, executor), args);
    }
}