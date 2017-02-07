package com.elastisys.scale.cloudpool.google.container.testutils;

import java.util.ArrayList;
import java.util.List;

import com.elastisys.scale.cloudpool.google.container.client.InstanceGroupSnapshot;
import com.elastisys.scale.cloudpool.google.container.client.NodePoolSnapshot;
import com.google.api.services.container.model.NodePool;

public class NodePoolBuilder {
    private NodePool metadata = new NodePool().setSelfLink(
            "https://container.googleapis.com/v1/projects/my-project/zones/europe-west1-c/clusters/my-cluster/nodePools/my-pool");
    private List<InstanceGroupSnapshot> instanceGroups = new ArrayList<>();

    public static NodePoolBuilder nodePool(String name) {
        return new NodePoolBuilder().with(new NodePool().setName(name).setSelfLink(
                "https://container.googleapis.com/v1/projects/my-project/zones/europe-west1-c/clusters/my-cluster/nodePools/"
                        + name));
    }

    public NodePoolSnapshot build() {
        return new NodePoolSnapshot(this.metadata, this.instanceGroups);
    }

    public NodePoolBuilder with(NodePool metadata) {
        this.metadata = metadata;
        return this;
    }

    public NodePoolBuilder with(InstanceGroupSnapshot instanceGroup) {
        this.instanceGroups.add(instanceGroup);
        return this;
    }
}