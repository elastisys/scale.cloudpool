package com.elastisys.scale.cloudpool.kubernetes.podpool;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Describes both actual and desired size of a {@link PodPool}.
 *
 * @see PodPool#getSize()
 */
public class PodPoolSize {

    /** The number of desired replicas in the {@link PodPool}. */
    private final int desiredReplicas;
    /** The most recently observed number of replicas in the {@link PodPool}. */
    private final int actualReplicas;

    /**
     * Creates a {@link PodPoolSize}.
     *
     * @param desiredReplicas
     *            The number of desired replicas in the {@link PodPool}.
     * @param actualReplicas
     *            The most recently observed number of replicas in the
     *            {@link PodPool}.
     */
    public PodPoolSize(int desiredReplicas, int actualReplicas) {
        this.desiredReplicas = desiredReplicas;
        this.actualReplicas = actualReplicas;
    }

    /**
     * The number of desired replicas in the {@link PodPool}.
     *
     * @return
     */
    public int getDesiredReplicas() {
        return this.desiredReplicas;
    }

    /**
     * The most recently observed number of replicas in the {@link PodPool}.
     *
     * @return
     */
    public int getActualReplicas() {
        return this.actualReplicas;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.desiredReplicas, this.actualReplicas);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PodPoolSize) {
            PodPoolSize that = (PodPoolSize) obj;
            return Objects.equals(this.desiredReplicas, that.desiredReplicas) //
                    && Objects.equals(this.actualReplicas, that.actualReplicas);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
