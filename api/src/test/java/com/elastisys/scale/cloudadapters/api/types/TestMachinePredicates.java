package com.elastisys.scale.cloudadapters.api.types;

import static com.elastisys.scale.cloudadapters.api.types.TestUtils.ips;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.types.LivenessState;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.Machine.MachineActivePredicate;
import com.elastisys.scale.cloudadapers.api.types.Machine.MachineAllocatedPredicate;
import com.elastisys.scale.cloudadapers.api.types.Machine.MachineWithLivenessState;
import com.elastisys.scale.cloudadapers.api.types.Machine.MachineWithState;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Predicate;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Verifies the behavior of {@link Predicate}s declared for the {@link Machine}
 * class.
 * 
 * 
 * 
 */
public class TestMachinePredicates {

	/**
	 * Verifies the {@link MachineWithState} {@link Predicate}.
	 */
	@Test
	public void testMachineWithStatePredicate() {
		JsonElement metadata = new JsonObject();
		DateTime now = UtcTime.now();
		Machine m1 = new Machine("id", MachineState.REQUESTED, now, null, null,
				metadata);
		Machine m2 = new Machine("id", MachineState.RUNNING, now,
				ips("1.2.3.4"), ips("1.2.3.5"), metadata);
		Machine m3 = new Machine("id", MachineState.PENDING, now, null, null,
				metadata);

		assertFalse(Machine.withState(MachineState.RUNNING).apply(m1));
		assertTrue(Machine.withState(MachineState.RUNNING).apply(m2));
		assertFalse(Machine.withState(MachineState.RUNNING).apply(m3));
	}

	/**
	 * Verifies the {@link MachineWithLivenessState} {@link Predicate}.
	 */
	@Test
	public void testMachineWithLivenessPredicate() {
		DateTime now = UtcTime.now();
		JsonElement metadata = new JsonObject();

		// no liveness => assigned default of UNKNOWN
		Machine noLivenessState = new Machine("id", MachineState.RUNNING, now,
				null, null, metadata);
		assertFalse(Machine.withLivenessState(LivenessState.BOOTING).apply(
				noLivenessState));
		assertFalse(Machine.withLivenessState(LivenessState.UNHEALTHY).apply(
				noLivenessState));
		assertFalse(Machine.withLivenessState(LivenessState.LIVE).apply(
				noLivenessState));
		assertTrue(Machine.withLivenessState(LivenessState.UNKNOWN).apply(
				noLivenessState));

		// set liveness to LIVE
		Machine liveMachine = new Machine("id", MachineState.RUNNING,
				LivenessState.LIVE, now, null, null, metadata);
		assertFalse(Machine.withLivenessState(LivenessState.BOOTING).apply(
				liveMachine));
		assertFalse(Machine.withLivenessState(LivenessState.UNHEALTHY).apply(
				liveMachine));
		assertFalse(Machine.withLivenessState(LivenessState.UNKNOWN).apply(
				liveMachine));
		assertTrue(Machine.withLivenessState(LivenessState.LIVE).apply(
				liveMachine));

	}

	/**
	 * Verifies the {@link MachineActivePredicate} {@link Predicate}.
	 */
	@Test
	public void testMachineActivePredicate() {
		JsonElement metadata = new JsonObject();
		Machine pending = new Machine("id", MachineState.PENDING,
				UtcTime.now(), null, null, metadata);
		Machine running = new Machine("id", MachineState.RUNNING,
				UtcTime.now(), ips("1.2.3.4"), ips("1.2.3.5"), metadata);
		Machine nullLaunchTime = new Machine("id", MachineState.RUNNING, null,
				ips("1.2.3.4"), ips("1.2.3.5"), metadata);

		assertTrue(Machine.isActive().apply(pending));
		assertTrue(Machine.isActive().apply(running));
		assertFalse(Machine.isActive().apply(nullLaunchTime));

		Machine requested = new Machine("id", MachineState.REQUESTED,
				UtcTime.now(), null, null, metadata);
		Machine rejected = new Machine("id", MachineState.REJECTED,
				UtcTime.now(), null, null, metadata);
		Machine terminated = new Machine("id", MachineState.TERMINATED,
				UtcTime.now(), null, null, metadata);
		Machine terminating = new Machine("id", MachineState.TERMINATING,
				UtcTime.now(), null, null, metadata);
		assertFalse(Machine.isActive().apply(requested));
		assertFalse(Machine.isActive().apply(rejected));
		assertFalse(Machine.isActive().apply(terminated));
		assertFalse(Machine.isActive().apply(terminating));
	}

	/**
	 * Verifies the {@link MachineAllocatedPredicate} {@link Predicate}.
	 */
	@Test
	public void testMachineAllocatedPredicate() {
		JsonElement metadata = new JsonObject();
		Machine requested = new Machine("id", MachineState.REQUESTED, null,
				null, null, metadata);
		Machine pending = new Machine("id", MachineState.PENDING,
				UtcTime.now(), null, null, metadata);
		Machine running = new Machine("id", MachineState.RUNNING,
				UtcTime.now(), ips("1.2.3.4"), ips("1.2.3.5"), metadata);

		Machine rejected = new Machine("id", MachineState.REJECTED,
				UtcTime.now(), null, null, metadata);
		Machine terminated = new Machine("id", MachineState.TERMINATED,
				UtcTime.now(), null, null, metadata);
		Machine terminating = new Machine("id", MachineState.TERMINATING,
				UtcTime.now(), null, null, metadata);

		assertTrue(Machine.isAllocated().apply(requested));
		assertTrue(Machine.isAllocated().apply(pending));
		assertTrue(Machine.isAllocated().apply(running));
		assertFalse(Machine.isActive().apply(rejected));
		assertFalse(Machine.isActive().apply(terminated));
		assertFalse(Machine.isActive().apply(terminating));
	}
}
