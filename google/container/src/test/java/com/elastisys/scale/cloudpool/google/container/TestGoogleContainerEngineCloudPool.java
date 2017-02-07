package com.elastisys.scale.cloudpool.google.container;

import static com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics.RESIZE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.google.commons.api.CloudApiSettings;
import com.elastisys.scale.cloudpool.google.container.client.ClusterSnapshot;
import com.elastisys.scale.cloudpool.google.container.client.ContainerClusterClient;
import com.elastisys.scale.cloudpool.google.container.config.ContainerCluster;
import com.elastisys.scale.cloudpool.google.container.config.GoogleContainerEngineCloudPoolConfig;
import com.elastisys.scale.cloudpool.google.container.config.ScalingPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.AlertWaiter;
import com.elastisys.scale.commons.net.alerter.multiplexing.AlertersConfig;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonObject;

public class TestGoogleContainerEngineCloudPool {
    private static final Logger LOG = LoggerFactory.getLogger(TestGoogleContainerEngineCloudPool.class);

    /** Executor used by cloud pool to schedule tasks. */
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);

    // Mock objects
    private ContainerClusterClient mockedApiClient;
    private EventBus eventBus = new AsyncEventBus(this.executor);

    /** Object under test. */
    private GoogleContainerEngineCloudPool cloudPool;

    @Before
    public void beforeTestMethod() {
        FrozenTime.setFixed(UtcTime.parse("2017-01-01T12:00:00.000Z"));

        this.executor.setRemoveOnCancelPolicy(true);
        this.mockedApiClient = mock(ContainerClusterClient.class);
        this.cloudPool = new GoogleContainerEngineCloudPool(this.mockedApiClient, this.eventBus, this.executor);
    }

    /**
     * A valid configuration file should be possible to set and should also
     * result in the {@link ContainerClusterClient} being configured.
     */
    @Test
    public void configFromFile() {
        assertThat(this.cloudPool.getConfiguration().isPresent(), is(false));
        assertThat(this.cloudPool.getStatus(), is(CloudPoolStatus.UNCONFIGURED_STOPPED));

        JsonObject config = JsonUtils.parseJsonResource("config/valid-config.json").getAsJsonObject();
        this.cloudPool.configure(config);

        assertThat(this.cloudPool.getConfiguration().isPresent(), is(true));
        assertThat(this.cloudPool.getConfiguration().get(), is(config));
        assertThat(this.cloudPool.getStatus(), is(CloudPoolStatus.CONFIGURED_STOPPED));

        // verify that a call-through was made to configure client
        verify(this.mockedApiClient).configure(this.cloudPool.config().getCloudApiSettings());
    }

    /**
     * When passed an illegal configuration, no part of the configuration should
     * be applied.
     */
    @Test
    public void invalidConfigureFromFile() {
        assertThat(this.cloudPool.getConfiguration().isPresent(), is(false));
        assertThat(this.cloudPool.getStatus(), is(CloudPoolStatus.UNCONFIGURED_STOPPED));

        JsonObject config = JsonUtils.parseJsonResource("config/invalid-config.json").getAsJsonObject();
        try {
            this.cloudPool.configure(config);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            // expected
            assertThat(e.getMessage(), is("invalid cloudpool config: cluster: missing zone"));
        }

        assertThat(this.cloudPool.getConfiguration().isPresent(), is(false));
        assertThat(this.cloudPool.getStatus(), is(CloudPoolStatus.UNCONFIGURED_STOPPED));

        // verify that no call was made to configure client
        verifyZeroInteractions(this.mockedApiClient);
    }

    /**
     * Should not be possible to start prior to configuring.
     */
    @Test(expected = IllegalStateException.class)
    public void startBeforeConfigured() {
        this.cloudPool.start();
    }

    /**
     * It should be possible to restart cloud pool.
     */
    @Test
    public void startAndStop() {
        assertThat(this.executor.getQueue().size(), is(0));

        this.cloudPool.configure(validConfigAsJson());

        assertThat(this.cloudPool.getStatus().isStarted(), is(false));
        assertThat(this.executor.getQueue().size(), is(0));

        this.cloudPool.start();
        assertThat(this.cloudPool.getStatus().isStarted(), is(true));

        this.cloudPool.stop();
        assertThat(this.cloudPool.getStatus().isStarted(), is(false));
        // make sure stop stops any running tasks
        assertThat(this.executor.getQueue().size(), is(0));

        this.cloudPool.start();
        assertThat(this.cloudPool.getStatus().isStarted(), is(true));
    }

    /**
     * start operation should be idempotent.
     */
    @Test
    public void startIsIdempotent() {
        this.cloudPool.configure(validConfigAsJson());

        this.cloudPool.start();
        assertThat(this.executor.getQueue().size(), is(1));
        this.cloudPool.start();
        // should not schedule any additional tasks
        assertThat(this.executor.getQueue().size(), is(1));
        assertThat(this.cloudPool.getStatus().isStarted(), is(true));
    }

    /**
     * stop operation should be idempotent.
     */
    @Test
    public void stopIsIdempotent() {
        this.cloudPool.configure(validConfigAsJson());

        this.cloudPool.start();
        assertThat(this.cloudPool.getStatus().isStarted(), is(true));
        this.cloudPool.stop();
        this.cloudPool.stop();
        assertThat(this.cloudPool.getStatus().isStarted(), is(false));
    }

    /**
     * On reconfiguration, make sure client gets reconfigured.
     */
    @Test
    public void reconfigure() {
        GoogleContainerEngineCloudPoolConfig config1 = validConfig();
        this.cloudPool.configure(asJson(config1));
        assertThat(this.cloudPool.config(), is(config1));

        reset(this.mockedApiClient);

        GoogleContainerEngineCloudPoolConfig config2 = config1.withCloudApiSettings(
                new CloudApiSettings("src/test/resources/config/valid-service-account-key2.json", null));
        this.cloudPool.configure(asJson(config2));

        // verify that new config is passed on to client
        verify(this.mockedApiClient).configure(config2.getCloudApiSettings());

    }

    /**
     * Run state should be preserved when a new configuration is set.
     */
    @Test
    public void reconfigureStopped() {
        GoogleContainerEngineCloudPoolConfig config1 = validConfig();
        this.cloudPool.configure(asJson(config1));
        assertThat(this.cloudPool.getStatus().isStarted(), is(false));
        assertThat(this.executor.getQueue().size(), is(0));

        // reconfigure
        GoogleContainerEngineCloudPoolConfig config2 = config1.withCloudApiSettings(
                new CloudApiSettings("src/test/resources/config/valid-service-account-key2.json", null));
        this.cloudPool.configure(asJson(config2));

        // should still be stopped
        assertThat(this.cloudPool.getStatus().isStarted(), is(false));
        assertThat(this.executor.getQueue().size(), is(0));
    }

    /**
     * Run state should be preserved when a new configuration is set.
     */
    @Test
    public void reconfigureStarted() {
        GoogleContainerEngineCloudPoolConfig config1 = validConfig();
        this.cloudPool.configure(asJson(config1));
        this.cloudPool.start();
        assertThat(this.cloudPool.getStatus().isStarted(), is(true));
        assertThat(this.executor.getQueue().size(), is(1));

        // reconfigure
        GoogleContainerEngineCloudPoolConfig config2 = config1.withCloudApiSettings(
                new CloudApiSettings("src/test/resources/config/valid-service-account-key2.json", null));
        this.cloudPool.configure(asJson(config2));

        // should still be started
        assertThat(this.cloudPool.getStatus().isStarted(), is(true));
        assertThat(this.executor.getQueue().size(), is(1));
    }

    /**
     * Verify that the {@link ClusterSnapshot} is correctly translated to a set
     * of {@link Machine}s.
     */
    @Test
    public void getMachinePool() {
        GoogleContainerEngineCloudPoolConfig config = validConfig();
        this.cloudPool.configure(asJson(config));
        this.cloudPool.start();

        // prepare client to return a given cluster snapshot
        SimulatedCluster fakeCluster = new SimulatedCluster(config.getCluster(), //
                ImmutableMap.of(//
                        "nodePool1", 1, //
                        "nodePool2", 2));
        fakeCluster.prepareMock(this.mockedApiClient);

        MachinePool machinePool = this.cloudPool.getMachinePool();
        assertThat(machinePool.getTimestamp(), is(UtcTime.now()));
        assertThat(machinePool.getMachines().size(), is(3));
        LOG.debug("machine pool: {}", JsonUtils.toPrettyString(JsonUtils.toJson(machinePool)));
    }

    /**
     * If no explicit desired size has been set, the first successful cluster
     * snapshot should set the initial desired size.
     */
    @Test
    public void determineDesiredSize() {
        GoogleContainerEngineCloudPoolConfig config = validConfig();
        this.cloudPool.configure(asJson(config));
        this.cloudPool.start();

        // no desired size should be set yet
        assertThat(this.cloudPool.desiredSize(), is(nullValue()));

        // prepare client to return a given cluster snapshot
        SimulatedCluster fakeCluster = new SimulatedCluster(config.getCluster(), //
                ImmutableMap.of("nodePool1", 4));
        fakeCluster.prepareMock(this.mockedApiClient);

        this.cloudPool.refreshClusterSnapshot();

        // desired size should now be initialized
        assertThat(this.cloudPool.desiredSize(), is(4));
    }

    /**
     * If an explicit desired size has been set, the first cluster snapshot
     * should <b>not</b> be used to set an initial desired size.
     */
    @Test
    public void explicitlySetDesiredSizeTakesPrecendenceOverDetermined() {
        GoogleContainerEngineCloudPoolConfig config = validConfig();
        this.cloudPool.configure(asJson(config));
        this.cloudPool.start();

        // prepare client to return a given cluster snapshot
        SimulatedCluster fakeCluster = new SimulatedCluster(config.getCluster(), //
                ImmutableMap.of("nodePool1", 4));
        fakeCluster.prepareMock(this.mockedApiClient);

        // explicitly set desired size
        this.cloudPool.setDesiredSize(0);
        assertThat(this.cloudPool.desiredSize(), is(0));

        // should not initialize desired size
        this.cloudPool.refreshClusterSnapshot();

        // desired size should still be what we originally set it to
        assertThat(this.cloudPool.desiredSize(), is(0));
    }

    /**
     * Verify that correct pool update calls are made to resize the cluster's
     * instance groups.
     */
    @Test
    public void updatePool() throws InterruptedException {
        // listens on the eventbus and waits for the resize operation to
        // complete
        AlertWaiter resizeAwaiter = new AlertWaiter(this.eventBus, a -> a.getTopic().equals(RESIZE.name()));

        GoogleContainerEngineCloudPoolConfig config = validConfig();
        this.cloudPool.configure(asJson(config));
        this.cloudPool.start();

        // prepare client to return a given cluster snapshot
        SimulatedCluster fakeCluster = new SimulatedCluster(config.getCluster(),
                ImmutableMap.of(//
                        "nodePool1", 4, //
                        "nodePool2", 4));
        fakeCluster.prepareMock(this.mockedApiClient);

        this.cloudPool.setDesiredSize(10);

        // wait for RESIZE event to be sent on event bus (signals completion)
        resizeAwaiter.await();

        // verify that expected API calls were made to resize the instance
        // groups
        String instanceGroup1Url = fakeCluster.instanceGroups().get(0).getSelfLink();
        String instanceGroup2Url = fakeCluster.instanceGroups().get(1).getSelfLink();
        verify(this.mockedApiClient.instanceGroup(instanceGroup1Url)).resize(5);
        verify(this.mockedApiClient.instanceGroup(instanceGroup2Url)).resize(5);
    }

    private static JsonObject validConfigAsJson() {
        return asJson(validConfig());
    }

    public static GoogleContainerEngineCloudPoolConfig validConfig() {
        JsonObject apiSettingsJson = JsonUtils.parseJsonResource("config/valid-service-account-key.json")
                .getAsJsonObject();
        CloudApiSettings apiSettings = new CloudApiSettings(null, apiSettingsJson);
        ContainerCluster cluster = new ContainerCluster("my-container-cluster", "my-project", "europe-west1-b");
        ScalingPolicy scalingPolicy = null;
        AlertersConfig alerts = null;
        TimeInterval poolUpdateInterval = null;
        GoogleContainerEngineCloudPoolConfig config = new GoogleContainerEngineCloudPoolConfig("my-project",
                apiSettings, cluster, scalingPolicy, alerts, poolUpdateInterval);
        return config;
    }

    public static JsonObject asJson(GoogleContainerEngineCloudPoolConfig config) {
        return JsonUtils.toJson(config).getAsJsonObject();
    }
}
