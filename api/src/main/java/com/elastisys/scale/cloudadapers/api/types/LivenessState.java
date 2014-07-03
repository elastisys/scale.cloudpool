package com.elastisys.scale.cloudadapers.api.types;

/**
 * The range of permissible liveness states.
 * 
 * Liveness states describe the operational status of {@link Machine}s in a
 * {@link MachinePool} that are in an active {@link MachineState} (either
 * {@link MachineState#PENDING} or {@link MachineState#RUNNING}).
 * 
 * 
 * 
 */
public enum LivenessState {
	/** The machine is being bootstrapped and may not (yet) be operational. */
	BOOTING,
	/** The machine is operational (liveness tests pass). */
	LIVE,
	/** The machine may not be operational (liveness tests fail). */
	UNHEALTHY,
	/**
	 * The liveness state of the machine is currently unknown (for example, it
	 * may not have been possible to determine yet).
	 */
	UNKNOWN;
}
