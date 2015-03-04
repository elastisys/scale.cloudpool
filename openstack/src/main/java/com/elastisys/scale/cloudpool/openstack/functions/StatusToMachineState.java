package com.elastisys.scale.cloudpool.openstack.functions;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.ACTIVE;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.BUILD;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.ERROR;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.HARD_REBOOT;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.PASSWORD;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.REBOOT;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.REBUILD;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.RESIZE;
import static org.jclouds.openstack.nova.v2_0.domain.Server.Status.REVERT_RESIZE;

import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.Server.Status;

import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.google.common.base.Function;

/**
 * A {@link Function} that takes a {@link Server} status and converts it to the
 * corresponding {@link MachineState}.
 */
public class StatusToMachineState implements Function<Status, MachineState> {

	@Override
	public MachineState apply(Status state) {
		if (asList(ACTIVE, ERROR, PASSWORD).contains(state)) {
			return MachineState.RUNNING;
		} else if (asList(BUILD, REBUILD, REBOOT, HARD_REBOOT, RESIZE,
				REVERT_RESIZE, Status.VERIFY_RESIZE).contains(state)) {
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
