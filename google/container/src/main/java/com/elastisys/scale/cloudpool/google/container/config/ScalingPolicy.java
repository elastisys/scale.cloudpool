package com.elastisys.scale.cloudpool.google.container.config;

import com.elastisys.scale.cloudpool.google.container.scalingstrategy.ScalingStrategy;
import com.elastisys.scale.cloudpool.google.container.scalingstrategy.impl.BalancedScalingStrategy;

/**
 * The range of possible policies for scaling the container cluster.
 *
 * @see GoogleContainerEngineCloudPoolConfig
 */
public enum ScalingPolicy {

    /**
     * A scaling policy that strives to keep an equal number of instances in all
     * node pools.
     */
    Balanced(BalancedScalingStrategy.INSTANCE);

    /**
     * The {@link ScalingPolicy}'s {@link ScalingStrategy}, which implements the
     * policy.
     */
    private final ScalingStrategy strategy;

    private ScalingPolicy(ScalingStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Returns the {@link ScalingPolicy}'s {@link ScalingStrategy}, which
     * implements the policy.
     *
     * @return
     */
    public ScalingStrategy getStrategy() {
        return this.strategy;
    }
}
