package com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.impl;

import static com.elastisys.scale.cloudpool.commons.basepool.IsAlert.isAlert;
import static com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics.POOL_FETCH;
import static com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.FetchOption.FORCE_REFRESH;
import static com.elastisys.scale.commons.net.alerter.AlertSeverity.WARN;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.types.CloudProviders;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.cloudpool.commons.basepool.config.PoolFetchConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.RetriesConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.FetchOption;
import com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.PoolFetcher;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.elastisys.scale.commons.util.concurrent.Sleep;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercise the {@link CachingPoolFetcher}.
 */
public class TestCachingPoolFetcher {

    private static final Logger LOG = LoggerFactory.getLogger(TestCachingPoolFetcher.class);

    private static final File STATE_STORAGE_DIR = new File(
            "target/state-" + TestCachingPoolFetcher.class.getSimpleName());
    private static final StateStorage STATE_STORAGE = StateStorage.builder(STATE_STORAGE_DIR).build();

    /**
     * Delegate {@link PoolFetcher} that is wrapped by the
     * {@link CachingPoolFetcher}.
     */
    private final PoolFetcher delegate = mock(PoolFetcher.class);
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    /** {@link EventBus} used to carry {@link Alert}s. */
    private final EventBus mockEventbus = mock(EventBus.class);

    private static final Integer REACHABILITY_TIMEOUT_MINUTES = 5;
    private static final TimeInterval REACHABILITY_TIMEOUT = new TimeInterval(REACHABILITY_TIMEOUT_MINUTES.longValue(),
            TimeUnit.MINUTES);
    private static final TimeInterval REFRESH_INTERVAL = new TimeInterval(30L, TimeUnit.SECONDS);
    private static final PoolFetchConfig FETCH_CONFIG = new PoolFetchConfig(
            new RetriesConfig(3, new TimeInterval(10L, TimeUnit.MILLISECONDS)), REFRESH_INTERVAL, REACHABILITY_TIMEOUT);

    @Before
    public void beforeTestMethod() throws Exception {
        deleteStateDir();
        FrozenTime.setFixed(UtcTime.parse("2015-11-16T12:00:00.000Z"));
    }

    private void deleteStateDir() throws Exception {
        if (STATE_STORAGE_DIR.exists()) {
            // make several attempts: appears to fail sometimes
            Callable<Void> deleteDirTask = () -> {
                FileUtils.deleteRecursively(STATE_STORAGE_DIR);
                return null;
            };
            Retryers.fixedDelayRetryer("delete-test-dir", deleteDirTask, 1, TimeUnit.SECONDS, 3).call();
        }
    }

    /**
     * On creation, the {@link CachingPoolFetcher} should call through to its
     * delegate to populate the cache.
     */
    @Test
    public void populateCacheOnFirstCall() {
        when(this.delegate.get(FORCE_REFRESH)).thenReturn(pool(machines("i-1", "i-2")));

        CachingPoolFetcher fetcher = new CachingPoolFetcher(STATE_STORAGE, this.delegate, FETCH_CONFIG, this.executor,
                this.mockEventbus);
        fetcher.awaitFirstFetch();

        MachinePool pool = fetcher.get();
        assertThat(pool, is(pool(machines("i-1", "i-2"))));

        // a call through to the delegate pool fetcher should have been made
        verify(this.delegate, times(1)).get(FORCE_REFRESH);

        fetcher.close();
    }

    /**
     * If attempts have been made to fetch the pool, but none has been
     * successful yet, the {@link CachingPoolFetcher} should fail with a
     * {@link PoolUnreachableException}.
     */
    @Test
    public void failIfNoFetchHasBeenSuccessful() {
        when(this.delegate.get(FORCE_REFRESH)).thenThrow(new CloudPoolException("api outage"));

        CachingPoolFetcher fetcher = new CachingPoolFetcher(STATE_STORAGE, this.delegate, FETCH_CONFIG, this.executor,
                this.mockEventbus);
        fetcher.awaitFirstFetch();

        try {
            fetcher.get();
            fail("expected to fail");
        } catch (PoolUnreachableException e) {
            assertFalse(e.noFetchAttemptCompletedYet());
        }

        // a single attempt to call the delegate pool fetcher should have been
        // made
        verify(this.delegate, times(1)).get(FORCE_REFRESH);
        verify(this.mockEventbus).post(argThat(isAlert(POOL_FETCH.name(), WARN)));
        fetcher.close();
    }

