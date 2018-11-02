package com.elastisys.scale.cloudpool.kubernetes.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Triple;

import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.ClientConfig;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.ClientCredentials;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.ClientCredentials.Builder;
import com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig.Cluster;
import com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig.KubeConfig;
import com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig.User;
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
     * A path to a kubeconfig file, from which Kubernetes client settings will
     * be loaded. This option is mutually exclusive with {@link #apiServerUrl}
     * and {@link #auth}. Note that the kubeconfig file must specify a
     * {@code current-context}.
     */
    private final String kubeConfigPath;
    /**
     * The base URL of the Kubernetes API server. For example,
     * {@code https://some.host:443}. This option is mutually exclusive with
     * {@link #kubeConfigPath}.
     */
    private final String apiServerUrl;
    /**
     * API authentication credentials. This option is mutually exclusive with
     * {@link #kubeConfigPath}.
     */
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
     * @param kubeConfigPath
     *            A path to a kubeconfig file, from which Kubernetes client
     *            settings will be loaded. This option is mutually exclusive
     *            with {@link #apiServerUrl} and {@link #auth}. Note that the
     *            kubeconfig file must specify a {@code current-context}.
     * @param apiServerUrl
     *            The base URL of the Kubernetes API server. For example,
     *            {@code https://some.host:443}. This option is mutually
     *            exclusive with {@link #kubeConfigPath}.
     * @param auth
     *            API authentication credentials. This option is mutually
     *            exclusive with {@link #kubeConfigPath}.
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
    public KubernetesCloudPoolConfig(String kubeConfigPath, String apiServerUrl, AuthConfig auth, PodPoolConfig podPool,
            TimeInterval updateInterval, AlertersConfig alerts) {
        this.kubeConfigPath = kubeConfigPath;
        this.apiServerUrl = apiServerUrl;
        this.podPool = podPool;
        this.auth = auth;
        this.updateInterval = updateInterval;
        this.alerts = alerts;
    }

    public ClientConfig getClientConfig() throws IllegalArgumentException {
        validate();

        try {
            // Assume that the config is validated. Hence, either kubeConfigPath
            // or apiServerUrl+auth should have been specified.
            if (this.kubeConfigPath != null) {
                return clientConfigFromKubeConfig(this.kubeConfigPath);
            } else {
                return new ClientConfig(this.apiServerUrl, credentialsFromAuth(this.auth));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "failed to extract a client configuration from cloudpool config: " + e.getMessage(), e);
        }
    }

    private ClientCredentials credentialsFromAuth(AuthConfig authConfig) {
        Builder builder = ClientCredentials.builder();
        builder.certData(authConfig.getClientCert());
        builder.certPath(authConfig.getClientCertPath());
        builder.keyData(authConfig.getClientKey());
        builder.keyPath(authConfig.getClientKeyPath());
        builder.tokenData(authConfig.getClientToken());
        builder.tokenPath(authConfig.getClientTokenPath());
        builder.serverCertData(authConfig.getServerCert());
        builder.serverCertPath(authConfig.getServerCertPath());
        return builder.build();
    }

    private ClientConfig clientConfigFromKubeConfig(String kubeconfigPath) throws IOException {
        Builder builder = ClientCredentials.builder();
        KubeConfig kubeConfig = KubeConfig.load(new File(kubeconfigPath));
        Triple<Cluster, User, String> context = kubeConfig.loadCurrentContext();
        Cluster cluster = context.getLeft();
        User user = context.getMiddle();
        if (!cluster.getInsecureSkipTlsVerify()) {
            builder.serverCertPath(cluster.getCertificateAuthorityPath());
            builder.serverCertData(cluster.getCertificateAuthorityData());
        }

        builder.certData(user.getClientCertificateData());
        builder.certPath(user.getClientCertificatePath());
        builder.keyData(user.getClientKeyData());
        builder.keyPath(user.getClientKeyPath());
        builder.tokenData(user.getTokenData());
        builder.tokenPath(user.getTokenPath());
        builder.username(user.getUsername());
        builder.password(user.getPassword());
        return new ClientConfig(cluster.getServer(), builder.build());
    }

    /**
     * A path to a kubeconfig file, from which Kubernetes client settings will
     * be loaded. This option is mutually exclusive with {@link #apiServerUrl}
     * and {@link #auth}. Note that the kubeconfig file must specify a
     * {@code current-context}.
     *
     * @return
     */
    public String getKubeConfigPath() {
        return this.kubeConfigPath;
    }

    /**
     * The base URL of the Kubernetes API server. For example,
     * {@code https://some.host:443}. This option is mutually exclusive with
     * {@link #kubeConfigPath}.
     *
     * @return
     */
    public String getApiServerUrl() {
        return this.apiServerUrl;
    }

    /**
     * API authentication credentials. This option is mutually exclusive with
     * {@link #kubeConfigPath}.
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
        return Objects.hash(this.kubeConfigPath, this.apiServerUrl, this.auth, this.podPool, this.updateInterval,
                this.alerts);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof KubernetesCloudPoolConfig) {
            KubernetesCloudPoolConfig that = (KubernetesCloudPoolConfig) obj;
            return Objects.equals(this.kubeConfigPath, that.kubeConfigPath)
                    && Objects.equals(this.apiServerUrl, that.apiServerUrl) //
                    && Objects.equals(this.auth, that.auth) //
                    && Objects.equals(this.podPool, that.podPool) //
                    && Objects.equals(this.updateInterval, that.updateInterval) //
                    && Objects.equals(this.alerts, that.alerts);
        }
        return false;
    }

    public void validate() throws IllegalArgumentException {

        if (this.kubeConfigPath != null) {
            // connection/auth settings specified via kubeConfigPath
            checkArgument(this.apiServerUrl == null, "config: kubeConfigPath is mutually exclusive with apiServerUrl");
            checkArgument(this.auth == null, "config: kubeConfigPath is mutually exclusive with auth");
            try {
                KubeConfig.load(new File(this.kubeConfigPath)).validate();
            } catch (Exception e) {
                throw new IllegalArgumentException("config: kubeConfig: " + e.getMessage(), e);
            }
        } else {
            // connection/auth settings specified via apiServerUrl and auth
            checkArgument(this.apiServerUrl != null, "config: neither kubeConfigPath nor apiServerUrl given");
            checkArgument(this.auth != null, "config: apiServerUrl given but no auth specified");
            // verify that apiServerUrl is a URL
            try {
                UrlUtils.url(this.apiServerUrl);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        String.format("config: apiServerUrl: %s: %s", this.apiServerUrl, e.getMessage()), e);
            }
            try {
                this.auth.validate();
            } catch (Exception e) {
                throw new IllegalArgumentException("config: " + e.getMessage(), e);
            }
        }

        checkArgument(this.podPool != null, "config: no podPool given");
        try {
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
