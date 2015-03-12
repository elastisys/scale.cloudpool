package com.elastisys.scale.cloudpool.api.types;

import static com.elastisys.scale.cloudpool.api.types.TestUtils.ips;
import static com.elastisys.scale.cloudpool.api.types.TestUtils.machine;
import static com.elastisys.scale.cloudpool.api.types.TestUtils.secondsBetween;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.types.Machine.InstanceHourStart;
import com.elastisys.scale.cloudpool.api.types.Machine.MachineIdExtractor;
import com.elastisys.scale.cloudpool.api.types.Machine.MachineStateExtractor;
import com.elastisys.scale.cloudpool.api.types.Machine.RemainingInstanceHourTime;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Function;
import com.google.gson.JsonObject;

/**
 * Verifies the behavior of {@link Function}s declared for the {@link Machine}
 * class.
 *
 *
 *
 */
public class TestMachineFunctions {

	/**
	 * Verifies the {@link MachineStateExtractor} {@link Function}.
	 */
	@Test
	public void testMachineStateExtractor() {
		DateTime now = UtcTime.now();
		Machine m1 = new Machine("id", MachineState.REQUESTED, now, null, null);
		Machine m2 = new Machine("id", MachineState.RUNNING, now,
				ips("1.2.3.4"), ips("1.2.3.5"));
		Machine m3 = new Machine("id", MachineState.PENDING, now, null, null);

		assertThat(Machine.toState().apply(m1), is(MachineState.REQUESTED));
		assertThat(Machine.toState().apply(m2), is(MachineState.RUNNING));
		assertThat(Machine.toState().apply(m3), is(MachineState.PENDING));
	}

	/**
	 * Verifies the {@link MachineIdExtractor} {@link Function}.
	 */
	@Test
	public void testMachineIdExtractor() {
		DateTime now = UtcTime.now();
		Machine m1 = new Machine("i-1", MachineState.REQUESTED, now, null, null);
		Machine m2 = new Machine("i-2", MachineState.RUNNING, now,
				ips("1.2.3.4"), ips("1.2.3.5"));
		Machine m3 = new Machine("i-3", MachineState.PENDING, now, null, null);

		assertThat(Machine.toId().apply(m1), is("i-1"));
		assertThat(Machine.toId().apply(m2), is("i-2"));
		assertThat(Machine.toId().apply(m3), is("i-3"));
	}

