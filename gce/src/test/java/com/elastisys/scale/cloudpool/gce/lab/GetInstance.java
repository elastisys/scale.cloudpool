package com.elastisys.scale.cloudpool.gce.lab;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.gce.driver.client.impl.StandardGceClient;
import com.elastisys.scale.cloudpool.gce.driver.config.CloudApiSettings;
import com.google.api.services.compute.model.Instance;

/**
 * Retrieves metadata about a {@link Instance}.
 */
public class GetInstance extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(GetInstance.class);

    /** The project under which the instance group was created. */
    private static String project = System.getenv("GCE_PROJECT");
    /** TODO: the name of the zone where the instance is located. */
    private static String zone = "europe-west1-d";

    /** TODO: set to the name of the instance. */
    private static String instanceName = "webserver-farm-s4s0";

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        StandardGceClient client = new StandardGceClient();
        client.configure(new CloudApiSettings(serviceAccountKey.getAbsolutePath(), null));

        Instance instance = client.getInstance(project, zone, instanceName);

        LOG.debug("instance: {}", instance.toPrettyString());
    }

}
