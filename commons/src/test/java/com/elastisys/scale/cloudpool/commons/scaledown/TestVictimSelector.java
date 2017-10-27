package com.elastisys.scale.cloudpool.commons.scaledown;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.commons.scaledown.strategies.OldestMachineVictimSelectionStrategy;

/**
 * Exercises the {@link VictimSelector}.
 * <p/>
 * This test does not verify the behavior of each
 * {@link VictimSelectionStrategy}. Each strategy is tested in a separate unit
 * test.
 */
public class TestVictimSelector extends AbstractScaledownTest {

    /** Object under test. */
    private VictimSelector victimSelector;

    @Before
    public void onSetup() {
        this.victimSelector = new VictimSelector(OldestMachineVictimSelectionStrategy.INSTANCE);
    }

    @Test(expected = NullPointerException.class)
    public void createWithNullStrategy() {
        new VictimSelector(null);
    }

    @Test(expected = NullPointerException.class)
    public void onNullCandidateSet() {
        this.victimSelector.selectVictims(null, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onNegativeNumberOfVictims() {
        List<Machine> set = Arrays.asList(instance("i-1", "2012-06-01T12:00:00"));
        this.victimSelector.selectVictims(set, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onMoreVictimsThanCandidates() {
        List<Machine> set = Arrays.asList(instance("i-1", "2012-06-01T12:00:00"));
        this.victimSelector.selectVictims(set, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onEmptyCandidateSet() {
        List<Machine> set = Arrays.asList();
        this.victimSelector.selectVictims(set, 1);
    }

    @Test
    public void onSingletonCandidateSet() {
        Machine instance1 = instance("i-1", "2012-06-01T12:00:00");
        List<Machine> set = asList(instance1);
        List<Machine> victims = this.victimSelector.selectVictims(set, 1);
        assertThat(victims.size(), is(1));
        assertThat(victims.get(0), is(instance1));
    }

}
