package com.elastisys.scale.cloudpool.kubernetes.apiserver.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.SocketException;

import javax.net.ssl.SSLHandshakeException;

import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.config.AuthConfig;
import com.elastisys.scale.cloudpool.kubernetes.mock.FakeServlet;
import com.elastisys.scale.cloudpool.kubernetes.mock.HttpResponse;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.server.ServletDefinition;
import com.elastisys.scale.commons.server.ServletServerBuilder;
import com.elastisys.scale.commons.server.SslKeyStoreType;

/**
 * Verifies proper behavior of {@link StandardApiServerClient} when told to
 * authenticate the certificate of the the API server.
 */
public class TestStandardApiServerClientServerAuth {
    private static final String AUTH_TOKEN_PATH = "src/test/resources/ssl/auth-token";

    private static final String TRUSTED_SERVER_CERT_PATH = "src/test/resources/apiserver-test/server/server_certificate.pem";
    private static final String TRUSTED_SERVER_KEYSTORE_PATH = "src/test/resources/apiserver-test/server/server_keystore.p12";
    private static final String TRUSTED_SERVER_KEYSTORE_PASSWORD = "serverpass";

    private static final String UNTRUSTED_SERVER_KEYSTORE_PATH = "src/test/resources/apiserver-test/untrusted-server/server_keystore.p12";
    private static final String UNTRUSTED_SERVER_KEYSTORE_PASSWORD = "serverpass";

    private int port = HostUtils.findFreePorts(1).get(0);

    /** Server hosting the fake api server. */
    private Server server;

    private FakeServlet fakeApiServer = new FakeServlet(new HttpResponse(200, null));

    /** Object under test. Set up to talk to {@link #fakeApiServer}. */
    private ApiServerClient apiServerClient;

    @Before
    public void beforeTest() throws Exception {
        startServer(TRUSTED_SERVER_KEYSTORE_PATH, TRUSTED_SERVER_KEYSTORE_PASSWORD);
    }

    @After
    public void afterTest() throws Exception {
        stopServer();
    }

    /**
     * When the {@link StandardApiServerClient} has been told to authenticate
     * the API server's certificate, it must let the message pass if the
     * server's certificate matches the expected certificate.
     */
    @Test
    public void verifyServerCertOnTrustedServer() throws Exception {
        this.apiServerClient = new StandardApiServerClient().configure(apiServerUrl(), serverCertAuth());
        this.apiServerClient.get("/some/path");

        // verify that the call was made
        assertThat(this.fakeApiServer.getRequests().size(), is(1));
    }

    /**
     * When the {@link StandardApiServerClient} has been told to authenticate
     * the API server's certificate, it must refuse the SSL connection if the
     * server's certificate does not match the expected certificate.
     */
    @Test
    public void verifyServerCertOnUntrustedServer() throws Exception {
        // replace trusted server with a server with an untrusted server cert
        stopServer();
        startServer(UNTRUSTED_SERVER_KEYSTORE_PATH, UNTRUSTED_SERVER_KEYSTORE_PASSWORD);

        this.apiServerClient = new StandardApiServerClient().configure(apiServerUrl(), serverCertAuth());
        try {
            this.apiServerClient.get("/some/path");
            fail("remote server should not be authenticated");
        } catch (Exception e) {
            assertSslHandshakeFailure(e);
        }

        // verify that no call reached the server
        assertThat(this.fakeApiServer.getRequests().size(), is(0));
    }

    private AuthConfig serverCertAuth() {
        return AuthConfig.builder().serverCertPath(TRUSTED_SERVER_CERT_PATH).tokenPath(AUTH_TOKEN_PATH).build();
    }

    private String apiServerUrl() {
        return String.format("https://localhost:%d", this.port);
    }

    /**
     * Verify that an exception is due to a failure to establish an SSL
     * connection.
     *
     * @param cause
     */
    private void assertSslHandshakeFailure(Throwable cause) {
        if (cause instanceof SSLHandshakeException) {
            return;
        }
        // in some cases it seems that a "connection reset" error can also be
        // seen on the client side on failure to authenticate with a cert.
        if (cause instanceof SocketException) {
            assertTrue(cause.getMessage().contains("Connection reset"));
        }
    }

    private void startServer(String keyStorePath, String keystorePassword) throws Exception {
        ServletDefinition servletDef = new ServletDefinition.Builder().servlet(this.fakeApiServer).build();
        this.server = ServletServerBuilder.create().httpsPort(this.port) //
                .sslKeyStorePath(keyStorePath).sslKeyStorePassword(keystorePassword)
                .sslKeyStoreType(SslKeyStoreType.PKCS12) //
                .addServlet(servletDef).build();
        this.server.start();
    }

    private void stopServer() throws Exception {
        if (this.server != null) {
            this.server.stop();
        }
    }

}
