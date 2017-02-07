package com.elastisys.scale.cloudpool.google.container.testutils;

import java.util.ArrayList;
import java.util.List;

import com.elastisys.scale.cloudpool.google.container.client.InstanceGroupSnapshot;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceGroupManager;

public class InstanceGroupBuilder {
    private InstanceGroupManager metadata = new InstanceGroupManager();
    private List<Instance> instances = new ArrayList<>();

    public static InstanceGroupBuilder instanceGroup(String name) {
        return new InstanceGroupBuilder().with(new InstanceGroupManager().setName(name)
                .setSelfLink(
                        "https://www.googleapis.com/compute/v1/projects/my-project/zones/europe-west1-c/instanceGroupManagers/"
                                + name)
                .setTargetSize(0));
    }

    public InstanceGroupSnapshot build() {
        return new InstanceGroupSnapshot(this.metadata, this.instances);
    }

    public InstanceGroupBuilder withSize(int targetSize) {
        this.metadata.setTargetSize(targetSize);
        return this;
    }

    public InstanceGroupBuilder with(InstanceGroupManager metadata) {
        this.metadata = metadata;
        return this;
    }

    public InstanceGroupBuilder with(Instance instance) {
        this.instances.add(instance);
        return this;
    }
}