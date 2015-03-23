package com.elastisys.scale.cloudpool.openstack.functions;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;

import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.google.common.base.Function;

/**
 * A {@link Function} that takes a {@link Server} status and converts it to the
 * corresponding {@link MachineState}.
 */
public class StatusToMachineState implements Function<Status, MachineState> {

	@Override
	public MachineState apply(Status state) {
		if (asList(Status.ACTIVE, Status.ERROR, Status.PASSWORD)
				.contains(state)) {
			return MachineState.RUNNING;
		} else if (asList(Status.BUILD, Status.REBUILD, Status.REBOOT,
				Status.HARD_REBOOT, Status.RESIZE, Status.REVERT_RESIZE,
				Status.VERIFY_RESIZE).contains(state)) {
			return MachineState.PENDING;
		} else if (asList(Status.STOPPED, Status.SHUTOFF, Status.PAUSED,
				Status.SUSPENDED, Status.DELETED).contains(state)) {
			return MachineState.TERMINATED;
		} else {
			throw new IllegalArgumentException(format(
					"unrecognized instance state \"%s\"", state));
		}
	}
}
