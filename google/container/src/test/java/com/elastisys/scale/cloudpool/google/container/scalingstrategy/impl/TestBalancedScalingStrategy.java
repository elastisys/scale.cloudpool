package com.elastisys.scale.cloudpool.google.container.scalingstrategy.impl;

import static com.elastisys.scale.cloudpool.google.container.testutils.ClusterBuilder.cluster;
import static com.elastisys.scale.cloudpool.google.container.testutils.InstanceGroupBuilder.instanceGroup;
import static com.elastisys.scale.cloudpool.google.container.testutils.NodePoolBuilder.nodePool;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.URL;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.google.container.client.ClusterSnapshot;
import com.elastisys.scale.cloudpool.google.container.client.InstanceGroupSnapshot;
import com.elastisys.scale.cloudpool.google.container.client.NodePoolSnapshot;
import com.elastisys.scale.cloudpool.google.container.scalingstrategy.ResizePlan;
import com.elastisys.scale.cloudpool.google.container.scalingstrategy.ScalingStrategy;
import com.elastisys.scale.cloudpool.google.container.testutils.ClusterBuilder;
import com.elastisys.scale.commons.net.url.UrlUtils;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link BalancedScalingStrategy}.
 */
public class TestBalancedScalingStrategy {

    private final ScalingStrategy strategy = BalancedScalingStrategy.INSTANCE;

    @Before
    public void beforeTestMethod() {
        FrozenTime.setFixed(UtcTime.parse("2017-01-01T12:00:00.000Z"));
    }

    /**
     * When the desired size is equal to the cluster size, the cluster layout
     * should be kept as-is (no rebalancing or anything like that should be
     * attempted).
     */
    @Test
    public void keepSameSize() {
        InstanceGroupSnapshot pool1Group1 = instanceGroup("p1-g1").withSize(0).build();
        InstanceGroupSnapshot pool1Group2 = instanceGroup("p1-g2").withSize(1).build();
        NodePoolSnapshot nodePool1 = nodePool("p1").with(pool1Group1).with(pool1Group2).build();
        InstanceGroupSnapshot pool2Group1 = instanceGroup("p2-g1").withSize(2).build();
        InstanceGroupSnapshot pool2Group2 = instanceGroup("p2-g2").withSize(3).build();
        NodePoolSnapshot nodePool2 = nodePool("p2").with(pool2Group1).with(pool2Group2).build();
        ClusterSnapshot cluster = cluster().with(nodePool1).with(nodePool2).build();

        ResizePlan resizePlan = this.strategy.planResize(6 + 0, cluster);
        assertNewSize(resizePlan, pool1Group1, 0);
        assertNewSize(resizePlan, pool1Group2, 1);
        assertNewSize(resizePlan, pool2Group1, 2);
        assertNewSize(resizePlan, pool2Group2, 3);
    }

    /**
     * With a single node pool with a single instance group, all new instances
     * are trivially to be assigned to the single instance group.
     */
    @Test
    public void growSingleNodePoolWithSingleInstanceGroup() {
        InstanceGroupSnapshot group1 = instanceGroup("p1-g1").withSize(0).build();
        NodePoolSnapshot nodePool1 = nodePool("p1").with(group1).build();
        ClusterSnapshot cluster = cluster().with(nodePool1).build();

        ResizePlan resizePlan = this.strategy.planResize(0 + 1, cluster);
        assertNewSize(resizePlan, group1, 0 + 1);

        resizePlan = this.strategy.planResize(0 + 2, cluster);
        assertNewSize(resizePlan, group1, 0 + 2);

        resizePlan = this.strategy.planResize(0 + 4, cluster);
        assertNewSize(resizePlan, group1, 0 + 4);
    }

    /**
     * When growing a node pool, new instances are always to be added to the
     * smallest instance group.
     */
    @Test
    public void growSingleNodePoolWithMultipleInstanceGroups() {
        InstanceGroupSnapshot group1 = instanceGroup("p1-g1").withSize(0).build();
        InstanceGroupSnapshot group2 = instanceGroup("p1-g2").withSize(0).build();
        NodePoolSnapshot nodePool1 = nodePool("p1").with(group1).with(group2).build();
        ClusterSnapshot cluster = cluster().with(nodePool1).build();

        ResizePlan resizePlan = this.strategy.planResize(0 + 1, cluster);
        assertNewSize(resizePlan, group1, 0 + 1);
        assertNewSize(resizePlan, group2, 0);

        resizePlan = this.strategy.planResize(2, cluster);
        assertNewSize(resizePlan, group1, 0 + 1);
        assertNewSize(resizePlan, group2, 0 + 1);

        resizePlan = this.strategy.planResize(3, cluster);
        assertNewSize(resizePlan, group1, 0 + 2);
        assertNewSize(resizePlan, group2, 0 + 1);
    }

