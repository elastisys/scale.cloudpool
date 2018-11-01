package com.elastisys.scale.cloudpool.kubernetes.apiserver;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.security.pem.PemUtils;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.elastisys.scale.commons.util.io.IoUtils;

/**
 * Holds authentication credentials for an {@link ApiServerClient}. Supports one
 * or more of cert-based authentication, token-based authentication, or basic
 * authentication.
 *
 */
public class ClientCredentials {
    /** Bearer token for token-based client authentication. May be null. */
    private final String token;

    /** Certificate for cert-based client authentication. May be null. */
    private final Certificate cert;
    /** Private key for cert-based client authentication. May be null. */
    private final PrivateKey key;

    /** Username/password for basic client authentication. May be null. */
    private final BasicCredentials basicAuth;

    /**
     * The server/CA certificate to use to validate the server or null if no
     * server cert verification is to be carried out.
     */
    private final Certificate serverCert;

    /**
     * Kept private. Use {@link Builder} to create instances.
     */
    private ClientCredentials(String token, Certificate cert, PrivateKey key, BasicCredentials basicAuth,
            Certificate serverCert) {
        this.token = token;
        this.cert = cert;
        this.key = key;
        this.basicAuth = basicAuth;
        this.serverCert = serverCert;
    }

    /**
     * Create a {@link Builder} for constructing {@link ClientCredentials}
     * objects.
     *
     * @return
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the base64-encoded client auth token to be used, or
     * <code>null</code> if none was given.
     *
     * @return
     */
    public String getToken() {
        return this.token;
    }

    /**
     * <code>true</code> if token-based authentication was specified.
     *
     * @return
     */
    public boolean hasToken() {
        return this.token != null;
    }

    /**
     * Returns the client certificate to use for authentication, or
     * <code>null</code> if none was given.
     *
     * @return
     */
    public Certificate getCert() {
        return this.cert;
    }

    /**
     * <code>true</code> if cert-base authentication was specified. This implies
     * has
     *
     * @return
     */
    public boolean hasCert() {
        return this.cert != null;
    }

    /**
     * Returns the client key to use for authentication, or <code>null</code> if
     * none was given.
     *
     * @return
     */
    public PrivateKey getKey() {
        return this.key;
    }

    /**
     * <code>true</code> if cert-based authentication was specified.
     *
     * @return
     */
    public boolean hasKey() {
        return this.key != null;
    }

    /**
     * The server/CA certificate to use to validate the server or null if no
     * server cert verification is to be carried out.
     *
     * @return
     */
    public Certificate getServerCert() {
        return this.serverCert;
    }

    /**
     * <code>true</code> if the server certificate is to be verified.
     *
     * @return
     */
    public boolean hasServerCert() {
        return this.serverCert != null;
    }

    /**
     * Returns username/password credentials to use for authentication or null
     * if none are to be used.
     *
     * @return
     */
    public BasicCredentials getBasicAuth() {
        return this.basicAuth;
    }

