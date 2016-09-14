package com.elastisys.scale.cloudpool.api;

/**
 * Thrown by a {@link CloudPool} to indicate that a request won't be serviced
 * due to the {@link CloudPool} being in a stopped state.
 *
 * @see CloudPool
 */
public class NotStartedException extends CloudPoolException {

    /** Serial UID version. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link NotStartedException}.
     */
    public NotStartedException() {
        super();
    }

    /**
     * Constructs a new {@link NotStartedException}.
     *
     * @param message
     * @param cause
     */
    public NotStartedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link NotStartedException}.
     *
     * @param message
     */
    public NotStartedException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link NotStartedException}.
     *
     * @param cause
     */
    public NotStartedException(Throwable cause) {
        super(cause);
    }
}
