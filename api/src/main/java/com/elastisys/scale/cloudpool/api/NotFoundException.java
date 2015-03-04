package com.elastisys.scale.cloudpool.api;

/**
 * Thrown by a {@link CloudPool} to indicate that a requested resource (such as
 * a machine pool member) could not be found.
 *
 * @see CloudPool
 */
public class NotFoundException extends CloudPoolException {

	/** Serial UID version. */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new {@link NotFoundException}.
	 */
	public NotFoundException() {
		super();
	}

	/**
	 * Constructs a new {@link NotFoundException}.
	 *
	 * @param message
	 * @param cause
	 */
	public NotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new {@link NotFoundException}.
	 *
	 * @param message
	 */
	public NotFoundException(String message) {
		super(message);
	}

	/**
	 * Constructs a new {@link NotFoundException}.
	 *
	 * @param cause
	 */
	public NotFoundException(Throwable cause) {
		super(cause);
	}
}
