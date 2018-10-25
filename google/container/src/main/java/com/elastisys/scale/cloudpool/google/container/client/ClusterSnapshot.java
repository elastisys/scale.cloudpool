package com.elastisys.scale.cloudpool.google.container.client;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.joda.time.DateTime;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.NodePool;

/**
 * Represents a snapshot of a container {@link Cluster}, the {@link NodePool}s
 * it consists of, and the intance groups that the {@link NodePool}s consist of.
 * <p/>
 * Note: since this snapshot is pieced together from several API calls it may,
 * at times, contain inconsistent data.
 *
 */
public class ClusterSnapshot {

    /** Cluster metadata. */
    private final Cluster metadata;

    /**
     * Snapshots of the {@link NodePool}s that comprise the cluster. May be
     * <code>null</code>. Default: empty list.
     */
    private final List<NodePoolSnapshot> nodePools;

    /**
     * The time at which the snapshot was taken. If <code>null</code>, current
     * time is assumed.
     */
    private final DateTime timestamp;

    /**
     * Creates a {@link ClusterSnapshot} with the current UTC time as timestamp.
     *
     * @param metadata
     *            Cluster metadata.
     * @param nodePools
     *            Snapshots of the {@link NodePool}s that comprise the cluster.
     */
    public ClusterSnapshot(Cluster metadata, List<NodePoolSnapshot> nodePools) {
        this(metadata, nodePools, null);
    }

    /**
     * Creates a {@link ClusterSnapshot}.
     *
     * @param metadata
     *            Cluster metadata.
     * @param nodePools
     *            Snapshots of the {@link NodePool}s that comprise the cluster.
     *            May be <code>null</code>. Default: empty list.
     * @param timestamp
     *            The time at which the snapshot was taken. If
     *            <code>null</code>, current time is assumed.
     */
    public ClusterSnapshot(Cluster metadata, List<NodePoolSnapshot> nodePools, DateTime timestamp) {
        checkArgument(metadata != null, "ClusterSnapshot: metadata cannot be null");
        this.metadata = metadata;
        this.nodePools = Optional.ofNullable(nodePools).orElse(Collections.emptyList());
        this.timestamp = Optional.ofNullable(timestamp).orElse(UtcTime.now());
    }

    /**
     * Cluster metadata.
     *
     * @return
     */
    public Cluster getMetadata() {
        return this.metadata;
    }

    /**
     * Snapshots of the {@link NodePool}s that comprise the cluster.
     *
     * @return
     */
    public List<NodePoolSnapshot> getNodePools() {
        return this.nodePools;
    }

    /**
     * The time at which the snapshot was taken.
     *
     * @return
     */
    public DateTime getTimestamp() {
        return this.timestamp;
    }

    /**
     * Returns the total size as the sum of instance group target sizes.
     *
     * @return
     */
    public int getTotalSize() {
        int totalSize = 0;
        for (NodePoolSnapshot nodePool : this.nodePools) {
            for (InstanceGroupSnapshot instanceGroup : nodePool.getInstanceGroups()) {
                totalSize += instanceGroup.getMetadata().getTargetSize();
            }
        }
        return totalSize;
    }

    /**
     * Returns the current set of started node {@link Instance}s. Note that the
     * size of ths set may differ from {@link #getTotalSize()} since not all
     * instance groups in the cluster may have reached their current target
     * size.
     *
     * @return
     */
    public List<Instance> getStartedNodes() {
        List<Instance> nodes = new ArrayList<>();
        for (NodePoolSnapshot nodePool : this.nodePools) {
            for (InstanceGroupSnapshot instanceGroup : nodePool.getInstanceGroups()) {
                nodes.addAll(instanceGroup.getInstances());
            }
        }
        return nodes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.metadata, this.nodePools, this.timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClusterSnapshot) {
            ClusterSnapshot that = (ClusterSnapshot) obj;
            return Objects.equals(this.metadata, that.metadata) //
                    && Objects.equals(this.nodePools, that.nodePools) //
                    && Objects.equals(this.timestamp, that.timestamp);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

}
