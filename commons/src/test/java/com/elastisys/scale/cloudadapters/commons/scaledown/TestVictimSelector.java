package com.elastisys.scale.cloudadapters.commons.scaledown;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.commons.scaledown.strategies.OldestInstanceVictimSelectionStrategy;
import com.elastisys.scale.cloudadapters.commons.termqueue.ScheduledTermination;
import com.elastisys.scale.cloudadapters.commons.termqueue.TerminationQueue;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link VictimSelector}.
 * <p/>
 * This test does not verify the behavior of each
 * {@link VictimSelectionStrategy}. Each strategy is tested in a separate unit
 * test.
 * 
 * 
 * 
 */
public class TestVictimSelector extends AbstractScaledownTest {

	/** Object under test. */
	private VictimSelector victimSelector;
	/** Termination queue used by the object under test. */
	private TerminationQueue terminationQueue;

	@Before
	public void onSetup() {
		this.terminationQueue = new TerminationQueue();
		this.victimSelector = new VictimSelector(this.terminationQueue,
				OldestInstanceVictimSelectionStrategy.INSTANCE);
	}

	@Test(expected = NullPointerException.class)
	public void createWithNullTimeSource() {
		new VictimSelector(null, OldestInstanceVictimSelectionStrategy.INSTANCE);
	}

	@Test(expected = NullPointerException.class)
	public void createWithNullStrategy() {
		new VictimSelector(new TerminationQueue(), null);
	}

	@Test(expected = NullPointerException.class)
	public void onNullCandidateSet() {
		this.victimSelector.selectVictims(null, 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void onNegativeNumberOfVictims() {
		List<Machine> set = Arrays
				.asList(instance("i-1", "2012-06-01T12:00:00"));
		this.victimSelector.selectVictims(set, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void onMoreVictimsThanCandidates() {
		List<Machine> set = Arrays
				.asList(instance("i-1", "2012-06-01T12:00:00"));
		this.victimSelector.selectVictims(set, 2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void onMoreVictimsThanEligbleInstances() {
		FrozenTime.setFixed(UtcTime.parse("2013-06-01T12:00:00"));

		Machine instance1 = instance("i-1", "2012-06-01T11:00:00");
		Machine instance2 = instance("i-2", "2012-06-01T11:30:00");

		// instance1 in termination queue
		this.terminationQueue.add(new ScheduledTermination(instance1, UtcTime
				.now().plus(1)));

		// request two victims while only one is eligble for termination, the
		// other one is already marked for termination
		List<Machine> set = asList(instance1, instance2);
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

	/**
	 * Verifies that the termination queue is honored. That is, already
	 * termination-marked instances are never to be selected.
	 */
	@Test
	public void onCandidateSetWhereSomeInstancesAreTerminationMarked() {
		FrozenTime.setFixed(UtcTime.parse("2013-06-01T12:00:00"));

		Machine instance1 = instance("i-1", "2012-06-01T11:00:00");
		Machine instance2 = instance("i-2", "2012-06-01T11:30:00");
		Machine instance3 = instance("i-3", "2012-06-01T11:45:00");

		// instance1 in termination queue
		this.terminationQueue.add(new ScheduledTermination(instance1, UtcTime
				.now().plus(1)));

		List<Machine> set = asList(instance1, instance2, instance3);
		List<Machine> victims = this.victimSelector.selectVictims(set, 1);
		assertThat(victims.size(), is(1));
		assertThat(victims, is(asList(instance2)));

		victims = this.victimSelector.selectVictims(set, 2);
		assertThat(victims.size(), is(2));
		assertThat(victims, is(asList(instance2, instance3)));
	}

}
