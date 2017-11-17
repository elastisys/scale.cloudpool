package com.elastisys.scale.cloudpool.aws.ec2.functions;

import java.util.function.Function;

import com.amazonaws.services.ec2.model.InstanceState;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;

/**
 * {@link Function} that translates an EC2 API instance state representation to
 * its corresponding {@link Machine} state representation.
 */
public class InstanceStateToMachineState implements Function<InstanceState, MachineState> {

    /**
     * Convert an {@link InstanceState} to a {@link MachineState}.
     *
     * @see com.google.common.base.Function#apply(java.lang.Object)
     */
    @Override
    public MachineState apply(InstanceState state) {
        switch (state.getName()) {
        case "pending":
            return MachineState.PENDING;
        case "running":
            return MachineState.RUNNING;
        case "shutting-down":
            return MachineState.TERMINATING;
        case "terminated":
            return MachineState.TERMINATED;
        case "stopping":
            return MachineState.TERMINATING;
        case "stopped":
            return MachineState.TERMINATED;
        default:
            throw new IllegalArgumentException(
                    String.format("failed to translate unrecognized instance state '%s'", state.getName()));
        }
    }
}
