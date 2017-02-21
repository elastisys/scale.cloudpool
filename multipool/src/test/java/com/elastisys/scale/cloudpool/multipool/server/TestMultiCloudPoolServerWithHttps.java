package com.elastisys.scale.cloudpool.multipool.server;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.List;

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
import com.elastisys.scale.commons.rest.client.RestClients;

/**
 * Verifies that the {@link MultiCloudPoolServer} can be configured to run over
 * HTTPS.
 */
public class TestMultiCloudPoolServerWithHttps {
    private static final String SERVER_KEYSTORE = "src/test/resources/security/server/server_keystore.p12";
    private static final String SERVER_KEYSTORE_PASSWORD = "serverpass";

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
     * Verify that server cannot be reached over HTTPS.
     */
    @Test
    public void accessibleWithHttpsClient() {
        when(multiCloudPool.list()).thenReturn(asList("pool1", "pool2"));

        Response response = RestClients.httpsNoAuth().target(url("https", "/cloudpools")).request().get();

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(response.readEntity(new GenericType(List.class)),
                is(asList(url("https", "/cloudpools/pool1"), url("https", "/cloudpools/pool2"))));
    }

    /**
     * Verify that server cannot be reached over HTTP.
     */
    @Test
    public void notAccessibleWithHttpClient() {
        when(multiCloudPool.list()).thenReturn(asList("pool1", "pool2"));

        try {
            RestClients.httpNoAuth().target(url("http", "/cloudpools")).request().get();
            fail("expected to fail");
        } catch (Exception e) {
            // expected
        }
    }

    private static String url(String protocol, String path) {
        return String.format("%s://localhost:%d%s", protocol, port, path);
    }
}
