package com.elastisys.scale.cloudpool.api;

/**
 * Thrown by a {@link CloudPool} to indicate that an operation failed due to the
 * {@link CloudPool} not being configured.
 *
 * @see CloudPool
 */
public class NotConfiguredException extends CloudPoolException {

    /** Serial UID version. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link NotConfiguredException}.
     */
    public NotConfiguredException() {
        super();
    }

    /**
     * Constructs a new {@link NotConfiguredException}.
     *
     * @param message
     * @param cause
     */
    public NotConfiguredException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link NotConfiguredException}.
     *
     * @param message
     */
    public NotConfiguredException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link NotConfiguredException}.
     *
     * @param cause
     */
    public NotConfiguredException(Throwable cause) {
        super(cause);
    }
}
