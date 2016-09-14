package com.elastisys.scale.cloudpool.kubernetes.server;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;
import com.elastisys.scale.cloudpool.kubernetes.client.impl.StandardKubernetesClient;
import com.elastisys.scale.cloudpool.kubernetes.client.impl.http.impl.AuthenticatingHttpApiClient;

/**
 * Main class for starting the REST API server for an OpenStack
 * {@link CloudPool}.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        KubernetesCloudPool cloudPool = new KubernetesCloudPool(
                new StandardKubernetesClient(new AuthenticatingHttpApiClient()));
        CloudPoolServer.main(cloudPool, args);
    }
}
