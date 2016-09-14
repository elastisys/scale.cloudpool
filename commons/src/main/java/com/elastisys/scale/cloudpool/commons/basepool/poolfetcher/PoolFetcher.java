package com.elastisys.scale.cloudpool.commons.basepool.poolfetcher;

import java.io.Closeable;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.types.MachinePool;

/**
 * Retrieves the {@link MachinePool} members from the cloud provider API.
 * Implementations may choose to respond with locally cached observations, but
 * must indicate the age of the observation in the timestamp of the
 * {@link MachinePool}.
 * <p/>
 * When its services are no longer needed, the {@link #close()} method of a
 * {@link PoolFetcher} should be called to allow the {@link PoolFetcher} to
 * release any held system resources.
 */
public interface PoolFetcher extends Closeable {
    /**
     * Returns the latest {@link MachinePool} observation.
     * <p/>
     * Implementations may choose to respond with locally cached observations,
     * but must indicate the age of the observation in the timestamp of the
     * {@link MachinePool}. {@link FetchOption}s can be used to control this
     * behavior to some degree.
     *
     * @param options
     *            Options that control how pool members are to be fetched.
     * @return A time-stamped {@link MachinePool} observation.
     * @throws CloudPoolException
     *             On failure to supply a (sufficiently up-to-date)
     *             {@link MachinePool}.
     */
    MachinePool get(FetchOption... options) throws CloudPoolException;

    /**
     * Closes this {@link PoolFetcher}, allowing it to release any held system
     * resources. A {@link PoolFetcher} can not be used after it has been
     * closed.
     */
    @Override
    void close();
}
