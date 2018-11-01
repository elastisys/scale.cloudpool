package com.elastisys.scale.cloudpool.kubernetes.apiserver;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Captures connection and authentication credentials for an
 * {@link ApiServerClient}.
 */
public class ClientConfig {

    /**
     * The base URL of the Kubernetes API server. For example,
     * {@code https://some.host:443}.
     */
    private final String apiServerUrl;
    /** Client auth credentials. */
    private final ClientCredentials credentials;

    /**
     * @param apiServerUrl
     *            The base URL of the Kubernetes API server. For example,
     *            {@code https://some.host:443}.
     * @param credentials
     *            Client auth credentials.
     */
    public ClientConfig(String apiServerUrl, ClientCredentials credentials) {
        checkArgument(apiServerUrl != null, "ClientConfig: apiServerUrl cannot be null");
        checkArgument(credentials != null, "ClientConfig: credentials cannot be null");
        this.apiServerUrl = apiServerUrl;
        this.credentials = credentials;
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
     * Client auth credentials.
     *
     * @return
     */
    public ClientCredentials getCredentials() {
        return this.credentials;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.apiServerUrl, this.credentials);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClientConfig) {
            ClientConfig that = (ClientConfig) obj;
            return Objects.equals(this.apiServerUrl, that.apiServerUrl) //
                    && Objects.equals(this.credentials, that.credentials);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
