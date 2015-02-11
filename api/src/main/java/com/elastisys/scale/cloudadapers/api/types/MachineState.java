package com.elastisys.scale.cloudadapers.api.types;

/**
 * The range of permissible execution states for a {@link Machine} in a
 * {@link MachinePool}.
 * <p/>
 * The <i>machine state</i>, is the execution state of a {@link Machine}, as
 * reported by the infrastructure.
 *
 * @see Machine
 * @see MachinePool
 *
 *
 */
public enum MachineState {
	/**
	 * The machine has been requested from the underlying infrastructure and the
	 * request is pending fulfillment.
	 */
	REQUESTED,
	/** The machine request was rejected by the underlying infrastructure. */
	REJECTED,
	/** The machine is in the process of being launched. */
	PENDING,
	/**
	 * The machine is launched. However, the boot process may not yet have
	 * completed and the machine may not be operational (the {@link Machine}'s
	 * {@link ServiceState} may provide more detailed state information).
	 */
	RUNNING,
	/** The machine is in the process of being stopped/shut down. */
	TERMINATING,
	/** The machine has been stopped/shut down. */
	TERMINATED
}