	/**
	 * Verifies the correctness of {@link InstanceHourStart} {@link Function}
	 * calculations.
	 */
	@Test
	public void testInstanceHourStart() {
		// The start of the current instance hour should have the same
		// wallclock hour offset as the original launch time had. If the
		// instance was launched at 10:05:30 the start of current instance
		// hour should be XX:05:30 where XX is current hour.
		Machine machine = machine("i-1", UtcTime.parse("2013-12-09T08:50:00Z"));

		FrozenTime.setFixed(UtcTime.parse("2013-12-09T09:15:00Z"));
		assertThat(Machine.instanceHourStart().apply(machine),
				is(UtcTime.parse("2013-12-09T08:50:00Z")));

		FrozenTime.setFixed(UtcTime.parse("2013-12-09T08:55:00Z"));
		assertThat(Machine.instanceHourStart().apply(machine),
				is(UtcTime.parse("2013-12-09T08:50:00Z")));

		FrozenTime.setFixed(UtcTime.parse("2013-12-09T09:15:00Z"));
		assertThat(Machine.instanceHourStart().apply(machine),
				is(UtcTime.parse("2013-12-09T08:50:00Z")));

		FrozenTime.setFixed(UtcTime.parse("2013-12-09T09:49:00Z"));
		assertThat(Machine.instanceHourStart().apply(machine),
				is(UtcTime.parse("2013-12-09T08:50:00Z")));

		FrozenTime.setFixed(UtcTime.parse("2013-12-09T09:49:59Z"));
		assertThat(Machine.instanceHourStart().apply(machine),
				is(UtcTime.parse("2013-12-09T08:50:00Z")));

		FrozenTime.setFixed(UtcTime.parse("2013-12-09T09:50:00Z"));
		assertThat(Machine.instanceHourStart().apply(machine),
				is(UtcTime.parse("2013-12-09T09:50:00Z")));

		FrozenTime.setFixed(UtcTime.parse("2013-12-09T09:55:00Z"));
		assertThat(Machine.instanceHourStart().apply(machine),
				is(UtcTime.parse("2013-12-09T09:50:00Z")));

		FrozenTime.setFixed(UtcTime.parse("2013-12-09T10:51:00Z"));
		assertThat(Machine.instanceHourStart().apply(machine),
				is(UtcTime.parse("2013-12-09T10:50:00Z")));

		// with milliseconds precision
		FrozenTime.setFixed(UtcTime.parse("2013-12-09T10:51:00.000Z"));
		machine = machine("i-1", UtcTime.parse("2013-12-09T08:50:00.500Z"));
		assertThat(Machine.instanceHourStart().apply(machine),
				is(UtcTime.parse("2013-12-09T10:50:00.500Z")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInstanceHourStartWithNullMachine() {
		Machine.instanceHourStart().apply(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInstanceHourStartWithNullLaunchTime() {
		Machine.instanceHourStart().apply(machine("i-1", null));
	}

	/**
	 * Verifies the {@link RemainingInstanceHourTime} {@link Function}.
	 */
	@Test
	public void testRemainingInstanceHourTime() {
		Machine machine = machine("i-1", UtcTime.parse("2013-12-09T08:50:00Z"));

		FrozenTime.setFixed(UtcTime.parse("2013-12-09T09:15:00Z"));
		assertThat(
				Machine.remainingInstanceHourTime().apply(machine),
				is(secondsBetween(UtcTime.now(), machine.getLaunchtime()
						.plusHours(1))));

		FrozenTime.setFixed(UtcTime.parse("2013-12-09T08:55:00Z"));
		assertThat(
				Machine.remainingInstanceHourTime().apply(machine),
				is(secondsBetween(UtcTime.now(), machine.getLaunchtime()
						.plusHours(1))));

		FrozenTime.setFixed(UtcTime.parse("2013-12-09T09:15:00Z"));
		assertThat(
				Machine.remainingInstanceHourTime().apply(machine),
				is(secondsBetween(UtcTime.now(), machine.getLaunchtime()
						.plusHours(1))));

		FrozenTime.setFixed(UtcTime.parse("2013-12-09T09:49:00Z"));
		assertThat(
				Machine.remainingInstanceHourTime().apply(machine),
				is(secondsBetween(UtcTime.now(), machine.getLaunchtime()
						.plusHours(1))));

		FrozenTime.setFixed(UtcTime.parse("2013-12-09T09:49:59Z"));
		assertThat(
				Machine.remainingInstanceHourTime().apply(machine),
				is(secondsBetween(UtcTime.now(), machine.getLaunchtime()
						.plusHours(1))));

		FrozenTime.setFixed(UtcTime.parse("2013-12-09T09:50:00Z"));
		assertThat(
				Machine.remainingInstanceHourTime().apply(machine),
				is(secondsBetween(UtcTime.now(), machine.getLaunchtime()
						.plusHours(2))));
		FrozenTime.setFixed(UtcTime.parse("2013-12-09T09:55:00Z"));
		assertThat(
				Machine.remainingInstanceHourTime().apply(machine),
				is(secondsBetween(UtcTime.now(), machine.getLaunchtime()
						.plusHours(2))));

		FrozenTime.setFixed(UtcTime.parse("2013-12-09T10:51:00Z"));
		assertThat(
				Machine.remainingInstanceHourTime().apply(machine),
				is(secondsBetween(UtcTime.now(), machine.getLaunchtime()
						.plusHours(3))));

		// with milliseconds precision
		FrozenTime.setFixed(UtcTime.parse("2013-12-09T10:51:00.000Z"));
		machine = machine("i-1", UtcTime.parse("2013-12-09T08:50:00.500Z"));
		assertThat(
				Machine.remainingInstanceHourTime().apply(machine),
				is(secondsBetween(UtcTime.now(), machine.getLaunchtime()
						.plusHours(3))));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemainingInstanceHourTimeWithNullMachine() {
		Machine.remainingInstanceHourTime().apply(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemainingInstanceHourTimeWithNullLaunchTime() {
		Machine.remainingInstanceHourTime().apply(machine("i-1", null));
	}

	@Test
	public void testToShortMachineFormat() {
		DateTime now = UtcTime.now();
		JsonObject metadata = JsonUtils.parseJsonString("{\"id\": \"i-1\"}");
		Machine m1 = new Machine("i-2", MachineState.RUNNING,
				MembershipStatus.defaultStatus(), ServiceState.UNKNOWN, now,
				ips("1.2.3.4"), ips("1.2.3.5"), metadata);

		assertFalse(Machine.toShortFormat().apply(m1).contains("metadata"));
	}
}
