package com.elastisys.scale.cloudpool.commons.scaledown;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.cloudpool.commons.scaledown.strategies.NewestMachineVictimSelectionStrategy;
import com.elastisys.scale.cloudpool.commons.scaledown.strategies.OldestMachineVictimSelectionStrategy;

/**
 * Exercises the {@link VictimSelectionPolicy} class.
 */
public class TestVictimSelectionPolicy {

    @Test
    public void testPolicyStrategyAssociations() {
        assertThat(VictimSelectionPolicy.NEWEST.getVictimSelectionStrategy(),
                instanceOf(NewestMachineVictimSelectionStrategy.class));

        assertThat(VictimSelectionPolicy.OLDEST.getVictimSelectionStrategy(),
                instanceOf(OldestMachineVictimSelectionStrategy.class));

    }
}
