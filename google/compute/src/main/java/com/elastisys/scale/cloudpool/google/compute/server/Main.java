package com.elastisys.scale.cloudpool.google.compute.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.elastisys.scale.cloudpool.api.server.CloudPoolOptions;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.cloudpool.google.commons.api.compute.impl.StandardComputeClient;
import com.elastisys.scale.cloudpool.google.compute.driver.GoogleComputeEnginePoolDriver;

/**
 * Main class for launching a GCE cloud pool server.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        CloudPoolOptions options = CloudPoolServer.parseArgs(args);
        StateStorage stateStorage = StateStorage.builder(options.storageDir).build();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

        CloudPoolServer.main(new BaseCloudPool(stateStorage,
                new GoogleComputeEnginePoolDriver(new StandardComputeClient()), executor), args);
    }
}
