package com.elastisys.scale.cloudpool.google.container;

import static com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics.RESIZE;
import static com.elastisys.scale.commons.net.alerter.AlertSeverity.ERROR;
import static com.elastisys.scale.commons.net.alerter.AlertSeverity.INFO;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.google.commons.api.CloudApiSettings;
import com.elastisys.scale.cloudpool.google.commons.errors.GceException;
import com.elastisys.scale.cloudpool.google.container.client.ContainerClusterClient;
import com.elastisys.scale.cloudpool.google.container.config.ContainerCluster;
import com.elastisys.scale.cloudpool.google.container.config.GoogleContainerEngineCloudPoolConfig;
import com.elastisys.scale.cloudpool.google.container.config.ScalingPolicy;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertBuilder;
import com.elastisys.scale.commons.net.alerter.multiplexing.AlertersConfig;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

/**
 * Exercises the {@link Alert}ing capabilities of the
 * {@link GoogleContainerEngineCloudPool}.
 */
public class TestGoogleContainerEngineCloudPoolEventing {
    private static final Logger LOG = LoggerFactory.getLogger(TestGoogleContainerEngineCloudPoolEventing.class);

    /** Executor used by cloud pool to schedule tasks. */
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);

    // Mock objects
    private ContainerClusterClient mockedApiClient;
    private EventBus mockedEventBus = mock(EventBus.class);

    /** Object under test. */
    private GoogleContainerEngineCloudPool cloudPool;

    @Before
    public void beforeTestMethod() {
        FrozenTime.setFixed(UtcTime.parse("2017-01-01T12:00:00.000Z"));

        this.executor.setRemoveOnCancelPolicy(true);
        this.mockedApiClient = mock(ContainerClusterClient.class);
        this.cloudPool = new GoogleContainerEngineCloudPool(this.mockedApiClient, this.mockedEventBus, this.executor);

        GoogleContainerEngineCloudPoolConfig config = validConfig();
        this.cloudPool.configure(asJson(config));
        this.cloudPool.start();
    }

    /**
     * A RESIZE {@link Alert} should be pushed on the {@link EventBus} (for
     * later dispatch to configured alert receivers) after a successful resize
     * operation.
     */
    @Test
    public void eventOnSuccessfulResize() {
        GoogleContainerEngineCloudPoolConfig config = this.cloudPool.config();

        // prepare client to return a given cluster snapshot
        SimulatedCluster fakeCluster = new SimulatedCluster(config.getCluster(), Maps.of("nodePool1", 2));
        fakeCluster.prepareMock(this.mockedApiClient);

        // hack to set desiredSize without forking off updateCluster in a
        // separate thread
        setDesiredSizeWithoutUpdateCluster(this.cloudPool, 3);
        this.cloudPool.updateCluster();

        // verify that expected API calls were made to resize the instance
        // groups
        String instanceGroup1Url = fakeCluster.instanceGroups().get(0).getSelfLink();
        verify(this.mockedApiClient.instanceGroup(instanceGroup1Url)).resize(3);

        // verify
        verify(this.mockedEventBus).post(resizeEvent(2, 3));
    }

    /**
     * A RESIZE failure {@link Alert} should be pushed on the {@link EventBus}
     * (for later dispatch to configured alert receivers) after a failed resize
     * operation.
     */
    @Test
    public void eventOnFailedResize() {
        GoogleContainerEngineCloudPoolConfig config = this.cloudPool.config();

        // prepare client to return a given cluster snapshot
        SimulatedCluster fakeCluster = new SimulatedCluster(config.getCluster(), Maps.of("nodePool1", 2));
        fakeCluster.prepareMock(this.mockedApiClient);
        String groupUrl = fakeCluster.instanceGroups().get(0).getSelfLink();

        when(this.mockedApiClient.instanceGroup(groupUrl).resize(anyInt())).thenThrow(new GceException("API error"));

        // hack to set desiredSize without forking off updateCluster in a
        // separate thread
        setDesiredSizeWithoutUpdateCluster(this.cloudPool, 3);

        this.cloudPool.updateCluster();

        // verify expected API calls
        verify(this.mockedApiClient.instanceGroup(groupUrl)).resize(3);

        // verify
        verify(this.mockedEventBus).post(resizeFailedEvent("API error"));
    }

    private Alert resizeEvent(int oldSize, int newSize) {
        return AlertBuilder.create().topic(RESIZE.name()).severity(INFO)
                .message(String.format("container cluster size updated: %d -> %d", oldSize, newSize)).build();
    }

    private Alert resizeFailedEvent(String expectedMessage) {
        return AlertBuilder.create().topic(RESIZE.name()).severity(ERROR)
                .message(String.format("failed to update cluster size: %s", expectedMessage)).build();
    }

    /**
     * Use reflection to set the private {@code desiredSize} field. We only do
     * this in the test, instead of calling {@code setDesiredSize()}, to prevent
     * {@code updateCluster} from being started in a background thread.
     *
     * @param cloudPool
     * @param desiredSize
     * @throws RuntimeException
     */
    private static void setDesiredSizeWithoutUpdateCluster(GoogleContainerEngineCloudPool cloudPool,
            Integer desiredSize) throws RuntimeException {
        try {
            Field desiredSizeField = GoogleContainerEngineCloudPool.class.getDeclaredField("desiredSize");
            desiredSizeField.setAccessible(true);
            desiredSizeField.set(cloudPool, desiredSize);
        } catch (Exception e) {
            throw new RuntimeException("failed to set desiredSize field: " + e.getMessage(), e);
        }
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
