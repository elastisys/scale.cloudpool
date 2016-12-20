package com.elastisys.scale.cloudpool.gce.server;

import com.elastisys.scale.cloudpool.api.server.CloudPoolOptions;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.cloudpool.gce.driver.GcePoolDriver;
import com.elastisys.scale.cloudpool.gce.driver.client.impl.StandardGceClient;

/**
 * Main class for launching a GCE cloud pool server.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        CloudPoolOptions options = CloudPoolServer.parseArgs(args);
        StateStorage stateStorage = StateStorage.builder(options.storageDir).build();
        CloudPoolServer.main(
                new BaseCloudPool(stateStorage, new GcePoolDriver(new StandardGceClient())), args);
    }
}
