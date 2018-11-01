package com.elastisys.scale.cloudpool.kubernetes.apiserver.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.SocketException;

import javax.net.ssl.SSLHandshakeException;

import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.ClientConfig;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.ClientCredentials;
import com.elastisys.scale.cloudpool.kubernetes.mock.FakeServlet;
import com.elastisys.scale.cloudpool.kubernetes.mock.HttpResponse;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.server.ServletDefinition;
import com.elastisys.scale.commons.server.ServletServerBuilder;
import com.elastisys.scale.commons.server.SslKeyStoreType;

/**
 * Verifies proper behavior of {@link StandardApiServerClient} when told to
 * authenticate against the API server with a client certificate.
 */
public class TestStandardApiServerClientCertAuth {

    private static final String SSL_KEYSTORE_PATH = "src/test/resources/apiserver-test/server/server_keystore.p12";
    private static final String SSL_KEYSTORE_PASSWORD = "serverpass";
    private static final String SSL_TRUSTSTORE_PATH = "src/test/resources/apiserver-test/server/truststore.jks";
    private static final String SSL_TRUSTSTORE_PASSWORD = "trustpass";

    /** Certificate for a client that is in the server's trust store. */
    private static final String TRUSTED_CLIENT_CERT_PATH = "src/test/resources/apiserver-test/trusted-client/client_certificate.pem";
    private static final String TRUSTED_CLIENT_KEY_PATH = "src/test/resources/apiserver-test/trusted-client/client_private.pem";

    /** Certificate for a client that is not in the server's trust store. */
    private static final String UNTRUSTED_CLIENT_CERT_PATH = "src/test/resources/apiserver-test/untrusted-client/client_certificate.pem";
    private static final String UNTRUSTED_CLIENT_KEY_PATH = "src/test/resources/apiserver-test/untrusted-client/client_private.pem";

    private int port = HostUtils.findFreePorts(1).get(0);

    /** Server hosting the fake api server. */
    private Server server;

    private FakeServlet fakeApiServer = new FakeServlet(new HttpResponse(200, null));

    /** Object under test. Set up to talk to {@link #fakeApiServer}. */
    private ApiServerClient apiServerClient;

    @Before
    public void beforeTest() throws Exception {
        ServletDefinition servletDef = new ServletDefinition.Builder().servlet(this.fakeApiServer).build();
        this.server = ServletServerBuilder.create().httpsPort(this.port).//
                sslKeyStorePath(SSL_KEYSTORE_PATH).sslKeyStorePassword(SSL_KEYSTORE_PASSWORD)
                .sslKeyStoreType(SslKeyStoreType.PKCS12)//
                .sslRequireClientCert(true)//
                .sslTrustStorePath(SSL_TRUSTSTORE_PATH).sslTrustStorePassword(SSL_TRUSTSTORE_PASSWORD)
                .sslTrustStoreType(SslKeyStoreType.JKS)//
                .addServlet(servletDef).build();
        this.server.start();
    }

    @After
    public void afterTest() throws Exception {
        if (this.server != null) {
            this.server.stop();
        }
    }

    /**
     * Make sure that {@link StandardApiServerClient} can be told to
     * authenticate with a client certificate.
     */
    @Test
    public void useCertAuth() throws Exception {
        this.apiServerClient = new StandardApiServerClient().configure(trustedClientAuth());
        this.apiServerClient.get("/some/path");

        // verify that it is NOT possible, when using an untrusted client cert
        // (otherwise, our test server is incorrectly set up)
        try {
            this.apiServerClient = new StandardApiServerClient().configure(untrustedClientAuth());
            this.apiServerClient.get("/some/path");
            fail("should NOT be possible to authenticate with an untrusted client cert");
        } catch (Exception e) {
            assertSslHandshakeFailure(e);
        }
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
        // in some cases it seems that a "connection reset" or "broken pipe"
        // error can also be seen on the client side on failure to authenticate
        // with a cert.
        if (cause instanceof SocketException) {
            assertTrue(cause.getMessage().contains("Connection reset")
                    || cause.getMessage().contains("Broken pipe (Write failed)"));
        }
    }

    private ClientConfig trustedClientAuth() throws Exception {
        ClientConfig clientConfig = new ClientConfig(apiServerUrl(), ClientCredentials.builder()
                .certPath(TRUSTED_CLIENT_CERT_PATH).keyPath(TRUSTED_CLIENT_KEY_PATH).build());
        return clientConfig;
    }

    private ClientConfig untrustedClientAuth() throws Exception {
        ClientConfig clientConfig = new ClientConfig(apiServerUrl(), ClientCredentials.builder()
                .certPath(UNTRUSTED_CLIENT_CERT_PATH).keyPath(UNTRUSTED_CLIENT_KEY_PATH).build());
        return clientConfig;
    }

    private String apiServerUrl() {
        return String.format("https://localhost:%d", this.port);
    }

}
