package com.elastisys.scale.cloudpool.api.types;

import static com.elastisys.scale.cloudpool.api.types.Machine.isActiveMember;
import static com.elastisys.scale.cloudpool.api.types.Machine.isAllocated;
import static com.elastisys.scale.cloudpool.api.types.TestUtils.ips;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.types.Machine.ActiveMemberPredicate;
import com.elastisys.scale.cloudpool.api.types.Machine.AllocatedMachinePredicate;
import com.elastisys.scale.cloudpool.api.types.Machine.MachineWithState;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * Verifies the behavior of {@link Predicate}s declared for the {@link Machine}
 * class.
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
	 * Verifies the {@link AllocatedMachinePredicate} {@link Predicate}. Only
	 * machines that are REQUESTED/PENDING/RUNNING are to be considered
	 * allocated.
	 */
	@Test
	public void testAllocatedMachinePredicate() {
		// check all combinations of machineState, membershipStatus and
		// serviceState
		MachineState[] machineStates = MachineState.values();
		ServiceState[] serviceStates = ServiceState.values();
		MembershipStatus[] membershipStatuses = {
				new MembershipStatus(true, true),
				new MembershipStatus(true, false),
				new MembershipStatus(false, true),
				new MembershipStatus(false, false) };

		boolean allocatedFound = false;
		for (MachineState machineState : machineStates) {
			for (ServiceState serviceState : serviceStates) {
				for (MembershipStatus membershipStatus : membershipStatuses) {
					String combo = String.format(
							"tested combination: %s-%s-%s", machineState,
							membershipStatus, serviceState);
					LOG.info(combo);
					Machine machine = new Machine("id", machineState,
							membershipStatus, serviceState, UtcTime.now(),
							ips("1.2.3.4"), ips("1.2.3.5"), null);
					Set<MachineState> allocatedStates = Sets.newHashSet(
							MachineState.REQUESTED, MachineState.PENDING,
							MachineState.RUNNING);
					if (allocatedStates.contains(machine.getMachineState())) {
						allocatedFound = true;
						assertTrue(combo, isAllocated().apply(machine));
					} else {
						assertFalse(combo, isAllocated().apply(machine));
					}
				}
			}
		}
		// verify that at least one allocated machine was found
		assertTrue(allocatedFound);
	}

	/**
	 * Verifies the {@link ActiveMemberPredicate} {@link Predicate}. Only
	 * machines that are REQUESTED/PENDING/RUNNING and with an active membership
	 * status are to be considered active members of the pool.
	 */
	@Test
	public void testActiveMemberPredicate() {
		// check all combinations of machineState, membershipStatus and
		// serviceState
		MachineState[] machineStates = MachineState.values();
		ServiceState[] serviceStates = ServiceState.values();
		MembershipStatus[] membershipStatuses = {
				new MembershipStatus(true, true),
				new MembershipStatus(true, false),
				new MembershipStatus(false, true),
				new MembershipStatus(false, false) };
		boolean activeFound = false;
		for (MachineState machineState : machineStates) {
			for (ServiceState serviceState : serviceStates) {
				for (MembershipStatus membershipStatus : membershipStatuses) {
					String combo = String.format(
							"tested combination: %s-%s-%s", machineState,
							membershipStatus, serviceState);
					LOG.info(combo);
					Machine machine = new Machine("id", machineState,
							membershipStatus, serviceState, UtcTime.now(),
							ips("1.2.3.4"), ips("1.2.3.5"), null);
					Set<MachineState> allocatedStates = Sets.newHashSet(
							MachineState.REQUESTED, MachineState.PENDING,
							MachineState.RUNNING);
					if (allocatedStates.contains(machine.getMachineState())
							&& (machine.getMembershipStatus().isActive())) {
						activeFound = true;
						assertTrue(combo, isActiveMember().apply(machine));
					} else {
						assertFalse(combo, isActiveMember().apply(machine));
					}
				}
			}
		}
		// verify that at least one active member was found
		assertTrue(activeFound);
	}

	/**
	 * Verifies that {@link Machine#isEvictable()} only is <code>true</code> for
	 * machines with evictable set to <code>false</code> in their
	 * {@link MembershipStatus}.
	 */
	@Test
	public void testEvictablePredicate() {
		// evictable
		Machine m1 = new Machine("id", MachineState.REQUESTED, UtcTime.now(),
				null, null);
		// not evictable
		Machine m2 = new Machine("id", MachineState.RUNNING,
				MembershipStatus.blessed(), ServiceState.UNKNOWN,
				UtcTime.now(), ips("1.2.3.4"), ips("1.2.3.5"), null);
		// evictable
		Machine m3 = new Machine("id", MachineState.RUNNING,
				MembershipStatus.disposable(), ServiceState.UNKNOWN,
				UtcTime.now(), ips("1.2.3.4"), ips("1.2.3.5"), null);
		// not evictable
		Machine m4 = new Machine("id", MachineState.RUNNING,
				MembershipStatus.awaitingService(), ServiceState.UNKNOWN,
				UtcTime.now(), ips("1.2.3.4"), ips("1.2.3.5"), null);

		assertTrue(Machine.isEvictable().apply(m1));
		assertFalse(Machine.isEvictable().apply(m2));
		assertTrue(Machine.isEvictable().apply(m3));
		assertFalse(Machine.isEvictable().apply(m4));
	}
}
