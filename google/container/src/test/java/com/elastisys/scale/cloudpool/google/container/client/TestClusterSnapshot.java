package com.elastisys.scale.cloudpool.google.container.client;

import static com.elastisys.scale.cloudpool.google.container.testutils.InstanceGroupBuilder.instanceGroup;
import static com.elastisys.scale.cloudpool.google.container.testutils.NodePoolBuilder.nodePool;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.google.container.testutils.ClusterBuilder;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.container.model.Cluster;

/**
 * Exercises the {@link ClusterSnapshot} class.
 */
public class TestClusterSnapshot {

    @Before
    public void beforeTestMethod() {
        FrozenTime.setFixed(UtcTime.parse("2017-01-01T12:00:00.000Z"));
    }

    /**
     * Make sure getters return proper objects.
     */
    @Test
    public void basicSanity() {
        InstanceGroupSnapshot pool1Group1 = instanceGroup("p1-g1").withSize(1).build();
        InstanceGroupSnapshot pool1Group2 = instanceGroup("p1-g2").withSize(2).build();
        NodePoolSnapshot nodePool1 = nodePool("p1").with(pool1Group1).with(pool1Group2).build();
        InstanceGroupSnapshot pool2Group1 = instanceGroup("p2-g1").withSize(3).build();
        InstanceGroupSnapshot pool2Group2 = instanceGroup("p2-g2").withSize(4).build();
        NodePoolSnapshot nodePool2 = nodePool("p2").with(pool2Group1).with(pool2Group2).build();

        DateTime timestamp = UtcTime.parse("2017-02-01T13:00:00.000Z");
        ClusterSnapshot clusterSnapshot = new ClusterSnapshot(metadata(), Arrays.asList(nodePool1, nodePool2),
                timestamp);
        assertThat(clusterSnapshot.getMetadata(), is(metadata()));
        assertThat(clusterSnapshot.getNodePools(), is(Arrays.asList(nodePool1, nodePool2)));
        assertThat(clusterSnapshot.getTimestamp(), is(timestamp));
    }

    /**
     * Only metadata is mandatory, defaults are provided for the other fields.
     */
    @Test
    public void defaults() {
        List<NodePoolSnapshot> nullNodePools = null;
        DateTime nullTimestamp = null;
        ClusterSnapshot clusterSnapshot = new ClusterSnapshot(metadata(), nullNodePools, nullTimestamp);

        // check default values
        assertThat(clusterSnapshot.getNodePools(), is(Collections.emptyList()));
        assertThat(clusterSnapshot.getTimestamp(), is(UtcTime.now()));
    }

    /**
     * {@link ClusterSnapshot} requires metadata to be set.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createWithoutMetadata() {
        Cluster nullMetadata = null;
        new ClusterSnapshot(nullMetadata, Collections.emptyList());
    }

    /**
     * {@link ClusterSnapshot} nodePools may be empty.
     */
    @Test
    public void createWithEmptyNodePools() {
        ClusterSnapshot clusterSnapshot = new ClusterSnapshot(metadata(), Collections.emptyList());
        assertThat(clusterSnapshot.getNodePools(), is(Collections.emptyList()));

        assertThat(clusterSnapshot.getTotalSize(), is(0));
        assertThat(clusterSnapshot.getStartedNodes(), is(Collections.emptyList()));
    }

    /**
     * The total size of the cluster is the sum of all target sizes of its
     * instance groups.
     */
    @Test
    public void totalSize() {
        InstanceGroupSnapshot pool1Group1 = instanceGroup("p1-g1").withSize(1).build();
        InstanceGroupSnapshot pool1Group2 = instanceGroup("p1-g2").withSize(2).build();
        NodePoolSnapshot nodePool1 = nodePool("p1").with(pool1Group1).with(pool1Group2).build();
        InstanceGroupSnapshot pool2Group1 = instanceGroup("p2-g1").withSize(3).build();
        InstanceGroupSnapshot pool2Group2 = instanceGroup("p2-g2").withSize(4).build();
        NodePoolSnapshot nodePool2 = nodePool("p2").with(pool2Group1).with(pool2Group2).build();

        ClusterSnapshot clusterSnapshot = ClusterBuilder.cluster().with(nodePool1).with(nodePool2).build();
        assertThat(clusterSnapshot.getTotalSize(), is(10));
    }

    /**
     * The cluster's started nodes should be the {@link Instance}s of all its
     * instance groups.
     */
    @Test
    public void startedNodes() {
        InstanceGroupSnapshot pool1Group1 = instanceGroup("p1-g1").withSize(1).with(instance("p1-g1-i1")).build();
        InstanceGroupSnapshot pool1Group2 = instanceGroup("p1-g2").withSize(2).with(instance("p1-g2-i1"))
                .with(instance("p1-g2-i2")).build();
        NodePoolSnapshot nodePool1 = nodePool("p1").with(pool1Group1).with(pool1Group2).build();
        InstanceGroupSnapshot pool2Group1 = instanceGroup("p2-g1").withSize(3).with(instance("p2-g1-i1"))
                .with(instance("p2-g1-i2")).with(instance("p2-g1-i3")).build();
        InstanceGroupSnapshot pool2Group2 = instanceGroup("p2-g2").withSize(4).with(instance("p2-g2-i1"))
                .with(instance("p2-g2-i2")).with(instance("p2-g2-i3")).with(instance("p2-g2-i4")).build();
        NodePoolSnapshot nodePool2 = nodePool("p2").with(pool2Group1).with(pool2Group2).build();

        ClusterSnapshot clusterSnapshot = ClusterBuilder.cluster().with(nodePool1).with(nodePool2).build();
        assertThat(clusterSnapshot.getStartedNodes(),
                is(Arrays.asList(//
                        instance("p1-g1-i1"), //
                        instance("p1-g2-i1"), instance("p1-g2-i2"), //
                        instance("p2-g1-i1"), instance("p2-g1-i2"), instance("p2-g1-i3"), //
                        instance("p2-g2-i1"), instance("p2-g2-i2"), instance("p2-g2-i3"), instance("p2-g2-i4"))));
    }

    public Instance instance(String name) {
        return new Instance().setName(name);
    }

    private Cluster metadata() {
        return new Cluster().setName("my-cluster");
    }

}
