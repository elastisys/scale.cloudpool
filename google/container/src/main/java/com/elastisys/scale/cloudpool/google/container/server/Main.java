package com.elastisys.scale.cloudpool.google.container.server;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.google.container.GoogleContainerEngineCloudPool;
import com.elastisys.scale.cloudpool.google.container.client.impl.StandardContainerClusterClient;

/**
 * Main class for launching a Google Container Engine cloud pool server.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        CloudPool cloudPool = new GoogleContainerEngineCloudPool(new StandardContainerClusterClient());
        CloudPoolServer.main(cloudPool, args);
    }
}
