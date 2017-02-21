package com.elastisys.scale.cloudpool.multipool.api;

import com.elastisys.scale.cloudpool.api.CloudPool;

/**
 * Thrown by a {@link MultiCloudPool} to indicate a problem to delete a
 * {@link CloudPool}.
 *
 * @see MultiCloudPool
 */
public class CloudPoolDeleteException extends RuntimeException {

    public CloudPoolDeleteException() {
        super();
    }

    public CloudPoolDeleteException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CloudPoolDeleteException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudPoolDeleteException(String message) {
        super(message);
    }

    public CloudPoolDeleteException(Throwable cause) {
        super(cause);
    }

}
