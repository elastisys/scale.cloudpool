package com.elastisys.scale.cloudadapers.api;

/**
 * Thrown by a {@link CloudAdapter} to indicate an error condition.
 *
 * @see CloudAdapter
 * 
 */
public class CloudAdapterException extends RuntimeException {

	/** Serial UID version. */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new {@link CloudAdapterException}.
	 */
	public CloudAdapterException() {
		super();
	}

	/**
	 * Constructs a new {@link CloudAdapterException}.
	 *
	 * @param message
	 * @param cause
	 */
	public CloudAdapterException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new {@link CloudAdapterException}.
	 *
	 * @param message
	 */
	public CloudAdapterException(String message) {
		super(message);
	}

	/**
	 * Constructs a new {@link CloudAdapterException}.
	 *
	 * @param cause
	 */
	public CloudAdapterException(Throwable cause) {
		super(cause);
	}
}
