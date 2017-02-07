package com.elastisys.scale.cloudpool.google.container.client;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.api.services.compute.model.InstanceGroup;
import com.google.api.services.container.model.NodePool;

/**
 * <p/>
 * Note: since this snapshot is pieced together from several API calls it may,
 * at times, contain inconsistent data.
 */
public class NodePoolSnapshot {

    /** Metadata about the {@link NodePool}. */
    private final NodePool metadata;

    /**
     * Snapshots of the {@link InstanceGroup}s that comprise the
     * {@link NodePool}.
     */
    private final List<InstanceGroupSnapshot> instanceGroups;

    /**
     * Creates a {@link NodePoolSnapshot}.
     *
     * @param metadata
     *            Metadata about the {@link NodePool}.
     * @param instanceGroups
     *            Snapshots of the {@link InstanceGroup}s that comprise the
     *            {@link NodePool}.
     */
    public NodePoolSnapshot(NodePool metadata, List<InstanceGroupSnapshot> instanceGroups) {
        checkArgument(metadata != null, "NodePoolSnapshot: missing metadata");
        this.metadata = metadata;
        this.instanceGroups = Optional.ofNullable(instanceGroups).orElse(Collections.emptyList());
    }

    /**
     * Metadata about the {@link NodePool}.
     *
     * @return
     */
    public NodePool getMetadata() {
        return this.metadata;
    }

    /**
     * Snapshots of the {@link InstanceGroup}s that comprise the
     * {@link NodePool}
     *
     * @return
     */
    public List<InstanceGroupSnapshot> getInstanceGroups() {
        return this.instanceGroups;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.metadata, this.instanceGroups);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NodePoolSnapshot) {
            NodePoolSnapshot that = (NodePoolSnapshot) obj;
            return Objects.equals(this.metadata, that.metadata) //
                    && Objects.equals(this.instanceGroups, that.instanceGroups);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }

}