    /**
     * When growing a cluster, new instances are always to be added to the
     * smallest instance group within the smallest node pool.
     */
    @Test
    public void growMultipleNodePoolsWithMultipleInstanceGroups() {
        InstanceGroupSnapshot pool1Group1 = instanceGroup("p1-g1").withSize(0).build();
        InstanceGroupSnapshot pool1Group2 = instanceGroup("p1-g2").withSize(1).build();
        NodePoolSnapshot nodePool1 = nodePool("p1").with(pool1Group1).with(pool1Group2).build();
        InstanceGroupSnapshot pool2Group1 = instanceGroup("p2-g1").withSize(2).build();
        InstanceGroupSnapshot pool2Group2 = instanceGroup("p2-g2").withSize(3).build();
        NodePoolSnapshot nodePool2 = nodePool("p2").with(pool2Group1).with(pool2Group2).build();
        ClusterSnapshot cluster = cluster().with(nodePool1).with(nodePool2).build();

        ResizePlan resizePlan = this.strategy.planResize(6 + 1, cluster);
        assertNewSize(resizePlan, pool1Group1, 0 + 1);
        assertNewSize(resizePlan, pool1Group2, 1);
        assertNewSize(resizePlan, pool2Group1, 2);
        assertNewSize(resizePlan, pool2Group2, 3);

        resizePlan = this.strategy.planResize(6 + 2, cluster);
        assertNewSize(resizePlan, pool1Group1, 0 + 2);
        assertNewSize(resizePlan, pool1Group2, 1);
        assertNewSize(resizePlan, pool2Group1, 2);
        assertNewSize(resizePlan, pool2Group2, 3);

        resizePlan = this.strategy.planResize(6 + 3, cluster);
        assertNewSize(resizePlan, pool1Group1, 0 + 2);
        assertNewSize(resizePlan, pool1Group2, 1 + 1);
        assertNewSize(resizePlan, pool2Group1, 2);
        assertNewSize(resizePlan, pool2Group2, 3);

        resizePlan = this.strategy.planResize(6 + 4, cluster);
        assertNewSize(resizePlan, pool1Group1, 0 + 3);
        assertNewSize(resizePlan, pool1Group2, 1 + 1);
        assertNewSize(resizePlan, pool2Group1, 2);
        assertNewSize(resizePlan, pool2Group2, 3);

        // node pools are now of equal size
        resizePlan = this.strategy.planResize(6 + 5, cluster);
        assertNewSize(resizePlan, pool1Group1, 0 + 3);
        assertNewSize(resizePlan, pool1Group2, 1 + 2);
        assertNewSize(resizePlan, pool2Group1, 2);
        assertNewSize(resizePlan, pool2Group2, 3);

        // node pools 1 is now bigger than node pool 2
        resizePlan = this.strategy.planResize(6 + 6, cluster);
        assertNewSize(resizePlan, pool1Group1, 0 + 3);
        assertNewSize(resizePlan, pool1Group2, 1 + 2);
        assertNewSize(resizePlan, pool2Group1, 2 + 1);
        assertNewSize(resizePlan, pool2Group2, 3);

        // equal size again
        resizePlan = this.strategy.planResize(6 + 7, cluster);
        assertNewSize(resizePlan, pool1Group1, 0 + 4);
        assertNewSize(resizePlan, pool1Group2, 1 + 2);
        assertNewSize(resizePlan, pool2Group1, 2 + 1);
        assertNewSize(resizePlan, pool2Group2, 3);
    }

