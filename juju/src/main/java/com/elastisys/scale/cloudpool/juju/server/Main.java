package com.elastisys.scale.cloudpool.juju.server;

import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.juju.JujuCloudPool;
import com.elastisys.scale.cloudpool.juju.client.impl.CommandLineJujuClient;

/**
 * Main class for starting the REST API server for a Juju
 * {@link CloudPool}.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        JujuCloudPool cloudPool = new JujuCloudPool(
                new CommandLineJujuClient());
        CloudPoolServer.main(cloudPool, args);
    }
}
