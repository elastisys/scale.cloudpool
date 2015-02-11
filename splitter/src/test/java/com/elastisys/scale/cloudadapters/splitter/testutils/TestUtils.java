package com.elastisys.scale.cloudadapters.splitter.testutils;

import java.util.List;

import jersey.repackaged.com.google.common.collect.Lists;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.commons.util.time.UtcTime;

public class TestUtils {
	/**
	 * Creates a {@link MachinePool} with a collection of {@link Machine}s and
	 * the current time as timestamp.
	 * 
	 * @param machines
	 * @return
	 */
	public static MachinePool pool(Machine... machines) {
		List<Machine> machineList = Lists.newArrayList(machines);
		return new MachinePool(machineList, UtcTime.now());
	}

	/**
	 * Creates a {@link Machine} with name {@code i-<index>}.
	 *
	 * @param index
	 * @return
	 */
	public static Machine machine(int index) {
		return new Machine("i-" + index, MachineState.RUNNING, null, null, null);
	}
}
