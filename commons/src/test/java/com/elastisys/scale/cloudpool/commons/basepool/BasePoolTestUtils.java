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
				.machineSize("m1.small").cloudProvider("AWS-EC2")
				.region("us-east-1").launchTime(UtcTime.now()).build();
	}

	public static Machine machine(String id, MachineState machineState) {
		return Machine.builder().id(id).machineState(machineState)
				.machineSize("m1.small").cloudProvider("AWS-EC2")
				.region("us-east-1").launchTime(UtcTime.now()).build();
	}

	public static Machine machine(String id, MachineState machineState,
			DateTime launchTime) {
		return Machine.builder().id(id).machineState(machineState)
				.cloudProvider("AWS-EC2").region("us-east-1")
				.machineSize("m1.small").launchTime(launchTime).build();
	}

	public static Machine machine(String id, MachineState machineState,
			MembershipStatus membershipStatus, DateTime launchTime) {
		return Machine.builder().id(id).machineState(machineState)
				.cloudProvider("AWS-EC2").region("us-east-1")
				.machineSize("m1.small").membershipStatus(membershipStatus)
				.launchTime(launchTime).build();
	}

	public static Machine machine(String id, String publicIp) {
		return Machine.builder().id(id).machineState(MachineState.RUNNING)
				.cloudProvider("AWS-EC2").region("us-east-1")
				.machineSize("m1.small").publicIp(publicIp).build();
	}

	public static List<Machine> machines(Machine... machines) {
		List<Machine> list = Lists.newArrayList();
		for (Machine machine : machines) {
			list.add(machine);
		}
		return list;
	}
}
