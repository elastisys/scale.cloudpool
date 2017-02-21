package com.elastisys.scale.cloudpool.aws.autoscaling.server.multipool;

import java.io.File;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.multipool.impl.DiskBackedMultiCloudPool;
import com.elastisys.scale.cloudpool.multipool.server.MultiCloudPoolOptions;
import com.elastisys.scale.cloudpool.multipool.server.MultiCloudPoolServer;

/**
 * Main class that starts a REST API {@link MultiCloudPoolServer} for a
 * collection of AWS Auto Scaling Group {@link CloudPool} instances.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        MultiCloudPoolOptions options = MultiCloudPoolServer.parseArgs(args);

        MultiCloudPoolServer
                .main(new DiskBackedMultiCloudPool(new File(options.storageDir), new AwsAsCloudPoolFactory()), args);
    }
}
