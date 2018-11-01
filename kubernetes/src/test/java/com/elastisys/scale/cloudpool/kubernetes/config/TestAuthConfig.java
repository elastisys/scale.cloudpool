package com.elastisys.scale.cloudpool.kubernetes.config;

import static com.elastisys.scale.cloudpool.kubernetes.testutils.AuthUtils.loadString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Exercise {@link AuthConfig}.
 */
public class TestAuthConfig {
    /** Path to client cert. */
    private static final String CLIENT_CERT_PATH = "src/test/resources/ssl/admin.pem";
    /** Path to base64-encoded version of client cert. */
    private static final String CLIENT_CERT_BASE64_PATH = "src/test/resources/ssl/admin.pem.base64";
    /** Path to client key. */
    private static final String CLIENT_KEY_PATH = "src/test/resources/ssl/admin-key.pem";
    /** Path to base64-encoded version of client key. */
    private static final String CLIENT_KEY_BASE64_PATH = "src/test/resources/ssl/admin-key.pem.base64";

    /** Path to client token. */
    private static final String CLIENT_TOKEN_PATH = "src/test/resources/ssl/auth-token";

    /** Path to server/CA cert. */
    private static final String SERVER_CERT_PATH = "src/test/resources/ssl/ca.pem";
    /** Path to base64-encoded server/CA cert. */
    private static final String SERVER_CERT_BASE64_PATH = "src/test/resources/ssl/ca.pem.base64";

    /**
     * It should be possible to specify certificate authentication by specifying
     * file system paths to a client cert and key.
     */
    @Test
    public void configureCertAuthByPath() throws Exception {
        AuthConfig config = AuthConfig.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH).build();
        config.validate();

