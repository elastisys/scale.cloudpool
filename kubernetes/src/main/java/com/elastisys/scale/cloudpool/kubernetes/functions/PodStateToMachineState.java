package com.elastisys.scale.cloudpool.kubernetes.functions;

import java.util.function.Function;

import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.kubernetes.types.Pod;

/**
 * Converts a {@link Pod} <a href=
 * "https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/">state</a>
 * to a {@link MachineState}.
 *
 */
public class PodStateToMachineState implements Function<String, MachineState> {

    @Override
    public MachineState apply(String podState) {
        switch (podState) {
        case "Pending":
            return MachineState.PENDING;
        case "Running":
            return MachineState.RUNNING;
        case "Succeeded":
            return MachineState.TERMINATED;
        case "Failed":
            return MachineState.TERMINATED;
        default:
            throw new IllegalArgumentException(
                    String.format("unrecognized Kubernetes pod phase/state: '%s'", podState));
        }
    }

}
