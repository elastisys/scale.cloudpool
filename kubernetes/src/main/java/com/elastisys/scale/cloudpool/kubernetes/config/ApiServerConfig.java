package com.elastisys.scale.cloudpool.kubernetes.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;
import com.google.common.collect.Range;

/**
 * Declares a Kubernetes apiserver to be used by a {@link KubernetesCloudPool}.
 * As explained in the <a href=
 * "http://kubernetes.io/docs/user-guide/accessing-the-cluster/#accessing-the-api-from-a-pod">
 * Kubernetes documentation</a>, the preferred approach when running the
 * {@link KubernetesCloudPool} inside a Kubernetes pod is to use
 * {@code kubernetes} as the host name, since it is always resolvable by pods.
 *
 * @see KubernetesCloudPoolConfig
 */
public class ApiServerConfig {
    /** Default Kubernetes apiserver port. */
    public static final int DEFAULT_PORT = 443;
    /** Default Kubernetes apiserver host. */
    public static final String DEFAULT_HOST = "kubernetes";

    /**
     * The Kubernetes apiserver host/IP address. Use {@code kubernetes} when
     * running the {@link KubernetesCloudPool} inside a pod.
     */
    private final String host;

    /**
     * The port on which the apiserver is serving HTTPS traffic. Optional.
     * Default is {@code 443}.
     */
    private final Integer port;

    /**
     * Creates an {@link ApiServerConfig}
     *
     * @param host
     * @param port
     */
    public ApiServerConfig(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    /**
     * The Kubernetes apiserver host/IP address. Use {@code kubernetes} when
     * running the {@link KubernetesCloudPool} inside a pod.
     *
     * @return
     */
    public String getHost() {
        return Optional.ofNullable(this.host).orElse(DEFAULT_HOST);
    }

    /**
     * The port on which the apiserver is serving HTTPS traffic.
     *
     * @return
     */
    public Integer getPort() {
        return Optional.ofNullable(this.port).orElse(DEFAULT_PORT);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHost(), getPort());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ApiServerConfig) {
            ApiServerConfig that = (ApiServerConfig) obj;
            return Objects.equals(getHost(), that.getHost()) && Objects.equals(getPort(), that.getPort());
        }
        return super.equals(obj);
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.host != null, "apiServer: missing host");
        checkArgument(Range.closed(1, 65535).contains(getPort()), "apiServer: port not in legal port range [1, 65535]");
    }
}
