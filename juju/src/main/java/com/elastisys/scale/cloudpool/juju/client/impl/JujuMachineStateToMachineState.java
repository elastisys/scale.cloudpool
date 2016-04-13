package com.elastisys.scale.cloudpool.juju.client.impl;

import java.util.function.Function;

import com.elastisys.scale.cloudpool.api.types.MachineState;

/**
 * Performs the mapping between Juju machine states and {@link MachineState}.
 *
 * @author Elastisys AB <techteam@elastisys.com>
 */
public class JujuMachineStateToMachineState implements Function<String, MachineState> {
	@Override
	public MachineState apply(String state) {
		switch (state) {
		case "ACTIVE":
			return MachineState.RUNNING;
		default:
			throw new IllegalArgumentException(String.format("Unrecognized Juju unit state %s", state));
		}
	}
}
