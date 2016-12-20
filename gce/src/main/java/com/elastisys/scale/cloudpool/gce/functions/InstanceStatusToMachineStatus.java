package com.elastisys.scale.cloudpool.gce.functions;

import java.util.function.Function;

import com.elastisys.scale.cloudpool.api.types.MachineState;

/**
 * Translates a GCE <a href=
 * "https://cloud.google.com/compute/docs/reference/latest/instances#resource">instance
 * status</a> to its corresponding {@link MachineState}.
 */
public class InstanceStatusToMachineStatus implements Function<String, MachineState> {

    @Override
    public MachineState apply(String instanceStatus) {

        //

        switch (instanceStatus) {
        case "PROVISIONING":
            // PROVISIONING - Resources are being reserved for the instance. The
            // instance isn't running yet.
            return MachineState.PENDING;
        case "STAGING":
            // STAGING - Resources have been acquired and the instance is being
            // prepared for launch.
            return MachineState.PENDING;
        case "RUNNING":
            // RUNNING - The instance is booting up or running.
            return MachineState.RUNNING;
        case "STOPPING":
            // STOPPING - The instance is being stopped either due to a failure,
            // or the instance being shut down. This is a temporary status and
            // the instance will move to TERMINATED.
            return MachineState.TERMINATING;
        case "SUSPENDING":
            return MachineState.TERMINATING;
        case "SUSPENDED":
            return MachineState.TERMINATED;
        case "TERMINATED":
            // TERMINATED - The instance was shut down or encountered a failure,
            // either through the API or from inside the guest.
            return MachineState.TERMINATED;
        default:
            throw new IllegalArgumentException("unknown instance status: " + instanceStatus);
        }
    }

}
