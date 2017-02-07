package com.elastisys.scale.cloudpool.google.container.scalingstrategy;

import com.elastisys.scale.cloudpool.google.container.client.ClusterSnapshot;
import com.google.api.services.compute.model.InstanceGroup;
import com.google.api.services.container.model.NodePool;

/**
 * A {@link ScalingStrategy} implements a certain policy to use when the managed
 * container cluster needs to be resized. In particular, a
 * {@link ScalingStrategy} determines which {@link NodePool} (in case of a
 * cluster with multiple {@link NodePool}s) and which instance group within that
 * {@link NodePool} whose target size is to be changed.
 */
public interface ScalingStrategy {

    /**
     * Calculates a resize plan which brings the cluster from its current size
     * (as indicated by the {@code cluster} summary) to a given
     * {@code targetSize} in a way that honors the given policy implemented by
     * this {@link ScalingStrategy}.
     *
     * @param desiredSize
     *            The desired total size of the container cluster.
     * @param clusterSnapshot
     *            A recent snapshot of the container cluster. The
     *            {@link ClusterSnapshot} is guaranteed to have at least one
     *            {@link NodePool} and each {@link NodePool} is guaranteed to
     *            have at least one {@link InstanceGroup}.
     * @return A {@link ResizePlan}, which assigns each instance group in the
     *         container cluster a new target size. The sum of these new target
     *         sizes must always equal the desired {@code targetSize}.
     */
    ResizePlan planResize(int desiredSize, ClusterSnapshot clusterSnapshot);
}
