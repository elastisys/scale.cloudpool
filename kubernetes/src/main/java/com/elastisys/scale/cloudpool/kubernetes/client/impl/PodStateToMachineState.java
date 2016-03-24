package com.elastisys.scale.cloudpool.kubernetes.client.impl;

import java.util.function.Function;

import com.elastisys.scale.cloudpool.api.types.MachineState;

/**
 * Converts a <a href="http://kubernetes.io/docs/user-guide/pod-states/">pod
 * state</a> to a {@link MachineState}.
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
			throw new IllegalArgumentException(String.format(
					"unrecognized Kubernetes pod phase/state '%s'", podState));
		}
	}

}
