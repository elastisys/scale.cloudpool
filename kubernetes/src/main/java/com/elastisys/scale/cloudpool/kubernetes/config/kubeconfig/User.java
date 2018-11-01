package com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ClientCredentials;
import com.elastisys.scale.commons.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents authentication credentials for a user in a {@link KubeConfig}.
 *
 * See https://godoc.org/k8s.io/client-go/tools/clientcmd/api#AuthInfo
 */
public class User {
    /** The path to a (PEM-encoded) client cert file for TLS. */
    @JsonProperty("client-certificate")
    String clientCertificatePath;
    /**
     * Base64-encoded client cert (PEM-encoded) for TLS. Overrides
     * {@code client-certificate}.
     */
    @JsonProperty("client-certificate-data")
    String clientCertificateData;

    /** The path to a (PEM-encoded) client key file for TLS. */
    @JsonProperty("client-key")
    String clientKeyPath;
    /**
     * Base64-encoded client cert (PEM-encoded) for TLS. Overrides client-key.
     */
    @JsonProperty("client-key-data")
    String clientKeyData;

    /**
     * A file that contains a bearer token. The fields username/password and
     * token are mutually exclusive.
     */
    @JsonProperty("token-file")
    String tokenPath;
    /**
     * The bearer token for authentication to the kubernetes cluster. Overrides
     * token-file. The fields username/password and token are mutually
     * exclusive.
     */
    @JsonProperty("token")
    String tokenData;

    /**
     * The username for basic authentication to the kubernetes cluster. The
     * fields username/password and token are mutually exclusive.
     */
    @JsonProperty("username")
    String username;
    /**
     * The password for basic authentication to the kubernetes cluster. The
     * fields username/password and token are mutually exclusive.
     */
    @JsonProperty("password")
    String password;

    /**
     * The path to a (PEM-encoded) client cert file for TLS.
     *
     * @return
     */
    public String getClientCertificatePath() {
        return this.clientCertificatePath;
    }

    /**
     * Base64-encoded client cert (PEM-encoded) for TLS. Overrides
     * {@code client-certificate}.
     *
     * @return
     */
    public String getClientCertificateData() {
        return this.clientCertificateData;
    }

    /**
     * <code>true</code> if a client cert was given (either by path or value).
     *
     * @return
     */
    public boolean hasClientCert() {
        return this.clientCertificatePath != null || this.clientCertificateData != null;
    }

    /**
     * The path to a (PEM-encoded) client key file for TLS.
     *
     * @return
     */
    public String getClientKeyPath() {
        return this.clientKeyPath;
    }

    /**
     * Base64-encoded client cert (PEM-encoded) for TLS. Overrides client-key.
     *
     * @return
     */
    public String getClientKeyData() {
        return this.clientKeyData;
    }

    /**
     * <code>true</code> if a client key was given (either by path or value).
     *
     * @return
     */
    public boolean hasClientKey() {
        return this.clientKeyPath != null || this.clientKeyData != null;
    }

    /**
     * A file that contains a bearer token. The fields username/password and
     * token are mutually exclusive.
     *
     * @return
     */
    public String getTokenPath() {
        return this.tokenPath;
    }

    /**
     * The bearer token for authentication to the kubernetes cluster. Overrides
     * token-file. The fields username/password and token are mutually
     * exclusive.
     *
     * @return
     */
    public String getTokenData() {
        return this.tokenData;
    }

    /**
     * <code>true</code> if a bearer token was given (either by path or value).
     *
     * @return
     */
    public boolean hasToken() {
        return this.tokenPath != null || this.tokenData != null;
    }

    /**
     * The username for basic authentication to the kubernetes cluster. The
     * fields username/password and token are mutually exclusive.
     *
     * @return
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * The password for basic authentication to the kubernetes cluster. The
     * fields username/password and token are mutually exclusive.
     *
     * @return
     */
    public String getPassword() {
        return this.password;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.clientCertificatePath, this.clientCertificateData, this.clientKeyPath,
                this.clientKeyData, this.tokenPath, this.tokenData, this.username, this.password);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User) {
            User that = (User) obj;
            return Objects.equals(this.clientCertificatePath, that.clientCertificatePath) //
                    && Objects.equals(this.clientCertificateData, that.clientCertificateData) //
                    && Objects.equals(this.clientKeyPath, that.clientKeyPath) //
                    && Objects.equals(this.clientKeyData, that.clientKeyData) //
                    && Objects.equals(this.tokenPath, that.tokenPath) //
                    && Objects.equals(this.tokenData, that.tokenData) //
                    && Objects.equals(this.username, that.username) //
                    && Objects.equals(this.password, that.password);
        }
        return false;
    }

    public boolean hasCertAuth() {
        return hasClientCert() && hasClientKey();
    }

    public boolean hasTokenAuth() {
        return hasToken();
    }

    public boolean hasBasicAuth() {
        return this.username != null && this.password != null;
    }

    public void validate() throws IllegalArgumentException {
        if (hasClientCert()) {
            checkArgument(hasClientKey(), "user: cannot specify a client cert without a client key");
        }
        if (hasClientKey()) {
            checkArgument(hasClientCert(), "user: cannot specify a client key without a client cert");
        }
        if (this.username != null) {
            checkArgument(this.password != null, "user: cannot specify a username without a password");
        }
        if (this.password != null) {
            checkArgument(this.username != null, "user: cannot specify a password without a username");
        }
        checkArgument(hasCertAuth() || hasTokenAuth() || hasBasicAuth(),
                "user: neither cert-auth, token-auth nor basic auth specified");

        // not sure why this is the case but it is suggested here:
        // https://github.com/eBay/Kubernetes/blob/master/docs/user-guide/kubeconfig-file.md
        checkArgument(!(hasTokenAuth() && hasBasicAuth()), "user: token-auth and basic-auth are mutually exclusive");

        if (hasCertAuth()) {
            try {
                ensureCertLoadable();
            } catch (Exception e) {
                throw new IllegalArgumentException("user: failed to load cert: " + e.getMessage(), e);
            }

            try {
                ensureKeyLoadable();
            } catch (Exception e) {
                throw new IllegalArgumentException("user: failed to load key: " + e.getMessage(), e);
            }
        }

    }

    private void ensureKeyLoadable() throws Exception {
        if (this.clientKeyData != null) {
            ClientCredentials.loadKeyData(this.clientKeyData);
        }
        if (this.clientKeyPath != null) {
            ClientCredentials.loadKeyPath(this.clientKeyPath);
        }
    }

    private void ensureCertLoadable() throws Exception {
        if (this.clientCertificateData != null) {
            ClientCredentials.loadCertData(this.clientCertificateData);
        }
        if (this.clientCertificatePath != null) {
            ClientCredentials.loadCertPath(this.clientCertificatePath);
        }
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
