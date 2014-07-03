package com.elastisys.scale.cloudadapters.commons.adapter;

import java.util.List;

import org.joda.time.DateTime;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.LivenessConfig;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

public class BaseAdapterTestUtils {

	public static Machine machine(String id) {
		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();
		return new Machine(id, MachineState.RUNNING, UtcTime.now(), publicIps,
				privateIps, new JsonObject());
	}

	public static Machine machine(String id, MachineState state) {
		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();
		return new Machine(id, state, UtcTime.now(), publicIps, privateIps,
				new JsonObject());
	}

	public static Machine machine(String id, MachineState state,
			DateTime launchTime) {
		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();
		return new Machine(id, state, launchTime, publicIps, privateIps,
				new JsonObject());
	}

	public static Machine machine(String id, String publicIp) {
		List<String> publicIps = Lists.newArrayList(publicIp);
		List<String> privateIps = Lists.newArrayList();
		return new Machine(id, MachineState.RUNNING, UtcTime.now(), publicIps,
				privateIps, new JsonObject());
	}

	public static LivenessConfig validLivenessConfig() {
		LivenessConfig livenessConfig = new LivenessConfig(
				22,
				"ubuntu",
				"src/test/resources/security/clientkey.pem",
				new BaseCloudAdapterConfig.BootTimeLivenessCheck(
						"service apache2 status | grep 'is running'", 20, 15),
				new BaseCloudAdapterConfig.RunTimeLivenessCheck(
						"service apache2 status | grep 'is running'", 60, 3, 10));
		return livenessConfig;
	}

	public static LivenessConfig validConfig2() {
		LivenessConfig livenessConfig = new LivenessConfig(
				22,
				"ubuntu",
				"src/test/resources/security/clientkey.pem",
				new BaseCloudAdapterConfig.BootTimeLivenessCheck(
						"service tcollector status | grep 'is running'", 10, 10),
				new BaseCloudAdapterConfig.RunTimeLivenessCheck(
						"service tcollector status | grep 'is running'", 120,
						2, 5));
		return livenessConfig;
	}

	public static List<Machine> machines(Machine... machines) {
		List<Machine> list = Lists.newArrayList();
		for (Machine machine : machines) {
			list.add(machine);
		}
		return list;
	}
}
