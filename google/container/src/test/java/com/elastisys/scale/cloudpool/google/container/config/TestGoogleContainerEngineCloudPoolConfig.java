package com.elastisys.scale.cloudpool.google.container.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.elastisys.scale.cloudpool.google.commons.api.CloudApiSettings;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.multiplexing.AlertersConfig;

/**
 * Exercises the {@link GoogleContainerEngineCloudPoolConfig} config class.
 */
public class TestGoogleContainerEngineCloudPoolConfig {

    private static final String NAME = "gke-cloudpool";
    private static final CloudApiSettings API_SETTINGS = new CloudApiSettings(null,
            JsonUtils.parseJsonResource("config/valid-service-account-key.json").getAsJsonObject());
    private static final ContainerCluster CLUSTER = new ContainerCluster("my-container-cluster", "my-project",
            "europe-west-1b");
    private static final ScalingPolicy SCALING_POLICY = ScalingPolicy.Balanced;
    private static final AlertersConfig ALERTS = new AlertersConfig(Collections.emptyList(), Collections.emptyList());
    private static final TimeInterval UPDATE_INTERVAL = TimeInterval.seconds(10);

    /**
     * Should be possible to give explicit values for all fields.
     */
    @Test
    public void complete() {
        GoogleContainerEngineCloudPoolConfig config = new GoogleContainerEngineCloudPoolConfig(NAME, API_SETTINGS,
                CLUSTER, SCALING_POLICY, ALERTS, UPDATE_INTERVAL);
        config.validate();

        assertThat(config.getName(), is(NAME));
        assertThat(config.getCloudApiSettings(), is(API_SETTINGS));
        assertThat(config.getCluster(), is(CLUSTER));
        assertThat(config.getScalingPolicy(), is(SCALING_POLICY));
        assertThat(config.getAlerts().isPresent(), is(true));
        assertThat(config.getAlerts().get(), is(ALERTS));
        assertThat(config.getPoolUpdateInterval(), is(UPDATE_INTERVAL));
    }

    /**
     * Fields scalingPolicy, alerts, and poolUpdateInterval are optional.
     */
    @Test
    public void defaults() {
        ScalingPolicy nullScalingPolicy = null;
        AlertersConfig nullAlerts = null;
        TimeInterval nullUpdateInterval = null;
        GoogleContainerEngineCloudPoolConfig config = new GoogleContainerEngineCloudPoolConfig(NAME, API_SETTINGS,
                CLUSTER, nullScalingPolicy, nullAlerts, nullUpdateInterval);
        config.validate();

        assertThat(config.getScalingPolicy(), is(GoogleContainerEngineCloudPoolConfig.DEFAULT_SCALING_POLICY));
        assertThat(config.getAlerts().isPresent(), is(false));
        assertThat(config.getPoolUpdateInterval(),
                is(GoogleContainerEngineCloudPoolConfig.DEFAULT_POOL_UPDATE_INTERVAL));
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroPoolUpdateInterval() {
        TimeInterval zeroInterval = TimeInterval.seconds(0);
        new GoogleContainerEngineCloudPoolConfig(NAME, API_SETTINGS, CLUSTER, SCALING_POLICY, ALERTS, zeroInterval)
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativePoolUpdateInterval() {
        TimeInterval negativeInterval = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"seconds\"}"), TimeInterval.class);
        new GoogleContainerEngineCloudPoolConfig(NAME, API_SETTINGS, CLUSTER, SCALING_POLICY, ALERTS, negativeInterval)
                .validate();
    }

    /**
     * Validation should be propagated to alerts.
     */
    @Test(expected = IllegalArgumentException.class)
    public void invalidAlerts() {
        new GoogleContainerEngineCloudPoolConfig(NAME, API_SETTINGS, CLUSTER, SCALING_POLICY, invalidAlertsConfig(),
                UPDATE_INTERVAL).validate();
    }

    /**
     * Validation should be propagated to cluster config.
     */
    @Test(expected = IllegalArgumentException.class)
    public void invalidClusterConfig() {
        new GoogleContainerEngineCloudPoolConfig(NAME, API_SETTINGS, invalidCluster(), SCALING_POLICY, ALERTS,
                UPDATE_INTERVAL).validate();
    }

    private ContainerCluster invalidCluster() {
        // cluster missing project
        return new ContainerCluster(NAME, null, "europe-west-1b");
    }

    private AlertersConfig invalidAlertsConfig() {
        HttpAlerterConfig illegalHttpAlerter = JsonUtils.toObject(
                JsonUtils.parseJsonString("{\"severityFilter\": \"ERROR\", \"destinationUrls\": null}"),
                HttpAlerterConfig.class);
        return new AlertersConfig(null, Arrays.asList(illegalHttpAlerter));
    }

}