    /**
     * If no attempt to fetch the cloud pool has completed yet, the
     * {@link CachingPoolFetcher} should fail with a
     * {@link PoolUnreachableException} that indicates that no attempt has
     * completed yet.
     */
    @Test
    public void failIfNoFetchAttemptHasCompletedYet() {
        // the delegate PoolFetch is slow to respond
        when(this.delegate.get(FORCE_REFRESH)).thenAnswer(invocation -> {
            LOG.debug("slow responding PoolFetcher called. delaying ...");
            Sleep.forTime(500, TimeUnit.MILLISECONDS);
            return pool(machines("i-1", "i-2"));
        });

        CachingPoolFetcher fetcher = new CachingPoolFetcher(STATE_STORAGE, this.delegate, FETCH_CONFIG, this.executor,
                this.mockEventbus);
        // note: don't wait for first fetch to complete, attempt is ongoing ...
        try {
            fetcher.get();
            fail("expected to fail, since no fetch has completed yet");
        } catch (PoolUnreachableException e) {
            assertTrue(e.noFetchAttemptCompletedYet());
        }
        fetcher.close();
    }

    /**
     * Should respond with cached {@link MachinePool} when one (sufficiently
     * up-to-date) is available.
     */
    @Test
    public void cachingBehavior() {
        MachinePool initialPool = pool(machines("i-1", "i-2"));
        when(this.delegate.get(FORCE_REFRESH)).thenReturn(initialPool);

        // populate cache
        CachingPoolFetcher fetcher = new CachingPoolFetcher(STATE_STORAGE, this.delegate, FETCH_CONFIG, this.executor,
                this.mockEventbus);
        fetcher.awaitFirstFetch();
        MachinePool cachedPool = fetcher.get();
        assertThat(cachedPool, is(initialPool));
        verify(this.delegate, times(1)).get(FORCE_REFRESH);

        // from now on, timestamped cached values are to be returned
        assertThat(fetcher.get(), is(cachedPool));
        FrozenTime.tick(60);
        assertThat(fetcher.get(), is(cachedPool));
        FrozenTime.tick(60);
        assertThat(fetcher.get(), is(cachedPool));
        // no more calls through to delegate pool fetcher should have been made
        verify(this.delegate, times(1)).get(FORCE_REFRESH);
        fetcher.close();
    }

    /**
     * A client can force a cache refresh with the
     * {@link FetchOption#FORCE_REFRESH} option.
     */
    @Test
    public void forceRefresh() {
        MachinePool initialPool = pool(machines("i-1", "i-2"));
        when(this.delegate.get(FORCE_REFRESH)).thenReturn(initialPool);

        // populate cache
        CachingPoolFetcher fetcher = new CachingPoolFetcher(STATE_STORAGE, this.delegate, FETCH_CONFIG, this.executor,
                this.mockEventbus);
        fetcher.awaitFirstFetch();
        MachinePool cachedPool = fetcher.get();
        assertThat(cachedPool, is(initialPool));
        verify(this.delegate, times(1)).get(FORCE_REFRESH);
        // from now on, timestamped cached values are to be returned
        assertThat(fetcher.get(), is(cachedPool));
        FrozenTime.tick(60);
        verify(this.delegate, times(1)).get(FORCE_REFRESH);

        MachinePool newPool = pool(machines("i-1", "i-2", "i-3"));
        when(this.delegate.get(FORCE_REFRESH)).thenReturn(newPool);
        // force a refresh
        assertThat(fetcher.get(FORCE_REFRESH), is(newPool));
        // ... should call through to delegate
        verify(this.delegate, times(2)).get(FORCE_REFRESH);
        fetcher.close();
    }

    /**
     * Should respond with cached {@link MachinePool} until the cached value is
     * older than {@code reachabilityTimeout} (then it should respond with a
     * {@link CloudPoolException}).
     */
    @Test
    public void failOnReachabilityTimeoutExceeded() {
        MachinePool initialPool = pool(machines("i-1", "i-2"));
        when(this.delegate.get(FORCE_REFRESH)).thenReturn(initialPool);

        // populate cache
        CachingPoolFetcher fetcher = new CachingPoolFetcher(STATE_STORAGE, this.delegate, FETCH_CONFIG, this.executor,
                this.mockEventbus);
        fetcher.awaitFirstFetch();
        MachinePool cachedPool = fetcher.get();
        assertThat(cachedPool, is(initialPool));
        verify(this.delegate, times(1)).get(FORCE_REFRESH);

        // from now on, timestamped cached values are to be returned
        assertThat(fetcher.get(), is(cachedPool));
        verify(this.delegate, times(1)).get(FORCE_REFRESH);
        FrozenTime.tick(60);
        assertThat(fetcher.get(), is(cachedPool));
        verify(this.delegate, times(1)).get(FORCE_REFRESH);

        // after reachabilityTimout is passed the cloud pool should be deemed
        // unreachable
        FrozenTime.tick(4 * 60);
        try {
            fetcher.get();
            fail("expected failure");
        } catch (PoolReachabilityTimeoutException e) {
            // expected
        }
        fetcher.close();
    }

