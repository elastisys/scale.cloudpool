package com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup;

import java.util.List;

import com.elastisys.scale.cloudadapers.api.types.Machine;

/**
 * Exception thrown by a {@link ScalingGroup} to signal that a request to start
 * a number of machines failed before being entirely complete.
 * <p/>
 * The error indicates the set of {@link Machine}s that were started (if any)
 * before the error occurred.
 *
 * 
 *
 */
public class StartMachinesException extends ScalingGroupException {
	/** Default serial version UID. */
	private static final long serialVersionUID = 1L;

	/** The number of {@link Machine}s that were requested. */
	private final int requestedMachines;

	/** {@link Machine}s that were started before the request failed. */
	private final List<Machine> startedMachines;

	/**
	 * Constructs a new {@link StartMachinesException}.
	 *
	 * @param requestedMachines
	 *            The number of {@link Machine}s that were requested.
	 * @param startedMachines
	 *            {@link Machine}s that were started before the request failed.
	 * @param cause
	 *            The error that caused the request to fail.
	 */
	public StartMachinesException(int requestedMachines,
			List<Machine> startedMachines, Throwable cause) {
		this(requestedMachines, startedMachines, cause, defaultErrorMessage(
				requestedMachines, startedMachines, cause));
	}

	/**
	 * Constructs a new {@link StartMachinesException} with a custom error
	 * detail message.
	 *
	 * @param requestedMachines
	 *            The number of {@link Machine}s that were requested.
	 * @param startedMachines
	 *            {@link Machine}s that were started before the request failed.
	 * @param cause
	 *            The error that caused the request to fail.
	 * @param message
	 *            The detail message.
	 */
	public StartMachinesException(int requestedMachines,
			List<Machine> startedMachines, Throwable cause, String message) {
		super(message, cause);
		this.requestedMachines = requestedMachines;
		this.startedMachines = startedMachines;
	}

	/**
	 * Returns the number of {@link Machine}s that were requested.
	 *
	 * @return
	 */
	public int getRequestedMachines() {
		return this.requestedMachines;
	}

	/**
	 * Returns the list of {@link Machine}s that were started before the request
	 * failed.
	 *
	 * @return
	 */
	public List<Machine> getStartedMachines() {
		return this.startedMachines;
	}

	private static String defaultErrorMessage(int numRequestedMachines,
			List<Machine> startedMachines, Throwable cause) {
		String message = String.format(
				"failure to complete request to start %d machine(s) "
						+ "(%d machine(s) were launched): %s",
				numRequestedMachines, startedMachines.size(),
				cause.getMessage());
		return message;
	}
}
