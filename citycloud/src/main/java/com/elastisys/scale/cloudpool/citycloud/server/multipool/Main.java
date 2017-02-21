package com.elastisys.scale.cloudpool.citycloud.server.multipool;

import java.io.File;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.multipool.impl.DiskBackedMultiCloudPool;
import com.elastisys.scale.cloudpool.multipool.server.MultiCloudPoolOptions;
import com.elastisys.scale.cloudpool.multipool.server.MultiCloudPoolServer;

/**
 * Main class that starts a REST API {@link MultiCloudPoolServer} for a
 * collection of CityCloud {@link CloudPool} instances.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        MultiCloudPoolOptions options = MultiCloudPoolServer.parseArgs(args);

        MultiCloudPoolServer.main(
                new DiskBackedMultiCloudPool(new File(options.storageDir), new CityCloudCloudPoolFactory()), args);
    }
}
