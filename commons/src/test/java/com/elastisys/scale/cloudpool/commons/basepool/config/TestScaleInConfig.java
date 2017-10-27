package com.elastisys.scale.cloudpool.commons.basepool.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;

/**
 * Exercises the {@link ScaleInConfig} class.
 */
public class TestScaleInConfig {

    @Test
    public void basicSanity() {
        ScaleInConfig config = new ScaleInConfig(VictimSelectionPolicy.OLDEST);
        config.validate();
        assertThat(config.getVictimSelectionPolicy(), is(VictimSelectionPolicy.OLDEST));

        // try out other victim selection policies
        config = new ScaleInConfig(VictimSelectionPolicy.NEWEST);
        config.validate();
        assertThat(config.getVictimSelectionPolicy(), is(VictimSelectionPolicy.NEWEST));
    }

}
