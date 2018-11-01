package com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ClientCredentials;
import com.elastisys.scale.commons.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Connection data for a Kubernetes cluster.
 *
 * See https://godoc.org/k8s.io/client-go/tools/clientcmd/api#Cluster
 */
public class Cluster {
    /** The fully qualified url for the kubernetes apiserver. */
    @JsonProperty("server")
    String server;

    /**
     * A path to a PEM-encoded cert file for the certificate authority. May be
     * null.
     */
    @JsonProperty("certificate-authority")
    String certificateAuthorityPath;

    /**
     * A base64-encoded certificate authority certificate (PEM-encoded). May be
     * null.
     */
    @JsonProperty("certificate-authority-data")
    String certificateAuthorityData;

    /**
     * If <code>true</code>, skip the validity check for the server's
     * certificate.
     */
    @JsonProperty("insecure-skip-tls-verify")
    Boolean insecureSkipTlsVerify;

    public Cluster() {
    }

    /**
     * The fully qualified url for the kubernetes apiserver.
     *
     * @return
     */
    public String getServer() {
        return this.server;
    }

    /**
     * The path to a cert file for the certificate authority.
     *
     * @return
     */
    public String getCertificateAuthorityPath() {
        return this.certificateAuthorityPath;
    }

    /**
     * The base64-encoded certificate authority certificate (PEM-encoded).
     *
     * @return
     */
    public String getCertificateAuthorityData() {
        return this.certificateAuthorityData;
    }

    public boolean hasCertificate() {
        return this.certificateAuthorityData != null || this.certificateAuthorityPath != null;
    }

    /**
     * If <code>true</code>, skip the validity check for the server's
     * certificate.
     *
     * @return
     */
    public boolean getInsecureSkipTlsVerify() {
        return Optional.ofNullable(this.insecureSkipTlsVerify).orElse(false);
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.server != null, "cluster: no server url specified");

        if (this.certificateAuthorityData != null) {
            checkArgument(this.certificateAuthorityPath == null,
                    "cluster: ambiguous input: both certificate-authority-data and certificate-authority specified");
            checkArgument(!getInsecureSkipTlsVerify(),
                    "cluster: ambiguous input: both certificate-authority-data and insecure-skip-tls-verify specified");
        }

        if (this.certificateAuthorityPath != null) {
            checkArgument(this.certificateAuthorityData == null,
                    "cluster: ambiguous input: both certificate-authority and certificate-authority-data specified");
            checkArgument(!getInsecureSkipTlsVerify(),
                    "cluster: ambiguous input: both certificate-authority and insecure-skip-tls-verify specified");
        }

        if (getInsecureSkipTlsVerify()) {
            checkArgument(this.certificateAuthorityPath == null,
                    "cluster: ambiguous input: both insecure-skip-tls-verify and certificate-authority specified");
            checkArgument(this.certificateAuthorityData == null,
                    "cluster: ambiguous input: both insecure-skip-tls-verify and certificate-authority-data specified");
        }

        if (hasCertificate()) {
            try {
                ensureCertLoadable();
            } catch (Exception e) {
                throw new IllegalArgumentException("cluster: failed to load ca cert: " + e.getMessage(), e);
            }
        }
    }

    private void ensureCertLoadable() throws Exception {
        if (this.certificateAuthorityData != null) {
            ClientCredentials.loadCertData(this.certificateAuthorityData);
        }
        if (this.certificateAuthorityPath != null) {
            ClientCredentials.loadCertPath(this.certificateAuthorityPath);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.server, this.certificateAuthorityPath, this.certificateAuthorityData,
                this.insecureSkipTlsVerify);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Cluster) {
            Cluster that = (Cluster) obj;
            return Objects.equals(this.server, that.server) //
                    && Objects.equals(this.certificateAuthorityPath, that.certificateAuthorityPath) //
                    && Objects.equals(this.certificateAuthorityData, that.certificateAuthorityData) //
                    && Objects.equals(getInsecureSkipTlsVerify(), that.getInsecureSkipTlsVerify());
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
