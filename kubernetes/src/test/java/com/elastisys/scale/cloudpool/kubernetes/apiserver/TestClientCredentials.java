package com.elastisys.scale.cloudpool.kubernetes.apiserver;

import static com.elastisys.scale.cloudpool.kubernetes.testutils.AuthUtils.loadCert;
import static com.elastisys.scale.cloudpool.kubernetes.testutils.AuthUtils.loadKey;
import static com.elastisys.scale.cloudpool.kubernetes.testutils.AuthUtils.loadString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Exercises {@link ClientCredentials} and its builder.
 */
public class TestClientCredentials {
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
    public void buildCertAuthByPath() throws Exception {
        ClientCredentials creds = ClientCredentials.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH)
                .build();

        assertThat(creds.hasCert(), is(true));
        assertThat(creds.hasKey(), is(true));
        assertThat(creds.hasToken(), is(false));
        assertThat(creds.hasServerCert(), is(false));
        assertThat(creds.hasBasicAuth(), is(false));

        assertThat(creds.getCert(), is(loadCert(CLIENT_CERT_PATH)));
        assertThat(creds.getKey(), is(loadKey(CLIENT_KEY_PATH)));
        assertThat(creds.getToken(), is(nullValue()));
        assertThat(creds.getServerCert(), is(nullValue()));
    }

    /**
     * It should be possible to pass client cert and key by (base64-encoded)
     * value.
     */
    @Test
    public void buildCertAuthByValue() throws Exception {
        ClientCredentials creds = ClientCredentials.builder().certData(loadString(CLIENT_CERT_BASE64_PATH))
                .keyData(loadString(CLIENT_KEY_BASE64_PATH)).build();

        assertThat(creds.hasCert(), is(true));
        assertThat(creds.hasKey(), is(true));
        assertThat(creds.hasToken(), is(false));
        assertThat(creds.hasServerCert(), is(false));
        assertThat(creds.hasBasicAuth(), is(false));

        assertThat(creds.getCert(), is(loadCert(CLIENT_CERT_PATH)));
        assertThat(creds.getKey(), is(loadKey(CLIENT_KEY_PATH)));
        assertThat(creds.getToken(), is(nullValue()));
        assertThat(creds.getServerCert(), is(nullValue()));
    }

    /**
     * Should be possible to specify either cert by path and key by value or
     * vice versa.
     */
    @Test
    public void configureCertByPathAndKeyByValue() throws Exception {
        // client by path, key by value
        ClientCredentials creds = ClientCredentials.builder().certPath(CLIENT_CERT_PATH)
                .keyData(loadString(CLIENT_KEY_BASE64_PATH)).build();
        assertThat(creds.getCert(), is(loadCert(CLIENT_CERT_PATH)));
        assertThat(creds.getKey(), is(loadKey(CLIENT_KEY_PATH)));

        // client by value, key by path
        creds = ClientCredentials.builder().certData(loadString(CLIENT_CERT_BASE64_PATH)).keyPath(CLIENT_KEY_PATH)
                .build();
        assertThat(creds.getCert(), is(loadCert(CLIENT_CERT_PATH)));
        assertThat(creds.getKey(), is(loadKey(CLIENT_KEY_PATH)));
    }

    /**
     * At least one of cert, token, or basic auth needs to be specified.
     */
    @Test(expected = IllegalArgumentException.class)
    public void buildWithoutAuthMethod() {
        ClientCredentials.builder().build();
    }

    /**
     * Cert-based auth requires a client cert.
     */
    @Test(expected = IllegalArgumentException.class)
    public void configureCertAuthWithMissingCert() {
        ClientCredentials.builder().keyPath(CLIENT_KEY_PATH).build();
    }

    /**
     * Cert-based auth requires a client key.
     */
    @Test(expected = IllegalArgumentException.class)
    public void configureCertAuthWithMissingKey() {
        ClientCredentials.builder().certPath(CLIENT_CERT_PATH).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureNonExistentClientCertFile() {
        ClientCredentials.builder().certPath("/non/existent/path").keyPath(CLIENT_KEY_PATH).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureNonExistentClientKeyFile() {
        ClientCredentials.builder().certPath(CLIENT_CERT_PATH).keyPath("/non/existent/path").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void specifyClientKeyBothAsPathAndValue() {
        ClientCredentials.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH)
                .keyData(loadString(CLIENT_KEY_PATH)).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void specifyClientCertBothAsPathAndValue() {
        ClientCredentials.builder().certPath(CLIENT_CERT_PATH).certData(loadString(CLIENT_CERT_PATH))
                .keyPath(CLIENT_KEY_PATH).build();
    }

    /**
     * It should be possible to specify token-based authentication by passing a
     * file system path to a JWT auth token file.
     */
    @Test
    public void configureTokenAuthByPath() {
        ClientCredentials creds = ClientCredentials.builder().tokenPath(CLIENT_TOKEN_PATH).build();

        assertThat(creds.hasCert(), is(false));
        assertThat(creds.hasKey(), is(false));
        assertThat(creds.hasToken(), is(true));
        assertThat(creds.hasServerCert(), is(false));
        assertThat(creds.hasBasicAuth(), is(false));

        assertThat(creds.getCert(), is(nullValue()));
        assertThat(creds.getKey(), is(nullValue()));
        assertThat(creds.getToken(), is(loadString(CLIENT_TOKEN_PATH)));
        assertThat(creds.getServerCert(), is(nullValue()));
    }

    /**
     * It should be possible to specify token-based authentication by passing a
     * base64-encoded JWT auth token file.
     */
    @Test
    public void configureTokenAuthByValue() throws Exception {
        ClientCredentials creds = ClientCredentials.builder().tokenData(loadString(CLIENT_TOKEN_PATH)).build();

        assertThat(creds.hasCert(), is(false));
        assertThat(creds.hasKey(), is(false));
        assertThat(creds.hasToken(), is(true));
        assertThat(creds.hasServerCert(), is(false));
        assertThat(creds.hasBasicAuth(), is(false));

        assertThat(creds.getCert(), is(nullValue()));
        assertThat(creds.getKey(), is(nullValue()));
        assertThat(creds.getToken(), is(loadString(CLIENT_TOKEN_PATH)));
        assertThat(creds.getServerCert(), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureNonExistentTokenFile() {
        ClientCredentials.builder().tokenPath("/non/existent/path").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void specifyClientTokenBothAsPathAndValue() {
        ClientCredentials.builder().tokenPath(CLIENT_TOKEN_PATH).tokenData(loadString(CLIENT_TOKEN_PATH)).build();
    }

    /**
     * If server cert should be verified, it can be specified as a file system
     * path.
     */
    @Test
    public void configureServerAuthByPath() throws Exception {
        ClientCredentials creds = ClientCredentials.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH)
                .serverCertPath(SERVER_CERT_PATH).build();

        assertThat(creds.hasCert(), is(true));
        assertThat(creds.hasKey(), is(true));
        assertThat(creds.hasToken(), is(false));
        assertThat(creds.hasServerCert(), is(true));
        assertThat(creds.hasBasicAuth(), is(false));

        assertThat(creds.getCert(), is(loadCert(CLIENT_CERT_PATH)));
        assertThat(creds.getKey(), is(loadKey(CLIENT_KEY_PATH)));
        assertThat(creds.getToken(), is(nullValue()));
        assertThat(creds.getServerCert(), is(loadCert(SERVER_CERT_PATH)));
    }

    /**
     * If server cert should be verified, it can be specified as a
     * base64-encoded value.
     */
    @Test
    public void configureServerAuthByValue() throws Exception {
        ClientCredentials creds = ClientCredentials.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH)
                .serverCertData(loadString(SERVER_CERT_BASE64_PATH)).build();
        assertThat(creds.getServerCert(), is(loadCert(SERVER_CERT_PATH)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureServerAuthWithNonExistentPath() {
        ClientCredentials.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH)
                .serverCertPath("/non/existent/path").build();
    }

    /**
     * Should be possible to specify both cert and token auth.
     */
    @Test
    public void configureWithCertAuthAndTokenAuth() throws Exception {
        ClientCredentials config = ClientCredentials.builder().certPath(CLIENT_CERT_PATH).keyPath(CLIENT_KEY_PATH)
                .tokenPath(CLIENT_TOKEN_PATH).build();

        assertThat(config.getCert(), is(loadCert(CLIENT_CERT_PATH)));
        assertThat(config.getKey(), is(loadKey(CLIENT_KEY_PATH)));
        assertThat(config.getToken(), is(loadString(CLIENT_TOKEN_PATH)));
        assertThat(config.getServerCert(), is(nullValue()));
    }

    // TODO: basic auth
    // TODO: cert and basic auth
    // TODO: token and basic auth is mutually exclusive?
    // TODO: token and basic auth is mutually exclusive?
}
