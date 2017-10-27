package com.elastisys.scale.cloudpool.api;

import com.elastisys.scale.cloudpool.api.types.MembershipStatus;

/**
 * Thrown to indicate that a given machine cannot be removed from the cloudpool
 * due to it having a membership status with {@code evictable: false}.
 * 
 * @see MembershipStatus
 */
public class NotEvictableException extends CloudPoolException {

    public NotEvictableException() {
        super();
    }

    public NotEvictableException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEvictableException(String message) {
        super(message);
    }

    public NotEvictableException(Throwable cause) {
        super(cause);
    }

}