        assertThat(config.getClientCertPath(), is(CLIENT_CERT_PATH));
        assertThat(config.getClientKeyPath(), is(CLIENT_KEY_PATH));
        assertThat(config.getClientToken(), is(nullValue()));
        assertThat(config.getServerCert(), is(nullValue()));
    }

    /**
     * It should be possible to pass client cert and key by (base64-encoded)
     * value.
     */
    @Test
    public void configureCertAuthByValue() throws Exception {
        AuthConfig config = AuthConfig.builder().certData(loadString(CLIENT_CERT_BASE64_PATH))
                .keyData(loadString(CLIENT_KEY_BASE64_PATH)).build();
        config.validate();

        assertThat(config.getClientCert(), is(loadString(CLIENT_CERT_BASE64_PATH)));
        assertThat(config.getClientKey(), is(loadString(CLIENT_KEY_BASE64_PATH)));
        assertThat(config.getClientToken(), is(nullValue()));
        assertThat(config.getServerCert(), is(nullValue()));
    }

    /**
     * Should be possible to specify either cert by path and key by value or
     * vice versa.
     */
    @Test
    public void configureCertByPathAndKeyByValue() throws Exception {
        // client by path, key by value
        AuthConfig config = AuthConfig.builder().certPath(CLIENT_CERT_PATH).keyData(loadString(CLIENT_KEY_BASE64_PATH))
                .build();
        assertThat(config.getClientCertPath(), is(CLIENT_CERT_PATH));
        assertThat(config.getClientKey(), is(loadString(CLIENT_KEY_BASE64_PATH)));

        // client by value, key by path
        config = AuthConfig.builder().certData(loadString(CLIENT_CERT_BASE64_PATH)).keyPath(CLIENT_KEY_PATH).build();
        assertThat(config.getClientCert(), is(loadString(CLIENT_CERT_BASE64_PATH)));
        assertThat(config.getClientKeyPath(), is(CLIENT_KEY_PATH));
    }

    /**
     * Cert-based auth requires a client cert.
     */
    @Test(expected = IllegalArgumentException.class)
    public void configureCertAuthWithMissingCert() {
        AuthConfig.builder().keyPath(CLIENT_KEY_PATH).build().validate();
    }

    /**
     * Cert-based auth requires a client key.
     */
    @Test(expected = IllegalArgumentException.class)
    public void configureCertAuthWithMissingKey() {
        AuthConfig.builder().certPath(CLIENT_CERT_PATH).build().validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureNonExistentClientCertFile() throws Exception {
        AuthConfig.builder().certPath("/non/existent/path").keyPath(CLIENT_KEY_PATH).build().validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureNonExistentClientKeyFile() throws Exception {
        AuthConfig.builder().certPath(CLIENT_CERT_PATH).keyPath("/non/existent/path").build().validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void specifyClientKeyBothAsPathAndValue() throws Exception {
        AuthConfig.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH).keyData(loadString(CLIENT_KEY_PATH))
                .build().validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void specifyClientCertBothAsPathAndValue() throws Exception {
        AuthConfig.builder().certPath(CLIENT_CERT_PATH).certData(loadString(CLIENT_CERT_PATH)).keyPath(CLIENT_KEY_PATH)
                .build().validate();
    }

    /**
     * It should be possible to specify token-based authentication by passing a
     * file system path to a JWT auth token file.
     */
    @Test
    public void configureTokenAuthByPath() throws Exception {
        AuthConfig config = AuthConfig.builder().tokenPath(CLIENT_TOKEN_PATH).build();
        config.validate();

        assertThat(config.getClientTokenPath(), is(CLIENT_TOKEN_PATH));
    }

    /**
     * It should be possible to specify token-based authentication by passing a
     * base64-encoded JWT auth token file.
     */
    @Test
    public void configureTokenAuthByValue() throws Exception {
        AuthConfig config = AuthConfig.builder().tokenData(loadString(CLIENT_TOKEN_PATH)).build();
        config.validate();

        assertThat(config.getClientCert(), is(nullValue()));
        assertThat(config.getClientKey(), is(nullValue()));
        assertThat(config.getClientToken(), is(loadString(CLIENT_TOKEN_PATH)));
        assertThat(config.getServerCert(), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureNonExistentTokenFile() throws Exception {
        AuthConfig config = AuthConfig.builder().tokenPath("/non/existent/path").build();
        config.validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void specifyClientTokenBothAsPathAndValue() throws Exception {
        AuthConfig.builder().tokenPath(CLIENT_TOKEN_PATH).tokenData(loadString(CLIENT_TOKEN_PATH)).build().validate();
    }

    /**
     * If server cert should be verified, it can be specified as a file system
     * path.
     */
    @Test
    public void configureServerAuthByPath() throws Exception {
        AuthConfig config = AuthConfig.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH)
                .serverCertPath(SERVER_CERT_PATH).build();
        config.validate();

        assertThat(config.getServerCertPath(), is(SERVER_CERT_PATH));
    }

    /**
     * If server cert should be verified, it can be specified as a
     * base64-encoded value.
     */
    @Test
    public void configureServerAuthByValue() throws Exception {
        AuthConfig config = AuthConfig.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH)
                .serverCertData(loadString(SERVER_CERT_BASE64_PATH)).build();
        config.validate();

        assertThat(config.getServerCert(), is(loadString(SERVER_CERT_BASE64_PATH)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureServerAuthWithNonExistentPath() throws Exception {
        AuthConfig.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH).serverCertPath("/non/existent/path")
                .build().validate();
    }

    /**
     * Should be possible to specify both cert and token auth.
     */
    @Test
    public void configureWithCertAuthAndTokenAuth() throws Exception {
        AuthConfig config = AuthConfig.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH)
                .tokenPath(CLIENT_TOKEN_PATH).build();
        config.validate();

        assertThat(config.getClientCertPath(), is(CLIENT_CERT_PATH));
        assertThat(config.getClientKeyPath(), is(CLIENT_KEY_PATH));
        assertThat(config.getClientTokenPath(), is(CLIENT_TOKEN_PATH));
        assertThat(config.getServerCert(), is(nullValue()));
    }
}
