package com.elastisys.scale.cloudpool.api.server;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.rest.client.RestClients;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.io.Resources;

/**
 * Verifies the behavior of the {@link CloudPoolServer} when configured to
 * require client certificate authentication.
 *
 *
 *
 */
public class TestRestApiSecurityBasicAuth {
    private static final String SERVER_KEYSTORE = Resources.getResource("security/server/server_keystore.p12")
            .toString();
    private static final String SERVER_KEYSTORE_PASSWORD = "serverpass";

    private static final String AUTHORIZED_CLIENT_USERNAME = "autoscaler";
    private static final String AUTHORIZED_CLIENT_PASSWORD = "autoscalerpassword";

    private static final String UNAUTHORIZED_CLIENT_USERNAME = "unknownuser";
    private static final String UNAUTHORIZED_CLIENT_PASSWORD = "unknownpassword";

    private static final String WRONG_ROLE_CLIENT_USERNAME = "guest";
    private static final String WRONG_ROLE_CLIENT_PASSWORD = "guestpassword";

    /** Web server to use throughout the tests. */
    private static Server server;
    /** Server port to use for HTTPS. */
    private static int httpsPort;
    /** Storage dir for configurations. */
    private static final String storageDir = Paths.get("target", "cloudpool", "storage").toString();

    private static CloudPool cloudPool = mock(CloudPool.class);

    @BeforeClass
    public static void onSetup() throws Exception {
        List<Integer> freePorts = HostUtils.findFreePorts(2);
        httpsPort = freePorts.get(0);

        // cloudpool is configured and started
        CloudPoolStatus startedStatus = new CloudPoolStatus(true, true);
        when(cloudPool.getStatus()).thenReturn(startedStatus);
        // set up mocked cloud pool that will back the created server
        when(cloudPool.getMachinePool()).thenReturn(MachinePool.emptyPool(UtcTime.now()));

        CloudPoolOptions options = new CloudPoolOptions();
        options.httpsPort = httpsPort;
        options.sslKeyStore = SERVER_KEYSTORE;
        options.sslKeyStorePassword = SERVER_KEYSTORE_PASSWORD;
        options.requireBasicAuth = true;
        options.requireRole = "AUTOSCALER";
        options.realmFile = "src/test/resources/security/server/security-realm.properties";
        options.storageDir = storageDir;

        server = CloudPoolServer.createServer(cloudPool, options);
        server.start();
    }

    @AfterClass
    public static void onTeardown() throws Exception {
        server.stop();
        server.join();
    }

    /**
     * Connect with a client that doesn't authenticate. This should fail, since
     * server requires basic authentication credentials.
     */
    @Test
    public void connectWithNoAuthentication() {
        Client noAuthClient = RestClients.httpsNoAuth();
        Response response = noAuthClient.target(getUrl()).request().get();
        assertThat(response.getStatus(), is(UNAUTHORIZED.getStatusCode()));
    }

    /**
     * Connect with an authorized client.
     */
    @Test
    public void connectWithAuthorizedClient() {
        Client client = RestClients.httpsBasicAuth(AUTHORIZED_CLIENT_USERNAME, AUTHORIZED_CLIENT_PASSWORD);
        Response response = client.target(getUrl()).request().get();
        assertThat(response.getStatus(), is(OK.getStatusCode()));
    }

    /**
     * Connect with authorized user, but with wrong password. Should fail.
     */
    @Test
    public void connectWithWrongPassword() {
        Client client = RestClients.httpsBasicAuth(AUTHORIZED_CLIENT_USERNAME, "baspassword");
        Response response = client.target(getUrl()).request().get();
        assertThat(response.getStatus(), is(UNAUTHORIZED.getStatusCode()));
    }

    /**
     * Connect with unauthorized client. Should fail.
     */
    @Test
    public void connectWithUnauthorizedClient() {
        Client client = RestClients.httpsBasicAuth(UNAUTHORIZED_CLIENT_USERNAME, UNAUTHORIZED_CLIENT_PASSWORD);
        Response response = client.target(getUrl()).request().get();
        assertThat(response.getStatus(), is(UNAUTHORIZED.getStatusCode()));
    }

    /**
     * Connect with a client that is authorized but has the wrong role. Should
     * fail.
     */
    @Test
    public void connectWithWrongRoleClient() {
        Client client = RestClients.httpsBasicAuth(WRONG_ROLE_CLIENT_USERNAME, WRONG_ROLE_CLIENT_PASSWORD);
        Response response = client.target(getUrl()).request().get();
        assertThat(response.getStatus(), is(FORBIDDEN.getStatusCode()));
    }

    /**
     * URL to do a {@code GET /pool} request.
     *
     * @return
     */
    private static String getUrl() {
        return String.format("https://localhost:%d/pool", httpsPort);
    }
}
