package com.elastisys.scale.cloudpool.multipool.impl;

import com.elastisys.scale.cloudpool.multipool.api.CloudPoolInstance;

/**
 * Thrown by a {@link DiskBackedMultiCloudPool} on failure to restore the state
 * of a {@link CloudPoolInstance} from disk.
 */
public class CloudPoolRestoreException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CloudPoolRestoreException() {
        super();
    }

    public CloudPoolRestoreException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CloudPoolRestoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudPoolRestoreException(String message) {
        super(message);
    }

    public CloudPoolRestoreException(Throwable cause) {
        super(cause);
    }

}
