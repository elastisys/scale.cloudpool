package com.elastisys.scale.cloudpool.juju.client.impl;

import java.util.function.Function;

import com.elastisys.scale.cloudpool.api.types.MachineState;

/**
 * Performs the mapping between Juju unit states and {@link MachineState}. See
 * also the
 * <a href="https://jujucharms.com/docs/stable/reference-status" target=
 * "_blank">official documentation for unit states</a>.
 *
 * @author Elastisys AB <techteam@elastisys.com>
 */
public class JujuUnitStateToMachineState implements Function<String, MachineState> {
	@Override
	public MachineState apply(String state) {
		switch (state) {
		// relatively good mappings
		case "terminated":
			return MachineState.TERMINATED;
		case "active":
			return MachineState.RUNNING;
		case "maintenance":
			return MachineState.RUNNING;
		case "waiting":
			return MachineState.PENDING;
		// questionable mappings
		case "blocked":
			return MachineState.REQUESTED;
		case "error":
			return MachineState.REQUESTED;
		case "unknown":
			return MachineState.REQUESTED;
		default:
			throw new IllegalArgumentException(String.format("Unrecognized Juju unit state %s", state));
		}
	}
}