    /**
     * <code>true</code> if basic authentication is to be carried out.
     *
     * @return
     */
    public boolean hasBasicAuth() {
        return this.basicAuth != null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.token, this.token, this.cert, this.key, this.basicAuth, this.serverCert);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClientCredentials) {
            ClientCredentials that = (ClientCredentials) obj;
            return Objects.equals(this.token, that.token) //
                    && Objects.equals(this.cert, that.cert) //
                    && Objects.equals(this.key, that.key) //
                    && Objects.equals(this.basicAuth, that.basicAuth)
                    && Objects.equals(this.serverCert, that.serverCert);

        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    /**
     * A builder class for {@link ClientCredentials}.
     */
    public static class Builder {
        private String clientTokenPath;
        private String clientTokenData;
        private String clientCertPath;
        private String clientCertData;
        private String clientKeyPath;
        private String clientKeyData;
        private String username;
        private String password;
        private String serverCertPath;
        private String serverCertData;

        /**
         * Builds {@link ClientCredentials} from the input received so far from
         * the client.
         *
         * @return
         * @throws IllegalArgumentException
         */
        public ClientCredentials build() throws IllegalArgumentException {
            String token = validateAndLoadTokenIfSupplied();
            Certificate cert = validateAndLoadClientCertIfSupplied();
            PrivateKey key = validateAndLoadClientKeyIfSupplied();
            BasicCredentials basicAuth = validateAndLoadBasicAuthIfSupplied();
            Certificate serverCert = validateAndLoadServerCertIfSupplied();

            checkArgument(hasClientToken() || hasClientCert() && hasClientKey() || hasUsername() && hasPassword(),
                    "neither token-auth, cert-auth, nor basic-auth specified");

            return new ClientCredentials(token, cert, key, basicAuth, serverCert);
        }

        /**
         * A JWT auth token at the specified file system path to use when
         * authenticating the client. May be null.
         *
         * @param path
         * @return
         */
        public Builder tokenPath(String path) {
            this.clientTokenPath = path;
            return this;
        }

        /**
         * A (base64-encoded) JWT auth token to use when authenticating the
         * client. May be null.
         *
         * @param tokenData
         * @return
         */
        public Builder tokenData(String tokenData) {
            this.clientTokenData = tokenData;
            return this;
        }

        /**
         * A PEM-encoded certificate at the specified file system path to use
         * when authenticating the client. May be null.
         *
         * @param path
         * @return
         */
        public Builder certPath(String path) {
            this.clientCertPath = path;
            return this;
        }

        /**
         * A (base64-encoded) PEM-encoded certificate to use when authenticating
         * the client. May be null.
         *
         * @param certContent
         * @return
         */
        public Builder certData(String certContent) {
            this.clientCertData = certContent;
            return this;
        }

        /**
         * A PEM-encoded private key at the specified file system path to use
         * when authenticating the client. May be null.
         *
         * @param path
         * @return
         */
        public Builder keyPath(String path) {
            this.clientKeyPath = path;
            return this;
        }

        /**
         * A (base64-encoded) PEM-encoded private key to use when authenticating
         * the client. May be null.
         *
         * @param keyContent
         * @return
         */
        public Builder keyData(String keyContent) {
            this.clientKeyData = keyContent;
            return this;
        }

        /**
         * A username for basic authentication to the kubernetes cluster. May be
         * null.
         *
         * @param username
         * @return
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * A password for basic authentication to the kubernetes cluster. May be
         * null.
         *
         * @param password
         * @return
         */
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * A PEM-encoded server/CA certificate at the specified file system path
         * to use when authenticating the server. May be null.
         *
         * @param path
         * @return
         */
        public Builder serverCertPath(String path) {
            this.serverCertPath = path;
            return this;
        }

        /**
         * A (base64-encoded) PEM-encoded server/CA certificate to use when
         * authenticating the server. May be null.
         *
         * @param certContent
         * @return
         */
        public Builder serverCertData(String certContent) {
            this.serverCertData = certContent;
            return this;
        }

        private boolean hasClientCert() {
            return this.clientCertData != null || this.clientCertPath != null;
        }

        private boolean hasClientKey() {
            return this.clientKeyData != null || this.clientKeyPath != null;
        }

        private boolean hasClientToken() {
            return this.clientTokenData != null || this.clientTokenPath != null;
        }

        private boolean hasServerCert() {
            return this.serverCertData != null || this.serverCertPath != null;
        }

        private boolean hasUsername() {
            return this.username != null;
        }

        private boolean hasPassword() {
            return this.password != null;
        }

        private Certificate validateAndLoadClientCertIfSupplied() throws IllegalArgumentException {
            if (!hasClientCert()) {
                return null;
            }

            checkArgument(!(this.clientCertData != null && this.clientCertPath != null),
                    "ambiguous input: both clientCertData and clientCertPath specified");
            checkArgument(hasClientKey(), "client certificate auth specified without a key");
            // makes sure that the client cert can be loaded/parsed.
            try {
                if (this.clientCertData != null) {
                    return loadCertData(this.clientCertData);
                }
                return loadCertPath(this.clientCertPath);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("failed to parse client cert: %s", e.getMessage()), e);
            }
        }

        private PrivateKey validateAndLoadClientKeyIfSupplied() throws IllegalArgumentException {
            if (!hasClientKey()) {
                return null;
            }

            checkArgument(!(this.clientKeyData != null && this.clientKeyPath != null),
                    "ambiguous input: both clientKeyData and clientKeyPath specified");
            checkArgument(hasClientCert(), "client key specified without a client cert");
            // makes sure that the client cert can be loaded/parsed.
            try {
                if (this.clientKeyData != null) {
                    return loadKeyData(this.clientKeyData);
                }
                return loadKeyPath(this.clientKeyPath);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("failed to parse client key: %s", e.getMessage()), e);
            }
        }

        private String validateAndLoadTokenIfSupplied() throws IllegalArgumentException {
            if (!hasClientToken()) {
                return null;
            }

            checkArgument(!(this.clientTokenData != null && this.clientTokenPath != null),
                    "ambiguous input: both clientTokenData and clientTokenPath specified");
            // if given, client token is *either* given as a value or as a path
            if (this.clientTokenData != null) {
                return this.clientTokenData;
            }

            try {
                return IoUtils.toString(new File(this.clientTokenPath), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        String.format("failed to load client token from %s: %s", this.clientTokenPath, e.getMessage()),
                        e);
            }
        }

        private Certificate validateAndLoadServerCertIfSupplied() throws IllegalArgumentException {
            if (!hasServerCert()) {
                return null;
            }

            checkArgument(!(this.serverCertData != null && this.serverCertPath != null),
                    "ambiguous input: both serverCertData and serverCertPath specified");

            try {
                if (this.serverCertData != null) {
                    return loadCertData(this.serverCertData);
                }
                return loadCertPath(this.serverCertPath);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("failed to parse server cert: %s", e.getMessage()), e);
            }
        }

        private BasicCredentials validateAndLoadBasicAuthIfSupplied() {
            if (!hasUsername() && !hasPassword()) {
                return null;
            }

            checkArgument(this.username != null && this.password != null,
                    "incomplete input: basic auth requires both username and password");

            return new BasicCredentials(this.username, this.password);
        }
    }

    /**
     * Loads a PEM-encoded private key from a base64-encoded string.
     *
     * @param base64Key
     *            A base64-encoded string containing a PEM-encoded key.
     * @return
     */
    public static PrivateKey loadKeyData(String base64Key) throws FileNotFoundException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeySpecException, IOException {
        String pemEncodedKey = Base64Utils.fromBase64(base64Key, StandardCharsets.UTF_8);
        return PemUtils.parseRsaPrivateKey(new StringReader(pemEncodedKey));
    }

    /**
     * Loads a PEM-encoded private key from a file system path.
     *
     * @param keyPath
     *            A file system path to a PEM-encoded key.
     * @return
     */
    public static PrivateKey loadKeyPath(String keyPath) throws FileNotFoundException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeySpecException, IOException {
        String pemEncodedKey = IoUtils.toString(new File(keyPath), StandardCharsets.UTF_8);
        return PemUtils.parseRsaPrivateKey(new StringReader(pemEncodedKey));
    }

    /**
     * Loads a PEM-encoded cert from a base64-encoded string.
     *
     *
     * @param base64Cert
     *            A base64-encoded string containing a PEM-encoded cert.
     * @return
     */
    public static Certificate loadCertData(String base64Cert) throws CertificateException, IOException {
        String pemEncodedCert = Base64Utils.fromBase64(base64Cert, StandardCharsets.UTF_8);
        return PemUtils.parseX509Cert(new StringReader(pemEncodedCert));
    }

    /**
     * Loads a PEM-encoded certificate from a file system path.
     *
     * @param certPath
     *            A file system path to a PEM-encoded cert.
     * @return
     */
    public static Certificate loadCertPath(String certPath) throws CertificateException, IOException {
        String pemEncodedCert = IoUtils.toString(new File(certPath), StandardCharsets.UTF_8);
        return PemUtils.parseX509Cert(new StringReader(pemEncodedCert));
    }
}
