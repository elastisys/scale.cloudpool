package com.elastisys.scale.cloudpool.commons.basepool.server;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolOptions;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DummyCloudPoolDriver;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Starts a {@link BaseCloudPool} server with a dummy backend driver.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        CloudPoolOptions options = CloudPoolServer.parseArgs(args);
        StateStorage stateStorage = StateStorage.builder(options.storageDir).build();
        DummyCloudPoolDriver driver = new DummyCloudPoolDriver();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
        CloudPoolServer.main(new BaseCloudPool(stateStorage, driver, executor), args);
    }
}
