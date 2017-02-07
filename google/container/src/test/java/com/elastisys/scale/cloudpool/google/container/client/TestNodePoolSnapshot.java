package com.elastisys.scale.cloudpool.google.container.client;

import static com.elastisys.scale.cloudpool.google.container.testutils.InstanceGroupBuilder.instanceGroup;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.google.api.services.compute.model.Instance;
import com.google.api.services.container.model.NodePool;

/**
 * Exercises {@link NodePoolSnapshot}.
 */
public class TestNodePoolSnapshot {

    /**
     * Make sure getters return proper objects.
     */
    @Test
    public void basicSanity() {
        InstanceGroupSnapshot group1 = instanceGroup("p1-g1").withSize(1).with(instance("p1-g1-i1")).build();
        InstanceGroupSnapshot group2 = instanceGroup("p1-g2").withSize(2).with(instance("p1-g2-i1"))
                .with(instance("p1-g2-i2")).build();

        NodePoolSnapshot nodePool = new NodePoolSnapshot(metadata(), Arrays.asList(group1, group2));
        assertThat(nodePool.getMetadata(), is(metadata()));
        assertThat(nodePool.getInstanceGroups(), is(Arrays.asList(group1, group2)));
    }

    /**
     * Only metadata is mandatory, defaults are provided for the other fields.
     */
    @Test
    public void defaults() {
        List<InstanceGroupSnapshot> nullInstanceGroups = null;
        NodePoolSnapshot nodePool = new NodePoolSnapshot(metadata(), nullInstanceGroups);

        // check default values
        assertThat(nodePool.getInstanceGroups(), is(Collections.emptyList()));
    }

    /**
     * {@link NodePoolSnapshot} requires metadata to be set.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createWithoutMetadata() {
        NodePool nullMetadata = null;
        new NodePoolSnapshot(nullMetadata, Collections.emptyList());
    }

    public Instance instance(String name) {
        return new Instance().setName(name);
    }

    private NodePool metadata() {
        return new NodePool().setName("my-nodepool");
    }

}
