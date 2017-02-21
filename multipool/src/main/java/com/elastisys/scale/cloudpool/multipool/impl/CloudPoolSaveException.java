package com.elastisys.scale.cloudpool.multipool.impl;

import com.elastisys.scale.cloudpool.multipool.api.CloudPoolInstance;

/**
 * Thrown by a {@link DiskBackedMultiCloudPool} on failure to save the state of
 * a {@link CloudPoolInstance}.
 */
public class CloudPoolSaveException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CloudPoolSaveException() {
        super();
    }

    public CloudPoolSaveException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CloudPoolSaveException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudPoolSaveException(String message) {
        super(message);
    }

    public CloudPoolSaveException(Throwable cause) {
        super(cause);
    }

}
