package com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.impl;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.commons.basepool.config.RetriesConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.FetchOption;
import com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.PoolFetcher;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * A {@link PoolFetcher} that synchronously "calls through" to the cloud
 * provider API to get the {@link MachinePool} on each invocation and uses a
 * configurable number of retries (with exponential back-off) to handle faults.
 */
public class RetryingPoolFetcher implements PoolFetcher {

    /** A cloud-specific management driver for the cloud pool. */
    private final CloudPoolDriver cloudDriver;

    /**
     * Maximum number of retries to make on failed attempts to fetch pool
     * members.
     */
    private final int maxRetries;
    /** Initial delay to use in exponential back-off on retries. */
    private final TimeInterval initialBackoffDelay;

    /**
     * Creates a {@link RetryingPoolFetcher} that will fetch machine pool
     * members with the given {@link CloudPoolDriver} and retry handling.
     *
     * @param cloudDriver
     *            A cloud-specific management driver for the cloud pool.
     * @param retriesConfig
     *            Retry handling when fetching pool members from the cloud API
     *            fails.
     */
    public RetryingPoolFetcher(CloudPoolDriver cloudDriver, RetriesConfig retriesConfig) {
        this(cloudDriver, retriesConfig.getMaxRetries(), retriesConfig.getInitialBackoffDelay());
    }

    @Override
    public void close() {
        // nothing to be done
    }

    /**
     * Creates a {@link RetryingPoolFetcher} that will fetch machine pool
     * members with the given {@link CloudPoolDriver}.
     *
     * @param cloudDriver
     *            A cloud-specific management driver for the cloud pool.
     * @param maxRetries
     *            Maximum number of retries to make on failed attempts to fetch
     *            pool members.
     * @param initialBackoffDelay
     *            Initial delay to use in exponential back-off on retries.
     */
    public RetryingPoolFetcher(CloudPoolDriver cloudDriver, int maxRetries, TimeInterval initialBackoffDelay) {
        this.cloudDriver = cloudDriver;
        this.maxRetries = maxRetries;
        this.initialBackoffDelay = initialBackoffDelay;
    }

    @Override
    public MachinePool get(FetchOption... options) throws CloudPoolException {
        List<Machine> machines = listMachines();
        MachinePool pool = new MachinePool(machines, UtcTime.now());
        return pool;
    }

    /**
     * Lists the {@link Machine}s using the {@link CloudPoolDriver}.
     *
     * @return
     */
    private List<Machine> listMachines() throws CloudPoolException {
        int backoffDelay = this.initialBackoffDelay.getTime().intValue();
        TimeUnit backoffDelayUnit = this.initialBackoffDelay.getUnit();
        int maxAttempts = 1 + this.maxRetries;
        Retryable<List<Machine>> retryable = Retryers.exponentialBackoffRetryer("pool-fetch",
                new GetMachinePool(this.cloudDriver), backoffDelay, backoffDelayUnit, maxAttempts);
        try {
            return retryable.call();
        } catch (Exception e) {
            throw new CloudPoolException(String.format("gave up trying to fetch pool members: %s", e.getMessage()), e);
        }
    }

    private static class GetMachinePool implements Callable<List<Machine>> {
        private final CloudPoolDriver cloudDriver;

        public GetMachinePool(CloudPoolDriver cloudDriver) {
            this.cloudDriver = cloudDriver;
        }

        @Override
        public List<Machine> call() throws Exception {
            return this.cloudDriver.listMachines();
        }
    }
}