    /**
     * When growing a cluster, new instances are added to the smallest instance
     * group in an attempt to even out the sizes. HOWEVER, it should never
     * attempt to redistribute existing sizes even if differences exist.
     */
    @Test
    public void growShouldNotRedistribute() {
        InstanceGroupSnapshot group1 = instanceGroup("p1-g1").withSize(1).build();
        InstanceGroupSnapshot group2 = instanceGroup("p1-g2").withSize(4).build();
        InstanceGroupSnapshot group3 = instanceGroup("p1-g3").withSize(10).build();
        NodePoolSnapshot nodePool1 = nodePool("p1").with(group1).with(group2).with(group3).build();
        ClusterSnapshot cluster = cluster().with(nodePool1).build();

        ResizePlan resizePlan = this.strategy.planResize(15 + 1, cluster);
        assertNewSize(resizePlan, group1, 1 + 1);
        // sizes of group2 and group3 should be kept as-is
        assertNewSize(resizePlan, group2, 4);
        assertNewSize(resizePlan, group3, 10);
    }

    /**
     * With a single node pool with a single instance group, all removed
     * instances are trivially removed from the single instance group.
     */
    @Test
    public void shrinkSingleNodePoolWithSingleInstanceGroup() {
        InstanceGroupSnapshot group1 = instanceGroup("p1-g1").withSize(2).build();
        NodePoolSnapshot nodePool1 = nodePool("p1").with(group1).build();
        ClusterSnapshot cluster = cluster().with(nodePool1).build();

        ResizePlan resizePlan = this.strategy.planResize(2 - 1, cluster);
        assertNewSize(resizePlan, group1, 2 - 1);

        resizePlan = this.strategy.planResize(2 - 2, cluster);
        assertNewSize(resizePlan, group1, 0);
    }

    /**
     * When shrinking a node pool, instances are always to be removed from the
     * largest instance group.
     */
    @Test
    public void shrinkSingleNodePoolWithMultipleInstanceGroups() {
        InstanceGroupSnapshot group1 = instanceGroup("p1-g1").withSize(3).build();
        InstanceGroupSnapshot group2 = instanceGroup("p1-g2").withSize(5).build();
        NodePoolSnapshot nodePool1 = nodePool("p1").with(group1).with(group2).build();
        ClusterSnapshot cluster = cluster().with(nodePool1).build();

        ResizePlan resizePlan = this.strategy.planResize(8 - 1, cluster);
        assertNewSize(resizePlan, group1, 3);
        assertNewSize(resizePlan, group2, 5 - 1);

        resizePlan = this.strategy.planResize(8 - 2, cluster);
        assertNewSize(resizePlan, group1, 3);
        assertNewSize(resizePlan, group2, 5 - 2);

        resizePlan = this.strategy.planResize(8 - 3, cluster);
        assertNewSize(resizePlan, group1, 3);
        assertNewSize(resizePlan, group2, 5 - 3);

        resizePlan = this.strategy.planResize(8 - 4, cluster);
        assertNewSize(resizePlan, group1, 3 - 1);
        assertNewSize(resizePlan, group2, 5 - 3);

        resizePlan = this.strategy.planResize(8 - 5, cluster);
        assertNewSize(resizePlan, group1, 3 - 1);
        assertNewSize(resizePlan, group2, 5 - 4);

        resizePlan = this.strategy.planResize(8 - 6, cluster);
        assertNewSize(resizePlan, group1, 3 - 2);
        assertNewSize(resizePlan, group2, 5 - 4);

        resizePlan = this.strategy.planResize(8 - 7, cluster);
        assertNewSize(resizePlan, group1, 3 - 2);
        assertNewSize(resizePlan, group2, 5 - 5);

        resizePlan = this.strategy.planResize(8 - 8, cluster);
        assertNewSize(resizePlan, group1, 3 - 3);
        assertNewSize(resizePlan, group2, 5 - 5);
    }

