package com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.impl;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.types.MachinePool;

/**
 * Thrown by a {@link CachingPoolFetcher} to indicate that it cannot supply a
 * sufficiently up-to-date {@link MachinePool}. It hasn't been able to refresh
 * its {@link MachinePool} within the specified {@code refreshTimeout}.
 *
 * @see CachingPoolFetcher
 */
public class PoolReachabilityTimeoutException extends CloudPoolException {

    public PoolReachabilityTimeoutException() {
        super();
    }

    public PoolReachabilityTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public PoolReachabilityTimeoutException(String message) {
        super(message);
    }

    public PoolReachabilityTimeoutException(Throwable cause) {
        super(cause);
    }

}
