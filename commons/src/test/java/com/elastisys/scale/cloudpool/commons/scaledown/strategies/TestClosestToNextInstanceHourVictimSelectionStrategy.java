package com.elastisys.scale.cloudpool.commons.scaledown.strategies;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.commons.scaledown.AbstractScaledownTest;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionStrategy;
import com.elastisys.scale.cloudpool.commons.scaledown.strategies.ClosestToInstanceHourVictimSelectionStrategy;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

public class TestClosestToNextInstanceHourVictimSelectionStrategy extends AbstractScaledownTest {

    /** Object under test. */
    private final VictimSelectionStrategy strategy = ClosestToInstanceHourVictimSelectionStrategy.INSTANCE;

    @Test(expected = NullPointerException.class)
    public void onNullCandidateSet() {
        this.strategy.selectVictim(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onEmptyCandidateSet() {
        List<Machine> empty = Arrays.asList();
        this.strategy.selectVictim(empty);
    }

    @Test
    public void onSingletonCandidateSet() {
        List<Machine> set = Arrays.asList(instance("i-1", "2012-06-01T10:00:00Z"));
        Machine victim = this.strategy.selectVictim(set);
        assertThat(victim, is(getOnlyElement(set)));
    }

    @Test
    public void onMultiCandidateSet() {
        FrozenTime.setFixed(UtcTime.parse("2012-06-01T10:00:00Z"));

        Machine instance1 = instance("i-1", "2012-06-01T09:45:00Z");
        Machine instance2 = instance("i-2", "2012-06-01T09:30:00Z");
        // closes to next instance hour (10:15:00)
        Machine instance3 = instance("i-3", "2012-06-01T09:15:00Z");

        // two candidates
        List<Machine> set = asList(instance1, instance2);
        assertThat(this.strategy.selectVictim(set), is(instance2));

        // three candidates
        set = asList(instance1, instance2, instance3);
        assertThat(this.strategy.selectVictim(set), is(instance3));

        // candidate order should not matter
        set = asList(instance2, instance3, instance1);
        assertThat(this.strategy.selectVictim(set), is(instance3));
        set = asList(instance3, instance1, instance2);
        assertThat(this.strategy.selectVictim(set), is(instance3));
    }

}
