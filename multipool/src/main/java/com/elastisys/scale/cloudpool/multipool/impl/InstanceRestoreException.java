package com.elastisys.scale.cloudpool.multipool.impl;

import com.elastisys.scale.cloudpool.multipool.api.CloudPoolInstance;

/**
 * Thrown by a {@link DiskBackedMultiCloudPool} to indicate a problem to restore
 * all {@link CloudPoolInstance}s.
 */
public class InstanceRestoreException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InstanceRestoreException() {
        super();
    }

    public InstanceRestoreException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InstanceRestoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public InstanceRestoreException(String message) {
        super(message);
    }

    public InstanceRestoreException(Throwable cause) {
        super(cause);
    }

}
