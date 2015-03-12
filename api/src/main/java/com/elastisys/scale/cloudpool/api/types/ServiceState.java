package com.elastisys.scale.cloudpool.api.types;

/**
 * The range of permissible service states that a {@link Machine} can take on.
 * <p/>
 * The <i>service state</i>, is the operational state of the service running on
 * the machine. This is different from the {@link MachineState}, which is the
 * execution state of the {@link Machine} as reported by the infrastructure.
 */
public enum ServiceState {
	/**
	 * The service state of the machine cannot be (or has not yet been)
	 * determined.
	 */
	UNKNOWN,
	/**
	 * The service is being bootstrapped and may not (yet) be operational.
	 */
	BOOTING,
	/**
	 * The service is operational and ready to accept work (health checks pass).
	 */
	IN_SERVICE,
	/**
	 * The service is not functioning properly (health checks fail).
	 */
	UNHEALTHY,
	/**
	 * The service is unhealthy and is in need of repair. It should be
	 * considered out of service and not able to accept work until it has
	 * recovered. Machines in this service state are not to be considered part
	 * of the active machine pool and candidates for being replaced.
	 */
	OUT_OF_SERVICE;
}
