package com.elastisys.scale.cloudpool.google.container.client;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceGroupManager;

/**
 * Represents a snapshot of an instance group together with metadata about all
 * member {@link Instance}s.
 * <p/>
 * Note: since this snapshot is pieced together from several API calls it may,
 * at times, contain inconsistent data.
 */
public class InstanceGroupSnapshot {

    /** Metadata about the instance group. */
    private final InstanceGroupManager metadata;

    /**
     * Snapshots of the {@link Instance}s that comprise the instance group.
     */
    private final List<Instance> instances;

    /**
     * Creates an {@link InstanceGroupSnapshot}
     *
     * @param metadata
     *            Metadata about the instance group.
     * @param instances
     *            Snapshots of the {@link Instance}s that comprise the instance
     *            group.
     */
    public InstanceGroupSnapshot(InstanceGroupManager metadata, List<Instance> instances) {
        checkArgument(metadata != null, "InstanceGroupSnapshot: missing metadata");

        this.metadata = metadata;
        this.instances = Optional.ofNullable(instances).orElse(Collections.emptyList());
    }

    /**
     * Metadata about the instance group.
     *
     * @return
     */
    public InstanceGroupManager getMetadata() {
        return this.metadata;
    }

    /**
     * Snapshots of the {@link Instance}s that comprise the instance group.
     *
     * @return
     */
    public List<Instance> getInstances() {
        return this.instances;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.metadata, this.instances);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InstanceGroupSnapshot) {
            InstanceGroupSnapshot that = (InstanceGroupSnapshot) obj;
            return Objects.equals(this.metadata, that.metadata) //
                    && Objects.equals(this.instances, that.instances);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
