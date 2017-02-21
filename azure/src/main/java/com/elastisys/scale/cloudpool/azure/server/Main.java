package com.elastisys.scale.cloudpool.azure.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolOptions;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.azure.driver.AzurePoolDriver;
import com.elastisys.scale.cloudpool.azure.driver.client.impl.StandardAzureClient;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;

/**
 * Main class for starting the REST API server for the AWS Spot Instance
 * {@link CloudPool}.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        CloudPoolOptions options = CloudPoolServer.parseArgs(args);
        StateStorage stateStorage = StateStorage.builder(options.storageDir).build();
        CloudPoolDriver driver = new AzurePoolDriver(new StandardAzureClient());
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

        CloudPoolServer.main(new BaseCloudPool(stateStorage, driver, executor), args);
    }
}
