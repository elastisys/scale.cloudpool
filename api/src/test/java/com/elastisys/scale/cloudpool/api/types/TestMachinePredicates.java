package com.elastisys.scale.cloudpool.api.types;

import static com.elastisys.scale.cloudpool.api.types.Machine.isActiveMember;
import static com.elastisys.scale.cloudpool.api.types.Machine.isAllocated;
import static com.elastisys.scale.cloudpool.api.types.Machine.isStarted;
import static com.elastisys.scale.cloudpool.api.types.TestUtils.ips;
import static com.elastisys.scale.cloudpool.api.types.TestUtils.machine;
import static com.elastisys.scale.cloudpool.api.types.TestUtils.machineNoIp;
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
	public void testMachineInStatePredicate() {
		DateTime now = UtcTime.now();
		Machine m1 = machineNoIp("id", MachineState.REQUESTED, now);
		Machine m2 = machine("id", MachineState.RUNNING, now, ips("1.2.3.4"),
				ips("1.2.3.5"));
		Machine m3 = machineNoIp("id", MachineState.PENDING, now);

		assertFalse(Machine.inState(MachineState.RUNNING).apply(m1));
		assertTrue(Machine.inState(MachineState.RUNNING).apply(m2));
		assertFalse(Machine.inState(MachineState.RUNNING).apply(m3));
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
					String combo = String.format("tested combination: %s-%s-%s",
							machineState, membershipStatus, serviceState);
					LOG.info(combo);
					final DateTime timestamp = UtcTime.now();
					Machine machine = Machine.builder().id("id")
							.machineState(machineState).cloudProvider("AWS-EC2")
							.machineSize("m1.small")
							.membershipStatus(membershipStatus)
							.serviceState(serviceState).requestTime(timestamp)
							.launchTime(timestamp).publicIps(ips("1.2.3.4"))
							.privateIps(ips("1.2.3.5")).build();
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
	 * Verifies the {@link AllocatedMachinePredicate} {@link Predicate}. Only
	 * machines that are REQUESTED/PENDING/RUNNING are to be considered
	 * allocated.
	 */
	@Test
	public void testStartedMachinePredicate() {
		// check all combinations of machineState, membershipStatus and
		// serviceState
		MachineState[] machineStates = MachineState.values();
		ServiceState[] serviceStates = ServiceState.values();
		MembershipStatus[] membershipStatuses = {
				new MembershipStatus(true, true),
				new MembershipStatus(true, false),
				new MembershipStatus(false, true),
				new MembershipStatus(false, false) };

		boolean startedFound = false;
		for (MachineState machineState : machineStates) {
			for (ServiceState serviceState : serviceStates) {
				for (MembershipStatus membershipStatus : membershipStatuses) {
					String combo = String.format("tested combination: %s-%s-%s",
							machineState, membershipStatus, serviceState);
					LOG.info(combo);
					final DateTime timestamp = UtcTime.now();
					Machine machine = Machine.builder().id("id")
							.machineState(machineState).cloudProvider("AWS-EC2")
							.machineSize("m1.small")
							.membershipStatus(membershipStatus)
							.serviceState(serviceState).requestTime(timestamp)
							.launchTime(timestamp).publicIps(ips("1.2.3.4"))
							.privateIps(ips("1.2.3.5")).build();
					Set<MachineState> startedStates = Sets.newHashSet(
							MachineState.PENDING, MachineState.RUNNING);
					if (startedStates.contains(machine.getMachineState())) {
						startedFound = true;
						assertTrue(combo, isStarted().apply(machine));
					} else {
						assertFalse(combo, isStarted().apply(machine));
					}
				}
			}
		}
		// verify that at least one allocated machine was found
		assertTrue(startedFound);
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
					String combo = String.format("tested combination: %s-%s-%s",
							machineState, membershipStatus, serviceState);
					LOG.info(combo);
					final DateTime timestamp = UtcTime.now();
					Machine machine = Machine.builder().id("id")
							.machineState(machineState).cloudProvider("AWS-EC2")
							.machineSize("m1.small")
							.membershipStatus(membershipStatus)
							.serviceState(serviceState).requestTime(timestamp)
							.launchTime(timestamp).publicIps(ips("1.2.3.4"))
							.privateIps(ips("1.2.3.5")).build();
					Set<MachineState> allocatedStates = Sets.newHashSet(
							MachineState.REQUESTED, MachineState.PENDING,
							MachineState.RUNNING);
					if (allocatedStates.contains(machine.getMachineState())
							&& machine.getMembershipStatus().isActive()) {
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
		Machine m1 = machineNoIp("id", MachineState.REQUESTED, UtcTime.now());
		// not evictable
		Machine m2 = Machine.builder().id("id")
				.machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
				.machineSize("m1.small")
				.membershipStatus(MembershipStatus.blessed())
				.requestTime(UtcTime.now()).launchTime(UtcTime.now())
				.publicIps(ips("1.2.3.4")).privateIps(ips("1.2.3.5")).build();
		// evictable
		Machine m3 = Machine.builder().id("id")
				.machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
				.machineSize("m1.small")
				.membershipStatus(MembershipStatus.disposable())
				.requestTime(UtcTime.now()).launchTime(UtcTime.now())
				.publicIps(ips("1.2.3.4")).privateIps(ips("1.2.3.5")).build();
		// not evictable
		Machine m4 = Machine.builder().id("id")
				.machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
				.machineSize("m1.small")
				.membershipStatus(MembershipStatus.awaitingService())
				.requestTime(UtcTime.now()).launchTime(UtcTime.now())
				.publicIps(ips("1.2.3.4")).privateIps(ips("1.2.3.5")).build();

		assertTrue(Machine.isEvictable().apply(m1));
		assertFalse(Machine.isEvictable().apply(m2));
		assertTrue(Machine.isEvictable().apply(m3));
		assertFalse(Machine.isEvictable().apply(m4));
	}
}