    /**
     * {@link Alert}s should be posted on the {@link EventBus} on failures to
     * refresh the cache.
     */
    @Test
    public void alertOnFailuresToRefreshCache() {
        when(this.delegate.get(FORCE_REFRESH)).thenThrow(new CloudPoolDriverException("api outage"));

        CachingPoolFetcher fetcher = new CachingPoolFetcher(STATE_STORAGE, this.delegate, FETCH_CONFIG, this.executor,
                this.mockEventbus);
        fetcher.awaitFirstFetch();
        try {
            fetcher.get();
            fail("expected to fail");
        } catch (CloudPoolException e) {
            // expected
        }
        // a call through to the delegate pool fetcher should have been made
        verify(this.delegate, times(1)).get(FetchOption.FORCE_REFRESH);
        // ... and an alert should have been posted on the event bus
        verify(this.mockEventbus).post(argThat(isAlert(POOL_FETCH.name(), WARN)));
        fetcher.close();
    }

    /**
     * Verify that machine pool observations get written to disk and are
     * properly restored on re-instantiation of the {@link CachingPoolFetcher}.
     */
    @Test
    public void persistence() {
        when(this.delegate.get(FORCE_REFRESH)).thenReturn(pool(machines("i-1", "i-2")));

        File cacheFile = STATE_STORAGE.getCachedMachinePoolFile();
        assertThat(cacheFile.exists(), is(false));

        CachingPoolFetcher fetcher = new CachingPoolFetcher(STATE_STORAGE, this.delegate, FETCH_CONFIG, this.executor,
                this.mockEventbus);
        fetcher.awaitFirstFetch();

        // the fetched machine pool should be cached and written to disk
        assertThat(cacheFile.exists(), is(true));
        assertThat(restore(cacheFile), is(fetcher.get()));
        assertThat(restore(cacheFile), is(pool(machines("i-1", "i-2"))));

        // verify that changes get saved
        when(this.delegate.get(FORCE_REFRESH)).thenReturn(pool(machines("i-1", "i-2", "i-3")));
        assertThat(fetcher.get(FetchOption.FORCE_REFRESH), is(pool(machines("i-1", "i-2", "i-3"))));
        assertThat(restore(cacheFile), is(pool(machines("i-1", "i-2", "i-3"))));

        when(this.delegate.get(FORCE_REFRESH)).thenReturn(pool(machines()));
        fetcher.get(FetchOption.FORCE_REFRESH);
        assertThat(restore(cacheFile), is(pool(machines())));
        fetcher.close();
    }

    /**
     * Verify that the {@link CachingPoolFetcher} restores its cache (if one
     * exists) on creation.
     */
    @Test
    public void restoreOnCreation() throws IOException {
        MachinePool cachedPool = pool(machines("i-1", "i-2"));
        File cacheFile = STATE_STORAGE.getCachedMachinePoolFile();
        save(cachedPool, cacheFile);

        // create pool fetcher and verify that the cached pool is restored from
        // disk
        CachingPoolFetcher fetcher = new CachingPoolFetcher(STATE_STORAGE, this.delegate, FETCH_CONFIG, this.executor,
                this.mockEventbus);
        assertThat(fetcher.get(), is(cachedPool));
        fetcher.close();
    }

    /**
     * A {@link MachinePool} restored from cache that is not sufficiently
     * up-to-date should give rise to a {@link PoolReachabilityTimeoutException}
     * when asked for.
     */
    @Test(expected = PoolReachabilityTimeoutException.class)
    public void honorReachabilityTimeoutWithRestoredCache() throws IOException {
        DateTime cachedTimestamp = FrozenTime.now().minusMinutes(REACHABILITY_TIMEOUT_MINUTES + 1);
        MachinePool outdatedPool = new MachinePool(machines("i-1", "i-2"), cachedTimestamp);
        File cacheFile = STATE_STORAGE.getCachedMachinePoolFile();
        save(outdatedPool, cacheFile);

        // restored cache entry is not sufficiently up-to-date so a reachability
        // timeout error should be raised
        CachingPoolFetcher fetcher = new CachingPoolFetcher(STATE_STORAGE, this.delegate, FETCH_CONFIG, this.executor,
                this.mockEventbus);
        System.out.println(fetcher.get());
        fetcher.close();
    }

    private void save(MachinePool pool, File destination) throws IOException {
        Files.createDirectories(destination.getParentFile().toPath());
        Files.write(destination.toPath(), JsonUtils.toPrettyString(JsonUtils.toJson(pool)).getBytes());
    }

    /**
     * Loads a {@link MachinePool} from a given file.
     *
     * @param machinePoolCacheFile
     * @return
     */
    private MachinePool restore(File machinePoolCacheFile) {
        return JsonUtils.toObject(JsonUtils.parseJsonFile(machinePoolCacheFile), MachinePool.class);
    }

    private MachinePool pool(List<Machine> machines) {
        return new MachinePool(machines, UtcTime.now());
    }

    private List<Machine> machines(String... machineIds) {
        List<Machine> machines = new ArrayList<>();
        for (String id : machineIds) {
            machines.add(Machine.builder().id(id).machineSize("m1.medium").machineState(MachineState.RUNNING)
                    .cloudProvider(CloudProviders.AWS_EC2).region("us-east-1").build());
        }
        return machines;
    }
}
