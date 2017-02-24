package com.elastisys.scale.cloudpool.kubernetes.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.multiplexing.AlertersConfig;
import com.elastisys.scale.commons.net.url.UrlUtils;

/**
 * Carries configuration values for a {@link KubernetesCloudPool}. Declares
 * which Kubernetes API server to connect to (including authentication details)
 * and the
 * <a href="http://kubernetes.io/docs/user-guide/replication-controller/">
 * ReplicationController</a> whose size will be managed.
 *
 */
public class KubernetesCloudPoolConfig {
    /** Default update interval. */
    public static final TimeInterval DEFAULT_UPDATE_INTERVAL = new TimeInterval(10L, TimeUnit.SECONDS);

    /**
     * The base URL of the Kubernetes API server. For example,
     * {@code https://some.host:443}. Required.
     */
    private final String apiServerUrl;
    /** API authentication credentials. Required. */
    private final AuthConfig auth;
    /**
     * Configuration declaring the API construct (ReplicationController,
     * ReplicaSet, Deployment) whose pod set is to be scaled. Required.
     */
    private final PodPoolConfig podPool;

    /**
     * The time interval between periodical pool size updates. Optional. Default
     * is {@link #DEFAULT_UPDATE_INTERVAL}.
     */
    private final TimeInterval updateInterval;

    /** {@link Alert} settings. Optional. */
    private final AlertersConfig alerts;

    /**
     * Creates a {@link KubernetesCloudPoolConfig}.
     *
     * @param apiServerUrl
     *            The base URL of the Kubernetes API server. For example,
     *            {@code https://some.host:443}. Required.
     * @param auth
     *            API authentication credentials. Required.
     * @param podPool
     *            Configuration declaring the API construct
     *            (ReplicationController, ReplicaSet, Deployment) whose pod set
     *            is to be scaled. Required.
     * @param updateInterval
     *            The time interval between periodical pool size updates.
     *            Optional. Default is {@link #DEFAULT_UPDATE_INTERVAL}.
     * @param alerts
     *            {@link Alert} settings. Optional.
     */
    public KubernetesCloudPoolConfig(String apiServerUrl, AuthConfig auth, PodPoolConfig podPool,
            TimeInterval updateInterval, AlertersConfig alerts) {
        this.apiServerUrl = apiServerUrl;
        this.podPool = podPool;
        this.auth = auth;
        this.updateInterval = updateInterval;
        this.alerts = alerts;
    }

    /**
     * The base URL of the Kubernetes API server. For example,
     * {@code https://some.host:443}.
     *
     * @return
     */
    public String getApiServerUrl() {
        return this.apiServerUrl;
    }

    /**
     * API authentication credentials.
     *
     * @return
     */
    public AuthConfig getAuth() {
        return this.auth;
    }

    /**
     * Configuration declaring the API construct (ReplicationController,
     * ReplicaSet, Deployment) whose pod set is to be scaled.
     *
     * @return
     */
    public PodPoolConfig getPodPool() {
        return this.podPool;
    }

    /**
     * The time interval between periodical pool size updates.
     *
     * @return
     */
    public TimeInterval getUpdateInterval() {
        return Optional.ofNullable(this.updateInterval).orElse(DEFAULT_UPDATE_INTERVAL);
    }

    /**
     * Returns {@link Alert} settings. May be <code>null</code>.
     *
     * @return
     */
    public AlertersConfig getAlerts() {
        return this.alerts;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.apiServerUrl, this.auth, this.podPool, this.updateInterval, this.alerts);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof KubernetesCloudPoolConfig) {
            KubernetesCloudPoolConfig that = (KubernetesCloudPoolConfig) obj;
            return Objects.equals(this.apiServerUrl, that.apiServerUrl) //
                    && Objects.equals(this.auth, that.auth) //
                    && Objects.equals(this.podPool, that.podPool) //
                    && Objects.equals(this.updateInterval, that.updateInterval) //
                    && Objects.equals(this.alerts, that.alerts);
        }
        return false;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.apiServerUrl != null, "config: no apiServerUrl given");
        checkArgument(this.auth != null, "config: no auth given");
        checkArgument(this.podPool != null, "config: no podPool given");

        // verify that apiServerUrl is a URL
        try {
            UrlUtils.url(this.apiServerUrl);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("config: apiServerUrl: %s: %s", this.apiServerUrl, e.getMessage()), e);
        }

        try {
            this.auth.validate();
            this.podPool.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("config: " + e.getMessage(), e);
        }

        checkArgument(getUpdateInterval().getSeconds() > 0, "config: updateInterval must be a positive duration");
        try {
            getUpdateInterval().validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("config: updateInterval: " + e.getMessage(), e);
        }

        if (this.alerts != null) {
            try {
                this.alerts.validate();
            } catch (Exception e) {
                throw new IllegalArgumentException("config: alerts: " + e.getMessage(), e);
            }
        }
    }
}
