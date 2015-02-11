package com.elastisys.scale.cloudadapters.api.types;

import static com.elastisys.scale.cloudadapers.api.types.Machine.isAllocated;
import static com.elastisys.scale.cloudadapers.api.types.Machine.isEffectiveMember;
import static com.elastisys.scale.cloudadapters.api.types.TestUtils.ips;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.Machine.AllocatedMachinePredicate;
import com.elastisys.scale.cloudadapers.api.types.Machine.EffectiveMemberPredicate;
import com.elastisys.scale.cloudadapers.api.types.Machine.MachineActivePredicate;
import com.elastisys.scale.cloudadapers.api.types.Machine.MachineWithServiceState;
import com.elastisys.scale.cloudadapers.api.types.Machine.MachineWithState;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapers.api.types.ServiceState;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * Verifies the behavior of {@link Predicate}s declared for the {@link Machine}
 * class.
 *
 *
 *
 */
public class TestMachinePredicates {
	static final Logger LOG = LoggerFactory
			.getLogger(TestMachinePredicates.class);

	/**
	 * Verifies the {@link MachineWithState} {@link Predicate}.
	 */
	@Test
	public void testMachineWithStatePredicate() {
		DateTime now = UtcTime.now();
		Machine m1 = new Machine("id", MachineState.REQUESTED, now, null, null);
		Machine m2 = new Machine("id", MachineState.RUNNING, now,
				ips("1.2.3.4"), ips("1.2.3.5"));
		Machine m3 = new Machine("id", MachineState.PENDING, now, null, null);

		assertFalse(Machine.withState(MachineState.RUNNING).apply(m1));
		assertTrue(Machine.withState(MachineState.RUNNING).apply(m2));
		assertFalse(Machine.withState(MachineState.RUNNING).apply(m3));
	}

	/**
	 * Verifies the {@link MachineWithServiceState} {@link Predicate}.
	 */
	@Test
	public void testMachineWithServiceStatePredicate() {
		DateTime now = UtcTime.now();

		// no service state => assigned default of UNKNOWN
		Machine noServiceState = new Machine("id", MachineState.RUNNING, now,
				null, null);
		assertTrue(Machine.withServiceState(ServiceState.UNKNOWN).apply(
				noServiceState));
		assertFalse(Machine.withServiceState(ServiceState.BOOTING).apply(
				noServiceState));
		assertFalse(Machine.withServiceState(ServiceState.IN_SERVICE).apply(
				noServiceState));
		assertFalse(Machine.withServiceState(ServiceState.UNHEALTHY).apply(
				noServiceState));
		assertFalse(Machine.withServiceState(ServiceState.OUT_OF_SERVICE)
				.apply(noServiceState));

		// set service state to IN_SERVICE
		Machine liveMachine = new Machine("id", MachineState.RUNNING,
				ServiceState.IN_SERVICE, now, null, null);
		assertFalse(Machine.withServiceState(ServiceState.UNKNOWN).apply(
				liveMachine));
		assertFalse(Machine.withServiceState(ServiceState.BOOTING).apply(
				liveMachine));
		assertTrue(Machine.withServiceState(ServiceState.IN_SERVICE).apply(
				liveMachine));
		assertFalse(Machine.withServiceState(ServiceState.UNHEALTHY).apply(
				liveMachine));
		assertFalse(Machine.withServiceState(ServiceState.OUT_OF_SERVICE)
				.apply(liveMachine));

	}

	/**
	 * Verifies the {@link AllocatedMachinePredicate} {@link Predicate}.
	 */
	@Test
	public void testAllocatedMachinePredicate() {
		// Only machines that are REQUESTED/PENDING/RUNNING are considered
		// allocated.

		// check all combinations of machineState and serviceState
		MachineState[] machineStates = MachineState.values();
		ServiceState[] serviceStates = ServiceState.values();
		for (MachineState machineState : machineStates) {
			for (ServiceState serviceState : serviceStates) {
				String combo = String.format("tested combination: %s-%s",
						machineState, serviceState);
				LOG.info(combo);
				Machine machine = new Machine("id", machineState, serviceState,
						UtcTime.now(), ips("1.2.3.4"), ips("1.2.3.5"));
				Set<MachineState> allocatedStates = Sets.newHashSet(
						MachineState.REQUESTED, MachineState.PENDING,
						MachineState.RUNNING);
				if (allocatedStates.contains(machine.getMachineState())) {
					assertTrue(combo, isAllocated().apply(machine));
				} else {
					assertFalse(combo, isAllocated().apply(machine));
				}
			}
		}
	}

	/**
	 * Verifies the {@link MachineActivePredicate} {@link Predicate}.
	 */
	@Test
	public void testMachineActivePredicate() {
		// Only machines that are PENDING/RUNNING and not in service state
		// OUT_OF_SERVICE are considered active.

		// check all combinations of machineState and serviceState
		MachineState[] machineStates = MachineState.values();
		ServiceState[] serviceStates = ServiceState.values();
		for (MachineState machineState : machineStates) {
			for (ServiceState serviceState : serviceStates) {
				LOG.info("combination: {}-{}", machineState, serviceState);
				Machine machine = new Machine("id", machineState, serviceState,
						UtcTime.now(), ips("1.2.3.4"), ips("1.2.3.5"));
				Set<MachineState> activeStates = Sets.newHashSet(
						MachineState.PENDING, MachineState.RUNNING);
				if (activeStates.contains(machine.getMachineState())
						&& (machine.getServiceState() != ServiceState.OUT_OF_SERVICE)) {
					assertTrue(Machine.isActive().apply(machine));
				} else {
					assertFalse(Machine.isActive().apply(machine));
				}
			}
		}

		Machine nullLaunchTime = new Machine("id", MachineState.RUNNING, null,
				ips("1.2.3.4"), ips("1.2.3.5"));
		assertFalse(Machine.isActive().apply(nullLaunchTime));
	}

	/**
	 * Verifies the {@link EffectiveMemberPredicate} {@link Predicate}.
	 */
	@Test
	public void testEffectiveMemberPredicate() {
		// Only machines that are REQUESTED/PENDING/RUNNING and not in service
		// state OUT_OF_SERVICE contribute to the effective size of the pool.

		// check all combinations of machineState and serviceState
		MachineState[] machineStates = MachineState.values();
		ServiceState[] serviceStates = ServiceState.values();
		for (MachineState machineState : machineStates) {
			for (ServiceState serviceState : serviceStates) {
				String combo = String.format("tested combination: %s-%s",
						machineState, serviceState);
				LOG.info(combo);
				Machine machine = new Machine("id", machineState, serviceState,
						UtcTime.now(), ips("1.2.3.4"), ips("1.2.3.5"));
				Set<MachineState> allocatedStates = Sets.newHashSet(
						MachineState.REQUESTED, MachineState.PENDING,
						MachineState.RUNNING);
				if (allocatedStates.contains(machine.getMachineState())
						&& (machine.getServiceState() != ServiceState.OUT_OF_SERVICE)) {
					assertTrue(combo, isEffectiveMember().apply(machine));
				} else {
					assertFalse(combo, isEffectiveMember().apply(machine));
				}
			}
		}
	}
}