    /**
     * When shrinking a cluster, instances are always to be removed from the
     * largest instance group within the largest node pool.
     */
    @Test
    public void shrinkMultipleNodePoolsWithMultipleInstanceGroups() {
        InstanceGroupSnapshot pool1Group1 = instanceGroup("p1-g1").withSize(1).build();
        InstanceGroupSnapshot pool1Group2 = instanceGroup("p1-g2").withSize(2).build();
        NodePoolSnapshot nodePool1 = nodePool("p1").with(pool1Group1).with(pool1Group2).build();
        InstanceGroupSnapshot pool2Group1 = instanceGroup("p2-g1").withSize(3).build();
        InstanceGroupSnapshot pool2Group2 = instanceGroup("p2-g2").withSize(4).build();
        NodePoolSnapshot nodePool2 = nodePool("p2").with(pool2Group1).with(pool2Group2).build();
        ClusterSnapshot cluster = cluster().with(nodePool1).with(nodePool2).build();

        ResizePlan resizePlan = this.strategy.planResize(10 - 1, cluster);
        assertNewSize(resizePlan, pool1Group1, 1);
        assertNewSize(resizePlan, pool1Group2, 2);
        assertNewSize(resizePlan, pool2Group1, 3);
        assertNewSize(resizePlan, pool2Group2, 4 - 1);

        resizePlan = this.strategy.planResize(10 - 2, cluster);
        assertNewSize(resizePlan, pool1Group1, 1);
        assertNewSize(resizePlan, pool1Group2, 2);
        assertNewSize(resizePlan, pool2Group1, 3);
        assertNewSize(resizePlan, pool2Group2, 4 - 2);

        resizePlan = this.strategy.planResize(10 - 3, cluster);
        assertNewSize(resizePlan, pool1Group1, 1);
        assertNewSize(resizePlan, pool1Group2, 2);
        assertNewSize(resizePlan, pool2Group1, 3 - 1);
        assertNewSize(resizePlan, pool2Group2, 4 - 2);

        resizePlan = this.strategy.planResize(10 - 4, cluster);
        assertNewSize(resizePlan, pool1Group1, 1);
        assertNewSize(resizePlan, pool1Group2, 2);
        assertNewSize(resizePlan, pool2Group1, 3 - 1);
        assertNewSize(resizePlan, pool2Group2, 4 - 3);

        resizePlan = this.strategy.planResize(10 - 5, cluster);
        assertNewSize(resizePlan, pool1Group1, 1);
        assertNewSize(resizePlan, pool1Group2, 2);
        assertNewSize(resizePlan, pool2Group1, 3 - 2);
        assertNewSize(resizePlan, pool2Group2, 4 - 3);

        resizePlan = this.strategy.planResize(10 - 6, cluster);
        assertNewSize(resizePlan, pool1Group1, 1);
        assertNewSize(resizePlan, pool1Group2, 2 - 1);
        assertNewSize(resizePlan, pool2Group1, 3 - 2);
        assertNewSize(resizePlan, pool2Group2, 4 - 3);

        resizePlan = this.strategy.planResize(10 - 7, cluster);
        assertNewSize(resizePlan, pool1Group1, 1);
        assertNewSize(resizePlan, pool1Group2, 2 - 1);
        assertNewSize(resizePlan, pool2Group1, 3 - 2);
        assertNewSize(resizePlan, pool2Group2, 4 - 4);

        resizePlan = this.strategy.planResize(10 - 8, cluster);
        assertNewSize(resizePlan, pool1Group1, 1);
        assertNewSize(resizePlan, pool1Group2, 2 - 2);
        assertNewSize(resizePlan, pool2Group1, 3 - 2);
        assertNewSize(resizePlan, pool2Group2, 4 - 4);

        resizePlan = this.strategy.planResize(10 - 9, cluster);
        assertNewSize(resizePlan, pool1Group1, 1);
        assertNewSize(resizePlan, pool1Group2, 2 - 2);
        assertNewSize(resizePlan, pool2Group1, 3 - 3);
        assertNewSize(resizePlan, pool2Group2, 4 - 4);

        resizePlan = this.strategy.planResize(10 - 10, cluster);
        assertNewSize(resizePlan, pool1Group1, 1 - 1);
        assertNewSize(resizePlan, pool1Group2, 2 - 2);
        assertNewSize(resizePlan, pool2Group1, 3 - 3);
        assertNewSize(resizePlan, pool2Group2, 4 - 4);
    }

    private static void assertNewSize(ResizePlan plan, InstanceGroupSnapshot instanceGroup, int expectedSize) {
        URL instanceGroupUrl = UrlUtils.url(instanceGroup.getMetadata().getSelfLink());
        assertThat(plan.getInstanceGroupTargetSizes().get(instanceGroupUrl), is(expectedSize));
    }

    /**
     * Should refuse to resize when a negative cluster size is requested.
     */
    @Test(expected = IllegalArgumentException.class)
    public void onNegativeDesiredSize() {
        ClusterSnapshot clusterSnapshot = ClusterBuilder.cluster().build();
        int negativeSize = -1;
        this.strategy.planResize(negativeSize, clusterSnapshot);
    }

}
