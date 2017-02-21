package com.elastisys.scale.cloudpool.multipool.server;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
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
import com.elastisys.scale.commons.cli.server.BaseServerCliOptions;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.rest.client.RestClients;

/**
 * Verifies that the {@link MultiCloudPoolServer} can be configured to require
 * clients to configure with basic (password) authentication.
 */
public class TestMultiCloudPoolServerWithBasicAuth {
    private static final String SERVER_KEYSTORE = "src/test/resources/security/server/server_keystore.p12";
    private static final String SERVER_KEYSTORE_PASSWORD = "serverpass";

    private static final String REALM_FILE = "src/test/resources/security/server/security-realm.properties";
    private static final String AUTHORIZED_CLIENT_USERNAME = "admin";
    private static final String AUTHORIZED_CLIENT_PASSWORD = "adminpassword";

    private static final String UNAUTHORIZED_CLIENT_USERNAME = "unknownuser";
    private static final String UNAUTHORIZED_CLIENT_PASSWORD = "unknownpassword";

    private static final String WRONG_ROLE_CLIENT_USERNAME = "guest";
    private static final String WRONG_ROLE_CLIENT_PASSWORD = "guestpassword";

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
        options.requireBasicAuth = true;
        options.requireRole = "ADMIN";
        options.realmFile = REALM_FILE;
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

        Response response = RestClients.httpsNoAuth().target(url("/cloudpools")).request().get();
        assertThat(response.getStatus(), is(Status.UNAUTHORIZED.getStatusCode()));
    }

    /**
     * Access with an authorized client should be permitted.
     */
    @Test
    public void accessWithAuthorizedClient() {
        when(multiCloudPool.list()).thenReturn(asList("pool1", "pool2"));

        Response response = RestClients.httpsBasicAuth(AUTHORIZED_CLIENT_USERNAME, AUTHORIZED_CLIENT_PASSWORD)
                .target(url("/cloudpools")).request().get();

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(response.readEntity(new GenericType(List.class)),
                is(asList(url("/cloudpools/pool1"), url("/cloudpools/pool2"))));
    }

    /**
     * Both correct username and password must be given.
     */
    @Test
    public void accessWithWrongPassword() {
        when(multiCloudPool.list()).thenReturn(asList("pool1", "pool2"));

        Response response = RestClients.httpsBasicAuth(AUTHORIZED_CLIENT_USERNAME, "wrongpassword")
                .target(url("/cloudpools")).request().get();
        assertThat(response.getStatus(), is(Status.UNAUTHORIZED.getStatusCode()));

    }

    /**
     * An unknown client will be met by a 401 (Unauthorized).
     */
    @Test
    public void accessWithUnauthorizedClient() {
        when(multiCloudPool.list()).thenReturn(asList("pool1", "pool2"));

        Response response = RestClients.httpsBasicAuth(UNAUTHORIZED_CLIENT_USERNAME, UNAUTHORIZED_CLIENT_PASSWORD)
                .target(url("/cloudpools")).request().get();

        assertThat(response.getStatus(), is(Status.UNAUTHORIZED.getStatusCode()));
    }

    /**
     * The user must have the required role
     * ({@link BaseServerCliOptions#requireRole}) or else a 403 (Forbidden)
     * error is returned.
     */
    @Test
    public void accessWithWrongRole() {
        when(multiCloudPool.list()).thenReturn(asList("pool1", "pool2"));

        Response response = RestClients.httpsBasicAuth(WRONG_ROLE_CLIENT_USERNAME, WRONG_ROLE_CLIENT_PASSWORD)
                .target(url("/cloudpools")).request().get();

        assertThat(response.getStatus(), is(Status.FORBIDDEN.getStatusCode()));
    }

    private static String url(String path) {
        return String.format("https://localhost:%d%s", port, path);
    }
}
