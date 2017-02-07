package com.elastisys.scale.cloudpool.google.container.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.cloudpool.google.container.scalingstrategy.ScalingStrategy;
import com.elastisys.scale.cloudpool.google.container.scalingstrategy.impl.BalancedScalingStrategy;

/**
 * Exercises the {@link ScalingPolicy} configuration class.
 */
public class TestScalingPolicy {

    /**
     * Verifies that the {@link ScalingPolicy}s are configured to use their
     * expected {@link ScalingStrategy}s (which implement the policy behavior).
     */
    @Test
    public void verifyPolicyStrategy() {
        assertThat(ScalingPolicy.Balanced.getStrategy(), is(BalancedScalingStrategy.INSTANCE));
    }

}
