package com.elastisys.scale.cloudpool.commons.basepool;

import java.util.List;

import org.joda.time.DateTime;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.Lists;

public class BasePoolTestUtils {

	public static Machine machine(String id) {
		return Machine.builder().id(id).machineState(MachineState.RUNNING)
				.launchTime(UtcTime.now()).build();
	}

	public static Machine machine(String id, MachineState machineState) {
		return Machine.builder().id(id).machineState(machineState)
				.launchTime(UtcTime.now()).build();
	}

	public static Machine machine(String id, MachineState machineState,
			DateTime launchTime) {
		return Machine.builder().id(id).machineState(machineState)
				.launchTime(launchTime).build();
	}

	public static Machine machine(String id, MachineState machineState,
			MembershipStatus membershipStatus, DateTime launchTime) {
		return Machine.builder().id(id).machineState(machineState)
				.membershipStatus(membershipStatus).launchTime(launchTime)
				.build();
	}

	public static Machine machine(String id, String publicIp) {
		return Machine.builder().id(id).machineState(MachineState.RUNNING)
				.publicIp(publicIp).build();
	}

	public static List<Machine> machines(Machine... machines) {
		List<Machine> list = Lists.newArrayList();
		for (Machine machine : machines) {
			list.add(machine);
		}
		return list;
	}
}
