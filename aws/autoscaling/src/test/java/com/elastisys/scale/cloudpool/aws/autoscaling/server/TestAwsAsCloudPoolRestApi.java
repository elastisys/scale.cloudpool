package com.elastisys.scale.cloudpool.aws.autoscaling.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolOptions;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.AwsAsPoolDriver;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.client.AwsAutoScalingClient;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.rest.client.RestClients;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.elastisys.scale.commons.util.io.Resources;
import com.google.gson.JsonObject;

/**
 * Verifies some basic properties of the cloud pool's REST API.
 */
public class TestAwsAsCloudPoolRestApi {

    private static final String SERVER_KEYSTORE = Resources.getResource("security/server/server_keystore.p12")
            .toString();
    private static final String SERVER_KEYSTORE_PASSWORD = "serverpass";

    private static final File STATE_STORAGE_DIR = new File("target/state");
    private static final StateStorage STATE_STORAGE = StateStorage.builder(STATE_STORAGE_DIR).build();

    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
    /** Web server to use throughout the tests. */
    private static Server server;
    /** Server port to use for HTTPS. */
    private static int httpsPort;
    private final static String storageDir = "target/test/cloudpool/storage";
    private static CloudPool cloudPool;

    @BeforeClass
    public static void onSetup() throws Exception {
        FileUtils.deleteRecursively(STATE_STORAGE_DIR);

        List<Integer> freePorts = HostUtils.findFreePorts(1);
        httpsPort = freePorts.get(0);

        cloudPool = new BaseCloudPool(STATE_STORAGE, new AwsAsPoolDriver(new AwsAutoScalingClient()), executor);

        CloudPoolOptions options = new CloudPoolOptions();
        options.httpsPort = httpsPort;
        options.sslKeyStore = SERVER_KEYSTORE;
        options.sslKeyStorePassword = SERVER_KEYSTORE_PASSWORD;
        options.requireClientCert = false;
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
     * Verifies that a {@code GET /config} request against the cloud pool before
     * it has had its configuration set results in a {@code 404} (Not Found)
     * error response.
     * <p/>
     * At one time, this caused a bug due to conflicting versions of the Jackson
     * library (pulled in by Jersey and aws-sdk, respectively).
     */
    @Test
    public void testGetConfigBeforeSet() throws IOException {
        Client client = RestClients.httpsNoAuth();
        Response response = client.target(getUrl("/config")).request().get();
        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertNotNull(response.readEntity(JsonObject.class));
    }

    /**
     * URL to do a {@code GET /pool} request.
     *
     * @param path
     *            The resource path on the remote server.
     * @return
     */
    private static String getUrl(String path) {
        return String.format("https://localhost:%d%s", httpsPort, path);
    }

}
