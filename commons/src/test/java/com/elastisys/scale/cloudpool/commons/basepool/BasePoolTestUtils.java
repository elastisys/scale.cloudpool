package com.elastisys.scale.cloudpool.commons.basepool;

import java.util.List;

import org.joda.time.DateTime;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.Lists;

public class BasePoolTestUtils {

	public static Machine machine(String id) {
		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();
		return new Machine(id, MachineState.RUNNING, ServiceState.UNKNOWN,
				UtcTime.now(), publicIps, privateIps);
	}

	public static Machine machine(String id, MachineState machineState) {
		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();
		return new Machine(id, machineState, ServiceState.UNKNOWN,
				UtcTime.now(), publicIps, privateIps);
	}

	public static Machine machine(String id, MachineState machineState,
			DateTime launchTime) {
		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();
		return new Machine(id, machineState, ServiceState.UNKNOWN, launchTime,
				publicIps, privateIps);
	}

	public static Machine machine(String id, MachineState machineState,
			ServiceState serviceState, DateTime launchTime) {
		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();
		return new Machine(id, machineState, serviceState, launchTime,
				publicIps, privateIps);
	}

	public static Machine machine(String id, String publicIp) {
		List<String> publicIps = Lists.newArrayList(publicIp);
		List<String> privateIps = Lists.newArrayList();
		return new Machine(id, MachineState.RUNNING, UtcTime.now(), publicIps,
				privateIps);
	}

	public static List<Machine> machines(Machine... machines) {
		List<Machine> list = Lists.newArrayList();
		for (Machine machine : machines) {
			list.add(machine);
		}
		return list;
	}
}
