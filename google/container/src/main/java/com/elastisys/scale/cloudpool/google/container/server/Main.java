package com.elastisys.scale.cloudpool.google.container.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.google.container.GoogleContainerEngineCloudPool;
import com.elastisys.scale.cloudpool.google.container.client.impl.StandardContainerClusterClient;

/**
 * Main class for launching a Google Container Engine cloud pool server.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
        CloudPool cloudPool = new GoogleContainerEngineCloudPool(new StandardContainerClusterClient(), executor);

        CloudPoolServer.main(cloudPool, args);
    }
}
