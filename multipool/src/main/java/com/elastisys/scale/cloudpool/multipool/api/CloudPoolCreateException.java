package com.elastisys.scale.cloudpool.multipool.api;

import com.elastisys.scale.cloudpool.api.CloudPool;

/**
 * Thrown by a {@link MultiCloudPool} to indicate a problem to instantiate a
 * {@link CloudPool}.
 *
 * @see MultiCloudPool
 */
public class CloudPoolCreateException extends RuntimeException {

    public CloudPoolCreateException() {
        super();
    }

    public CloudPoolCreateException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CloudPoolCreateException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudPoolCreateException(String message) {
        super(message);
    }

    public CloudPoolCreateException(Throwable cause) {
        super(cause);
    }

}
