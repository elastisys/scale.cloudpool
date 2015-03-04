package com.elastisys.scale.cloudpool.commons.basepool.driver;

import com.elastisys.scale.cloudpool.api.CloudPoolException;

/**
 * An exception raised by a {@link CloudPoolDriver} to indicate a failure to
 * complete an operation.
 */
public class CloudPoolDriverException extends CloudPoolException {

	/** Default serial version UID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a {@link CloudPoolDriverException}.
	 */
	public CloudPoolDriverException() {
		super();
	}

	/**
	 * Creates a {@link CloudPoolDriverException}.
	 *
	 * @param message
	 * @param cause
	 */
	public CloudPoolDriverException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates a {@link CloudPoolDriverException}.
	 *
	 * @param message
	 */
	public CloudPoolDriverException(String message) {
		super(message);
	}

	/**
	 * Creates a {@link CloudPoolDriverException}.
	 *
	 * @param cause
	 */
	public CloudPoolDriverException(Throwable cause) {
		super(cause);
	}

}
