package com.elastisys.scale.cloudpool.kubernetes.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.impl.StandardApiServerClient;

/**
 * Main class for starting the REST API server for a
 * {@link KubernetesCloudPool}.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

        KubernetesCloudPool cloudPool = new KubernetesCloudPool(new StandardApiServerClient(), executor);
        CloudPoolServer.main(cloudPool, args);
    }
}
