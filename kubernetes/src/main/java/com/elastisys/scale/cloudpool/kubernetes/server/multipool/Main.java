package com.elastisys.scale.cloudpool.kubernetes.server.multipool;

import java.io.File;

import com.elastisys.scale.cloudpool.multipool.impl.DiskBackedMultiCloudPool;
import com.elastisys.scale.cloudpool.multipool.server.MultiCloudPoolOptions;
import com.elastisys.scale.cloudpool.multipool.server.MultiCloudPoolServer;

/**
 * Main class that starts a REST API {@link MultiCloudPoolServer} for a
 * {@link KubernetesCloudPoolFactory}.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        MultiCloudPoolOptions options = MultiCloudPoolServer.parseArgs(args);

        MultiCloudPoolServer.main(
                new DiskBackedMultiCloudPool(new File(options.storageDir), new KubernetesCloudPoolFactory()), args);
    }
}
