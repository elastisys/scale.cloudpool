package com.elastisys.scale.cloudpool.google.container.lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.container.Container;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.ListClustersResponse;

public class ListClusters extends BaseLabProgram {
    private static final Logger LOG = LoggerFactory.getLogger(ListClusters.class);

    /** The project under which the instance group was created. */
    private static String project = System.getenv("GOOGLE_PROJECT");
    /** TODO: the name of the zone where the instance is located. */
    private static String zone = "europe-west1-c";

    public static void main(String[] args) throws Exception {

        Container apiClient = containerApiClient();
        ListClustersResponse response = apiClient.projects().zones().clusters().list(project, zone).execute();
        for (Cluster cluster : response.getClusters()) {
            LOG.info("cluster: {}", cluster.toPrettyString());
        }
    }
}
