package com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.impl;

import static com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics.POOL_FETCH;
import static java.lang.String.format;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.cloudpool.commons.basepool.config.PoolFetchConfig;
import com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.FetchOption;
import com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.PoolFetcher;
import com.elastisys.scale.commons.json.persistence.PersistentState;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertBuilder;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.net.alerter.Alerter;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.eventbus.EventBus;

/**
 * A {@link PoolFetcher} that caches {@link MachinePool}s retrieved by a wrapped
 * {@link PoolFetcher} for a configurable time (thereby also masking failures to
 * retrieve pool members from the backing cloud API).
 */
public class CachingPoolFetcher implements PoolFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(CachingPoolFetcher.class);

    /** Wrapped {@link PoolFetcher} to delegate actual fetching to. */
    private final PoolFetcher delegate;
    /** Controls fetch behavior. */
    private final PoolFetchConfig fetchConfig;
    /** The last pool fetch error. */
    private Throwable lastFetchError;
    /**
     * {@link CountDownLatch} that can be used to wait for the first fetch
     * attempt to complete (successful or not). See {@link #awaitFirstFetch()}.
     */
    private final CountDownLatch firstFetchComplete;

    /**
     * {@link EventBus} used to post {@link Alert} events that are to be
     * forwarded by configured {@link Alerter}s (if any).
     */
    private final EventBus eventBus;

    private final PersistentState<MachinePool> cachedMachinePool;

    /** Task that periodically refreshes the cached {@link MachinePool}. */
    private final ScheduledFuture<?> refreshTask;

    /**
     * Creates a {@link CachingPoolFetcher} with a given {@link PoolFetcher}
     * delegate and configuration. The first attempt to fetch the machine pool
     * will be executed immediately and can be waited for using
     * {@link #awaitFirstFetch()}.
     *
     * @param delegate
     *            Wrapped {@link PoolFetcher} to delegate actual fetching to.
     * @param fetchConfig
     *            Controls fetch behavior.
     */
    public CachingPoolFetcher(StateStorage stateStorage, PoolFetcher delegate, PoolFetchConfig fetchConfig,
            ScheduledExecutorService executor, EventBus eventBus) {
        this.delegate = delegate;
        this.fetchConfig = fetchConfig;
        this.eventBus = eventBus;

        this.cachedMachinePool = new PersistentState<MachinePool>(stateStorage.getCachedMachinePoolFile(),
                MachinePool.class);
        if (this.cachedMachinePool.get().isPresent()) {
            LOG.info("recovered cached machine pool: {}", this.cachedMachinePool.get().get());
        } else {
            LOG.info("no previously stored machine pool found.");
        }
        this.lastFetchError = null;
        this.firstFetchComplete = new CountDownLatch(1);

        this.refreshTask = startPeriodicalFetch(executor, fetchConfig);

        LOG.debug("started {}", getClass().getSimpleName());
    }

    private ScheduledFuture<?> startPeriodicalFetch(ScheduledExecutorService executor, PoolFetchConfig fetchConfig) {
        TimeInterval refreshInterval = fetchConfig.getRefreshInterval();
        return executor.scheduleWithFixedDelay(new PoolRefreshTask(this), 0L, refreshInterval.getTime(),
                refreshInterval.getUnit());
    }

    /**
     * Waits for the first pool fetch attempt to complete. The method returns
     * when the first attempt has completed (successful or not).
     *
     * @throws InterruptedException
     */
    public CachingPoolFetcher awaitFirstFetch() {
        try {
            this.firstFetchComplete.await();
            return this;
        } catch (InterruptedException e) {
            throw new RuntimeException(
                    String.format("interrupted while waiting for first pool fetch: %s", e.getMessage()), e);
        }
    }

    @Override
    public void close() {
        // stop periodical execution of cache update task
        LOG.debug("stopping {} ...", getClass().getSimpleName());
        if (this.refreshTask != null) {
            this.refreshTask.cancel(true);
        }
    }

    @Override
    public MachinePool get(FetchOption... options) throws CloudPoolException {
        if (forceRefresh(options)) {
            refreshCache();
            return this.cachedMachinePool.get().get();
        }

        if (cacheEmpty()) {
            LOG.debug("no machine pool in cache yet. failing ...");
            poolUnreachableFailure();
        }

        if (reachabilityTimeoutExceeded(this.cachedMachinePool.get().get())) {
            // pool has not been reachable since reachabilityTimeout
            LOG.debug("cached pool older than reachabilityTimeout. failing ...");
            reachabilityTimeoutFailure();
        }

        MachinePool cachedPool = this.cachedMachinePool.get().get();
        LOG.debug("responding with cached machine pool: {}", cachedPool);
        return cachedPool;
    }

    private void reachabilityTimeoutFailure() {
        throw new PoolReachabilityTimeoutException(String.format(
                "Could not serve a sufficiently up-to-date machine pool (%d %s). "
                        + "Cloud API presumably unreachable.",
                reachabilityTimeout().getTime(), reachabilityTimeout().getUnit().name().toLowerCase()));
    }

    private void poolUnreachableFailure() {
        Throwable lastError = this.lastFetchError;
        if (lastError != null) {
            throw new PoolUnreachableException(String.format(
                    "Could not serve machine pool: no fetch attempt " + "has been successful yet. Latest error: %s",
                    lastError.getMessage()), lastError);
        } else {
            throw new PoolUnreachableException("Could not serve machine pool: no fetch attempt has completed yet.");
        }
    }

    private boolean cacheEmpty() {
        return !this.cachedMachinePool.get().isPresent();
    }

    private TimeInterval reachabilityTimeout() {
        return this.fetchConfig.getReachabilityTimeout();
    }

    /**
     * Determines if the cached {@link MachinePool} is too old. That is, returns
     * <code>true</code> if the reachability timeout (or maximum fault masking
     * time) has been exceeded.
     *
     * @param machinePool
     * @return
     */
    private boolean reachabilityTimeoutExceeded(MachinePool machinePool) {
        DateTime now = UtcTime.now();

        DateTime cacheTimestamp = machinePool.getTimestamp();
        long cacheAgeSeconds = new Duration(cacheTimestamp, now).getStandardSeconds();

        TimeInterval reachabilityTimeout = reachabilityTimeout();
        long maxAgeSeconds = TimeUnit.SECONDS.convert(reachabilityTimeout.getTime(), reachabilityTimeout.getUnit());

        return cacheAgeSeconds >= maxAgeSeconds;
    }

    private boolean forceRefresh(FetchOption... options) {
        return options != null && Arrays.asList(options).contains(FetchOption.FORCE_REFRESH);
    }

    /**
     * Forces a refresh of the cached machine pool. In case of failure, an
     * {@link Alert} is posted on the {@link EventBus} and a
     * {@link CloudPoolException} is thrown.
     *
     * @throws CloudPoolException
     */
    void refreshCache() throws CloudPoolException {
        LOG.debug("refreshing cached cloud pool ...");
        try {
            this.cachedMachinePool.update(this.delegate.get(FetchOption.FORCE_REFRESH));
        } catch (Throwable e) {
            this.lastFetchError = e;
            String message = format("machine pool refresh failed: %s", e.getMessage());
            Alert alert = AlertBuilder.create().topic(POOL_FETCH.name()).severity(AlertSeverity.WARN).message(message)
                    .build();
            this.eventBus.post(alert);
            LOG.warn(message, e);
            throw new CloudPoolException(message, e);
        } finally {
            this.firstFetchComplete.countDown();
        }
    }

    /** Task that, when executed, updates the machine pool cache. */
    public static class PoolRefreshTask implements Runnable {
        private final CachingPoolFetcher poolFetcher;

        public PoolRefreshTask(CachingPoolFetcher poolFetcher) {
            this.poolFetcher = poolFetcher;
        }

        @Override
        public void run() {
            try {
                this.poolFetcher.refreshCache();
            } catch (Exception e) {
                // just catch exception to prevent periodical execution from
                // aborting
            }
        }
    }
}
