package com.elastisys.scale.cloudpool.kubernetes.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;
import com.elastisys.scale.cloudpool.kubernetes.client.impl.StandardKubernetesClient;
import com.elastisys.scale.cloudpool.kubernetes.client.impl.http.impl.AuthenticatingHttpApiClient;

/**
 * Main class for starting the REST API server for a Kubernetes
 * ReplicationController {@link CloudPool}.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

        KubernetesCloudPool cloudPool = new KubernetesCloudPool(
                new StandardKubernetesClient(new AuthenticatingHttpApiClient()), executor);
        CloudPoolServer.main(cloudPool, args);
    }
}
