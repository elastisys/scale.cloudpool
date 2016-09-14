package com.elastisys.scale.cloudpool.commons.basepool.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;

/**
 * Exercises the {@link ScaleInConfig} class.
 */
public class TestScaleInConfig {

    @Test
    public void basicSanity() {
        ScaleInConfig config = new ScaleInConfig(VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, 500);
        config.validate();
        assertThat(config.getVictimSelectionPolicy(), is(VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR));
        assertThat(config.getInstanceHourMargin(), is(500));

        // try out other victim selection policies
        config = new ScaleInConfig(VictimSelectionPolicy.NEWEST_INSTANCE, 500);
        config.validate();
        assertThat(config.getVictimSelectionPolicy(), is(VictimSelectionPolicy.NEWEST_INSTANCE));

        config = new ScaleInConfig(VictimSelectionPolicy.OLDEST_INSTANCE, 500);
        config.validate();
        assertThat(config.getVictimSelectionPolicy(), is(VictimSelectionPolicy.OLDEST_INSTANCE));
    }

    @Test(expected = CloudPoolException.class)
    public void negativeInstanceHourMargin() {
        ScaleInConfig config = new ScaleInConfig(VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, -1);
        config.validate();
    }

    @Test(expected = CloudPoolException.class)
    public void tooHighInstanceHourMargin() {
        ScaleInConfig config = new ScaleInConfig(VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, 3600);
        config.validate();
    }

}
