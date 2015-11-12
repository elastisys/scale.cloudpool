package com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.impl;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.types.MachinePool;

/**
 * Thrown by a {@link CachingPoolFetcher} to indicate that it cannot supply a
 * {@link MachinePool}, since no attempt to fetch the pool has been successful
 * yet.
 * <p/>
 * If one or more pool fetch attempts have been made, the latest error should be
 * passed as the {@code cause}. If no pool fetch attempt has completed yet, the
 * {@code cause} is left out and {@link #noFetchAttemptCompletedYet()} will
 * return <code>true</code>.
 *
 * @see CachingPoolFetcher
 */
public class PoolUnreachableException extends CloudPoolException {

	public PoolUnreachableException() {
		super();
	}

	public PoolUnreachableException(String message, Throwable cause) {
		super(message, cause);
	}

	public PoolUnreachableException(String message) {
		super(message);
	}

	public PoolUnreachableException(Throwable cause) {
		super(cause);
	}

	/**
	 * Returns <code>true</code> if no pool fetch attempt has completed yet.
	 *
	 * @return
	 */
	public boolean noFetchAttemptCompletedYet() {
		return this.getCause() == null;
	}

}
