package com.elastisys.scale.cloudpool.api;

/**
 * Thrown by a {@link CloudPool} to indicate an error condition.
 *
 * @see CloudPool
 */
public class CloudPoolException extends RuntimeException {

    /** Serial UID version. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link CloudPoolException}.
     */
    public CloudPoolException() {
        super();
    }

    /**
     * Constructs a new {@link CloudPoolException}.
     *
     * @param message
     * @param cause
     */
    public CloudPoolException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link CloudPoolException}.
     *
     * @param message
     */
    public CloudPoolException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link CloudPoolException}.
     *
     * @param cause
     */
    public CloudPoolException(Throwable cause) {
        super(cause);
    }
}
