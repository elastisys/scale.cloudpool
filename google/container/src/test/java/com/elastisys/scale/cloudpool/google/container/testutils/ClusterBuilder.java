package com.elastisys.scale.cloudpool.google.container.testutils;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import com.elastisys.scale.cloudpool.google.container.client.ClusterSnapshot;
import com.elastisys.scale.cloudpool.google.container.client.NodePoolSnapshot;
import com.google.api.services.container.model.Cluster;

public class ClusterBuilder {
    private Cluster metadata = new Cluster().setName("my-cluster").setSelfLink(
            "https://container.googleapis.com/v1/projects/my-project/zones/europe-west1-c/clusters/my-cluster");
    private List<NodePoolSnapshot> nodePools = new ArrayList<>();
    private DateTime timestamp;

    public static ClusterBuilder cluster() {
        return new ClusterBuilder();
    }

    public ClusterSnapshot build() {
        return new ClusterSnapshot(this.metadata, this.nodePools);
    }

    public ClusterBuilder with(Cluster metadata) {
        this.metadata = metadata;
        return this;
    }

    public ClusterBuilder with(NodePoolSnapshot nodePool) {
        this.nodePools.add(nodePool);
        return this;
    }

    public ClusterBuilder with(DateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}