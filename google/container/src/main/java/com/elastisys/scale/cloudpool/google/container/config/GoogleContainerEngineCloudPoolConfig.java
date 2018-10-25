package com.elastisys.scale.cloudpool.google.container.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.cloudpool.google.commons.api.CloudApiSettings;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.multiplexing.AlertersConfig;

public class GoogleContainerEngineCloudPoolConfig {

    public static final ScalingPolicy DEFAULT_SCALING_POLICY = ScalingPolicy.Balanced;
    public static final TimeInterval DEFAULT_POOL_UPDATE_INTERVAL = TimeInterval.seconds(10);

    /** The name of the cloud pool. Required. */
    private final String name;

    /**
     * API access credentials and settings for the Google Cloud Platform.
     * Required.
     */
    private final CloudApiSettings cloudApiSettings;

    /** Specifies which container cluster to manage. Required. */
    private final ContainerCluster cluster;

    /**
     * The {@link ScalingPolicy} to use when changing the cluster size.
     * Optional. Default: {@link ScalingPolicy#Balanced}.
     */
    private final ScalingPolicy scalingPolicy;
    /**
     * Specifies how to send alerts on notable events. Optional. Default: don't
     * send alerts.
     */
    private final AlertersConfig alerts;

    /**
     * Specifies how often the cloud pool will refresh its cluster snapshot and
     * re-apply its desired size to the managed cluster. Optional. Default:
     * {@link #DEFAULT_POOL_UPDATE_INTERVAL}.
     */
    private final TimeInterval poolUpdateInterval;

    /**
     * Creates a {@link GoogleContainerEngineCloudPoolConfig}.
     *
     * @param name
     *            The name of the cloud pool. Required.
     * @param apiSettings
     *            API access credentials and settings for the Google Cloud
     *            Platform. Required.
     * @param cluster
     *            Specifies which container cluster to manage. Required.
     * @param scalingPolicy
     *            The {@link ScalingPolicy} to use when changing the cluster
     *            size. Optional. Default: {@link ScalingPolicy#Balanced}.
     * @param alerts
     *            Specifies how to send alerts on notable events. Optional.
     *            Default: don't send alerts.
     * @param poolUpdateInterval
     *            Specifies how often the cloud pool will refresh its cluster
     *            snapshot and re-apply its desired size to the managed cluster.
     *            Optional. Default: {@link #DEFAULT_POOL_UPDATE_INTERVAL}.
     */
    public GoogleContainerEngineCloudPoolConfig(String name, CloudApiSettings apiSettings, ContainerCluster cluster,
            ScalingPolicy scalingPolicy, AlertersConfig alerts, TimeInterval poolUpdateInterval) {
        this.name = name;
        this.cloudApiSettings = apiSettings;
        this.scalingPolicy = scalingPolicy;
        this.cluster = cluster;
        this.alerts = alerts;
        this.poolUpdateInterval = poolUpdateInterval;
    }

    /**
     * The name of the cloud pool.
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * API access credentials and settings for the Google Cloud Platform.
     *
     * @return
     */
    public CloudApiSettings getCloudApiSettings() {
        return this.cloudApiSettings;
    }

    /**
     * Specifies which container cluster to manage.
     *
     * @return
     */
    public ContainerCluster getCluster() {
        return this.cluster;
    }

    /**
     * The {@link ScalingPolicy} to use when changing the cluster size.
     *
     * @return
     */
    public ScalingPolicy getScalingPolicy() {
        return Optional.ofNullable(this.scalingPolicy).orElse(DEFAULT_SCALING_POLICY);
    }

    /**
     * Specifies how to send alerts on notable events.
     *
     * @return
     */
    public Optional<AlertersConfig> getAlerts() {
        return Optional.ofNullable(this.alerts);
    }

    /**
     * Specifies how often the cloud pool will refresh its cluster snapshot and
     * re-apply its desired size to the managed cluster
     *
     * @return
     */
    public TimeInterval getPoolUpdateInterval() {
        return Optional.ofNullable(this.poolUpdateInterval)
                .orElse(GoogleContainerEngineCloudPoolConfig.DEFAULT_POOL_UPDATE_INTERVAL);
    }

    public void validate() throws IllegalArgumentException {
        try {
            checkArgument(this.name != null, "missing name");
            checkArgument(this.cloudApiSettings != null, "missing cloudApiSettings");
            checkArgument(this.cluster != null, "missing cluster");
            this.cloudApiSettings.validate();
            this.cluster.validate();
            if (getAlerts().isPresent()) {
                getAlerts().get().validate();
            }

            getPoolUpdateInterval().validate();
            checkArgument(getPoolUpdateInterval().getSeconds() > 0, "poolUpdateInterval must be a positive duration");
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid cloudpool config: " + e.getMessage(), e);
        }

    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.cloudApiSettings, this.cluster, this.scalingPolicy, this.alerts,
                this.poolUpdateInterval);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GoogleContainerEngineCloudPoolConfig) {
            GoogleContainerEngineCloudPoolConfig that = (GoogleContainerEngineCloudPoolConfig) obj;
            return Objects.equals(this.name, that.name) //
                    && Objects.equals(this.cloudApiSettings, that.cloudApiSettings) //
                    && Objects.equals(this.cluster, that.cluster) //
                    && Objects.equals(this.scalingPolicy, that.scalingPolicy) //
                    && Objects.equals(this.alerts, that.alerts) //
                    && Objects.equals(this.poolUpdateInterval, that.poolUpdateInterval);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    /**
     * Returns a copy of this configuration with the {@link #cloudApiSettings}
     * field replaced with the given value.
     *
     * @param apiSettings
     * @return
     */
    public GoogleContainerEngineCloudPoolConfig withCloudApiSettings(CloudApiSettings apiSettings) {
        return new GoogleContainerEngineCloudPoolConfig(this.name, apiSettings, this.cluster, this.scalingPolicy,
                this.alerts, this.poolUpdateInterval);
    }

}
