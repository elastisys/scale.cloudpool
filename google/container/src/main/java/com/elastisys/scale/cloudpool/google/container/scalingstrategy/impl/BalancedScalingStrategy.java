package com.elastisys.scale.cloudpool.google.container.scalingstrategy.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.google.container.client.ClusterSnapshot;
import com.elastisys.scale.cloudpool.google.container.client.InstanceGroupSnapshot;
import com.elastisys.scale.cloudpool.google.container.client.NodePoolSnapshot;
import com.elastisys.scale.cloudpool.google.container.scalingstrategy.ResizePlan;
import com.elastisys.scale.cloudpool.google.container.scalingstrategy.ScalingStrategy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.url.UrlUtils;
import com.google.api.services.compute.model.InstanceGroup;
import com.google.api.services.container.model.NodePool;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;

/**
 * A {@link ScalingStrategy} that strives to always keep an equal number of
 * instances in each node pool (and in each of their instance groups).
 */
public enum BalancedScalingStrategy implements ScalingStrategy {
    INSTANCE;

    private static final Logger LOG = LoggerFactory.getLogger(BalancedScalingStrategy.class);

    @Override
    public ResizePlan planResize(int desiredSize, ClusterSnapshot clusterSnapshot) {
        checkArgument(desiredSize >= 0, "desiredSize must be non-negative");

        ClusterLayout cluster = buildClusterLayout(clusterSnapshot);
        LOG.debug("pre-planning cluster layout: {}", new ResizePlan(cluster.instanceGroupSizes()));

        int delta = desiredSize - cluster.totalSize();
        if (delta > 0) {
            // a new node is always added to the smallest instance group found
            // in the smallest node pool.
            for (int i = 0; i < delta; i++) {
                cluster.smallestNodePool().smallestInstanceGroup().increment();
            }
        } else {
            // a node is always deleted from the largest instance group found in
            // the largest node pool.
            for (int i = 0; i < Math.abs(delta); i++) {
                cluster.largestNodePool().largestInstanceGroup().decrement();
            }
        }

        ResizePlan plan = new ResizePlan(cluster.instanceGroupSizes());
        LOG.debug("planned resized cluster layout: {}", new ResizePlan(cluster.instanceGroupSizes()));
        return plan;
    }

    /**
     * Builds a {@link ClusterLayout} that the resize planning algorithm will
     * make use of.
     *
     * @param clusterSnapshot
     * @return
     */
    private ClusterLayout buildClusterLayout(ClusterSnapshot clusterSnapshot) {
        List<NodePoolLayout> nodePools = new ArrayList<>();
        for (NodePoolSnapshot nodePool : clusterSnapshot.getNodePools()) {
            List<InstanceGroupLayout> instanceGroups = new ArrayList<>();
            for (InstanceGroupSnapshot instanceGroup : nodePool.getInstanceGroups()) {
                instanceGroups.add(new InstanceGroupLayout(UrlUtils.url(instanceGroup.getMetadata().getSelfLink()),
                        instanceGroup.getMetadata().getTargetSize()));
            }
            nodePools.add(new NodePoolLayout(UrlUtils.url(nodePool.getMetadata().getSelfLink()), instanceGroups));
        }
        return new ClusterLayout(nodePools);
    }

    /**
     * Sorts the given list and returns a reference to itself (for chaining of
     * calls).
     *
     * @param list
     * @return
     */
    private static <T extends Comparable<T>> List<T> sorted(List<T> list) {
        Collections.sort(list);
        return list;
    }

    /**
     * Represents how a container cluster is organized in terms of the sizes of
     * its constituent {@link NodePool}s (and their {@link InstanceGroup}s).
     */
    private static class ClusterLayout {
        /** List of node pools. */
        private final List<NodePoolLayout> nodePools;

        public ClusterLayout(List<NodePoolLayout> nodePools) {
            this.nodePools = new ArrayList<>(nodePools);
        }

        public List<InstanceGroupLayout> getInstanceGroups() {
            List<InstanceGroupLayout> instanceGroups = new ArrayList<>();
            for (NodePoolLayout nodePool : this.nodePools) {
                instanceGroups.addAll(nodePool.instanceGroups);
            }
            return instanceGroups;
        }

        public int totalSize() {
            int totalSize = 0;
            for (NodePoolLayout nodePool : this.nodePools) {
                totalSize += nodePool.totalSize();
            }
            return totalSize;
        }

        public NodePoolLayout smallestNodePool() {
            return sorted(this.nodePools).get(0);
        }

        public NodePoolLayout largestNodePool() {
            return Iterables.getLast(sorted(this.nodePools));
        }

        public Map<URL, Integer> instanceGroupSizes() {
            Map<URL, Integer> instanceGroupSizes = new HashMap<>();
            for (InstanceGroupLayout instanceGroup : getInstanceGroups()) {
                instanceGroupSizes.put(instanceGroup.url, instanceGroup.size);
            }
            return instanceGroupSizes;
        }

        @Override
        public String toString() {
            return JsonUtils.toPrettyString(JsonUtils.toJson(this));
        }
    }

    /**
     * Represents a container cluster {@link NodePool} and the
     * {@link InstanceGroup}s it consists of.
     */
    private static class NodePoolLayout implements Comparable<NodePoolLayout> {
        private final URL url;
        /** List of instance groups. */
        private final List<InstanceGroupLayout> instanceGroups;

        public NodePoolLayout(URL url, List<InstanceGroupLayout> instanceGroups) {
            this.url = url;
            this.instanceGroups = new ArrayList<>(instanceGroups);
        }

        public int totalSize() {
            int totalSize = 0;
            for (InstanceGroupLayout instanceGroup : this.instanceGroups) {
                totalSize += instanceGroup.size;
            }
            return totalSize;
        }

        @Override
        public int compareTo(NodePoolLayout other) {
            return ComparisonChain.start().compare(totalSize(), other.totalSize())
                    .compare(this.url.toString(), other.url.toString()).result();
        }

        public InstanceGroupLayout smallestInstanceGroup() {
            return sorted(this.instanceGroups).get(0);
        }

        public InstanceGroupLayout largestInstanceGroup() {
            return Iterables.getLast(sorted(this.instanceGroups));
        }

        @Override
        public String toString() {
            return JsonUtils.toPrettyString(JsonUtils.toJson(this));
        }
    }

    /**
     * Represents a particular {@link InstanceGroup} and its target size.
     */
    private static class InstanceGroupLayout implements Comparable<InstanceGroupLayout> {
        private final URL url;

        private int size;

        public InstanceGroupLayout(URL url, int size) {
            this.url = url;
            this.size = size;
        }

        public void increment() {
            this.size++;
        }

        public void decrement() {
            this.size--;
        }

        @Override
        public int compareTo(InstanceGroupLayout other) {
            return ComparisonChain.start().compare(this.size, other.size)
                    .compare(this.url.toString(), other.url.toString()).result();
        }

        @Override
        public String toString() {
            return JsonUtils.toPrettyString(JsonUtils.toJson(this));
        }
    }

}
