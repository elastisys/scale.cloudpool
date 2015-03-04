package com.elastisys.scale.cloudpool.commons.scaledown;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.commons.scaledown.TerminationScheduler;
import com.elastisys.scale.cloudpool.commons.termqueue.ScheduledTermination;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link TerminationScheduler}.
 */
public class TestTerminationScheduler extends AbstractScaledownTest {

	@Test
	public void delayedTermination() {
		FrozenTime.setFixed(UtcTime.parse("2013-06-01T12:00:00"));

		// terminate five minutes prior to billing hour end
		int prepaidHourTerminationMargin = 300;
		TerminationScheduler scheduler = new TerminationScheduler(
				prepaidHourTerminationMargin);

		// single victim
		List<Machine> victims = Arrays.asList(instance("i-1",
				"2013-06-01T10:30:00"));
		List<ScheduledTermination> terminations = scheduler
				.scheduleEvictions(victims);
		// termination time should be five minutes prior to XX:30
		assertThat(terminations.size(), is(1));
		assertThat(
				terminations.get(0),
				is(new ScheduledTermination(victims.get(0), UtcTime
						.parse("2013-06-01T12:25:00"))));

		// multiple victims
		victims = Arrays.asList(instance("i-1", "2013-06-01T10:30:00"),
				instance("i-2", "2013-05-31T14:50:00"));
		terminations = scheduler.scheduleEvictions(victims);
		assertThat(terminations.size(), is(2));
		assertThat(
				terminations.get(0),
				is(new ScheduledTermination(victims.get(0), UtcTime
						.parse("2013-06-01T12:25:00"))));
		assertThat(
				terminations.get(1),
				is(new ScheduledTermination(victims.get(1), UtcTime
						.parse("2013-06-01T12:45:00"))));

		// corner case: just started next instance hour
		victims = Arrays.asList(instance("i-1", "2013-06-01T10:00:00"));
		terminations = scheduler.scheduleEvictions(victims);
		assertThat(
				terminations.get(0),
				is(new ScheduledTermination(victims.get(0), UtcTime
						.parse("2013-06-01T12:55:00"))));

		// corner case: closer than margin (5 min) to end of billing hour
		// in that case, instance is scheduled for immediate termination
		victims = Arrays.asList(instance("i-1", "2013-06-01T10:02:00"));
		terminations = scheduler.scheduleEvictions(victims);
		assertThat(
				terminations.get(0),
				is(new ScheduledTermination(victims.get(0), UtcTime
						.parse("2013-06-01T12:00:00"))));
	}

	/**
	 * Non-positive instance hour margin means immediate termination.
	 */
	@Test
	public void immediateTermination() {
		FrozenTime.setFixed(UtcTime.parse("2013-06-01T12:00:00"));

		int prepaidHourTerminationMargin = -1;
		TerminationScheduler scheduler = new TerminationScheduler(
				prepaidHourTerminationMargin);

		// single victim
		List<Machine> victims = Arrays.asList(instance("i-1",
				"2013-06-01T10:30:00"));
		List<ScheduledTermination> terminations = scheduler
				.scheduleEvictions(victims);
		// termination time should be now
		assertThat(terminations.size(), is(1));
		assertThat(
				terminations.get(0),
				is(new ScheduledTermination(victims.get(0), UtcTime
						.parse("2013-06-01T12:00:00"))));

		// multiple victims
		victims = Arrays.asList(instance("i-1", "2013-06-01T10:30:00"),
				instance("i-2", "2013-05-31T14:50:00"));
		terminations = scheduler.scheduleEvictions(victims);
		assertThat(terminations.size(), is(2));
		assertThat(
				terminations.get(0),
				is(new ScheduledTermination(victims.get(0), UtcTime
						.parse("2013-06-01T12:00:00"))));
		assertThat(
				terminations.get(1),
				is(new ScheduledTermination(victims.get(1), UtcTime
						.parse("2013-06-01T12:00:00"))));
	}

	/**
	 * Zero instance hour margin means immediate termination.
	 */
	@Test
	public void zeroMargin() {
		FrozenTime.setFixed(UtcTime.parse("2013-06-01T12:00:00"));

		int prepaidHourTerminationMargin = 0;
		TerminationScheduler scheduler = new TerminationScheduler(
				prepaidHourTerminationMargin);

		List<Machine> victims = Arrays.asList(instance("i-1",
				"2013-06-01T10:30:00"));
		List<ScheduledTermination> terminations = scheduler
				.scheduleEvictions(victims);
		assertThat(
				terminations.get(0),
				is(new ScheduledTermination(victims.get(0), UtcTime
						.parse("2013-06-01T12:00:00"))));
	}

	@Test
	public void emptyCandidateList() {
		FrozenTime.setFixed(UtcTime.parse("2013-06-01T12:00:00"));

		int prepaidHourTerminationMargin = 0;
		TerminationScheduler scheduler = new TerminationScheduler(
				prepaidHourTerminationMargin);

		List<Machine> victims = Arrays.asList();
		List<ScheduledTermination> terminations = scheduler
				.scheduleEvictions(victims);
		assertThat(terminations.size(), is(0));
	}

	@Test(expected = NullPointerException.class)
	public void nullCandidateList() {
		new TerminationScheduler(60).scheduleEvictions(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithIllegalMargin() {
		new TerminationScheduler(3600);
	}

	@Test(expected = NullPointerException.class)
	public void nullCandidate() {
		new TerminationScheduler(60).scheduleEviction(null);
	}

	@Test(expected = NullPointerException.class)
	public void candidateWithNullLaunchTime() {
		DateTime launchTime = null;
		Machine machine = new Machine("i-1", MachineState.REQUESTED,
				launchTime, null, null);
		new TerminationScheduler(60).scheduleEviction(machine);
	}
}
