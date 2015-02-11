package com.elastisys.scale.cloudadapters.commons.scaledown;

import java.util.List;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.Lists;

/**
 * Base class for scaledown unit tests.
 *
 *
 *
 */
public abstract class AbstractScaledownTest {

	public static Machine instance(String withId, String withLaunchTime) {
		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();
		return new Machine(withId, MachineState.RUNNING,
				UtcTime.parse(withLaunchTime), publicIps, privateIps);
	}
}
