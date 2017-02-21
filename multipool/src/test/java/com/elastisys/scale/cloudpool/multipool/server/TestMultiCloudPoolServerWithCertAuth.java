package com.elastisys.scale.cloudpool.multipool.server;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.net.SocketException;
import java.nio.file.Paths;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.elastisys.scale.cloudpool.multipool.api.MultiCloudPool;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;
import com.elastisys.scale.commons.rest.client.RestClients;

/**
 * Verifies that the {@link MultiCloudPoolServer} can be configured to require
 * clients to configure with certificate authentication.
 */
public class TestMultiCloudPoolServerWithCertAuth {
    private static final String SERVER_KEYSTORE = "src/test/resources/security/server/server_keystore.p12";
    private static final String SERVER_KEYSTORE_PASSWORD = "serverpass";

    private static final String SERVER_TRUSTSTORE = "src/test/resources/security/server/server_truststore.jks";
    private static final String SERVER_TRUSTSTORE_PASSWORD = "trustpass";

    private static final String TRUSTED_CLIENT_KEYSTORE = "src/test/resources/security/client/client_keystore.p12";
    private static final String TRUSTED_CLIENT_KEYSTORE_PASSWORD = "clientpass";
    private static final String UNTRUSTED_CLIENT_KEYSTORE = "src/test/resources/security/untrusted/untrusted_keystore.p12";
    private static final String UNTRUSTED_CLIENT_KEYSTORE_PASSWORD = "untrustedpass";

    /** Web server to use throughout the tests. */
    private static Server server;
    /** Server port to use. */
    private static int port;
    /** Storage dir for configurations. */
    private static final String storageDir = Paths.get("target", "multipool").toString();

    private static MultiCloudPool multiCloudPool = mock(MultiCloudPool.class);

    @BeforeClass
    public static void onSetup() throws Exception {
        List<Integer> freePorts = HostUtils.findFreePorts(1);
        port = freePorts.get(0);

        MultiCloudPoolOptions options = new MultiCloudPoolOptions();
        options.httpsPort = port;
        options.sslKeyStore = SERVER_KEYSTORE;
        options.sslKeyStorePassword = SERVER_KEYSTORE_PASSWORD;
        options.requireClientCert = true;
        options.sslTrustStore = SERVER_TRUSTSTORE;
        options.sslTrustStorePassword = SERVER_TRUSTSTORE_PASSWORD;
        options.storageDir = storageDir;

        server = MultiCloudPoolServer.createServer(multiCloudPool, options);
        server.start();
    }

    @Before
    public void beforeTestMethod() {
        reset(multiCloudPool);
    }

    @AfterClass
    public static void onTeardown() throws Exception {
        server.stop();
        server.join();
    }

    /**
     * An attempt to access without credentials should result in 401
     * (Unauthorized).
     */
    @Test
    public void accessWithUnauthenticatedClient() {
        when(multiCloudPool.list()).thenReturn(asList("pool1", "pool2"));

        try {
            RestClients.httpsNoAuth().target(url("/cloudpools")).request().get();
        } catch (ProcessingException e) {
            assertSslHandshakeFailure(e);
        }
    }

    /**
     * Access with an authorized client should be permitted.
     */
    @Test
    public void accessWithAuthorizedClient() {
        when(multiCloudPool.list()).thenReturn(asList("pool1", "pool2"));

        Response response = RestClients
                .httpsCertAuth(TRUSTED_CLIENT_KEYSTORE, TRUSTED_CLIENT_KEYSTORE_PASSWORD, KeyStoreType.PKCS12)
                .target(url("/cloudpools")).request().get();

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(response.readEntity(new GenericType(List.class)),
                is(asList(url("/cloudpools/pool1"), url("/cloudpools/pool2"))));
    }

    /**
     * An unknown client will be met by a 401 (Unauthorized).
     */
    @Test
    public void accessWithUnauthorizedClient() {
        when(multiCloudPool.list()).thenReturn(asList("pool1", "pool2"));

        try {
            RestClients
                    .httpsCertAuth(UNTRUSTED_CLIENT_KEYSTORE, UNTRUSTED_CLIENT_KEYSTORE_PASSWORD, KeyStoreType.PKCS12)
                    .target(url("/cloudpools")).request().get();
        } catch (ProcessingException e) {
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
        // in some cases it seems that a "connection reset" error can also be
        // seen on the client side on failure to authenticate with a cert.
        if (cause instanceof SocketException) {
            assertTrue(cause.getMessage().contains("Connection reset"));
        }
    }

    private static String url(String path) {
        return String.format("https://localhost:%d%s", port, path);
    }
}
