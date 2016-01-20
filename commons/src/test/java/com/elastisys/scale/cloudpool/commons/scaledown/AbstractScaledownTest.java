package com.elastisys.scale.cloudpool.commons.scaledown;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Base class for scaledown unit tests.
 */
public abstract class AbstractScaledownTest {

	public static Machine instance(String withId, String withLaunchTime) {
		return Machine.builder().id(withId).machineState(MachineState.RUNNING)
				.cloudProvider("AWS-EC2").region("us-east-1")
				.machineSize("m1.small")
				.launchTime(UtcTime.parse(withLaunchTime)).build();
	}
}
