package com.elastisys.scale.cloudpool.api.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jetty.server.Server;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.ApiVersion;
import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.NotConfiguredException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.server.CloudPoolOptions;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.api.types.CloudPoolMetadata;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.google.gson.JsonObject;

/**
 * Exercises the {@link CloudPoolClient} against a {@link CloudPool} server with
 * a mocked back-end.
 */
public class TestCloudPoolClient {
    private static final String SERVER_KEYSTORE = Resources.getResource("security/server/server_keystore.p12")
            .toString();
    private static final String SERVER_KEYSTORE_PASSWORD = "serverpass";

    /** Web server to use throughout the tests. */
    private static Server server;
    /** Server port to use for HTTPS. */
    private static int httpsPort;
    /** Storage dir for configurations. */
    private static final String storageDir = Paths.get("target", "cloudpool", "storage").toString();

    private static CloudPool cloudPool = mock(CloudPool.class);

    /** Object under test. */
    private static CloudPoolClient client;

    @BeforeClass
    public static void onSetup() throws Exception {
        List<Integer> freePorts = HostUtils.findFreePorts(1);
        httpsPort = freePorts.get(0);

        client = new CloudPoolClient(new AuthenticatedHttpClient(), "localhost", httpsPort);

        CloudPoolOptions options = new CloudPoolOptions();
        options.httpsPort = httpsPort;
        options.sslKeyStore = SERVER_KEYSTORE;
        options.sslKeyStorePassword = SERVER_KEYSTORE_PASSWORD;
        options.requireClientCert = false;
        options.storageDir = storageDir;

        server = CloudPoolServer.createServer(cloudPool, options);
        server.start();
    }

    @Before
    public void beforeTestMethod() {
        reset(cloudPool);
        // unless otherwise specified by test methods, cloudpool is configured
        // and started
        CloudPoolStatus startedStatus = new CloudPoolStatus(true, true);
        when(cloudPool.getStatus()).thenReturn(startedStatus);
    }

    @AfterClass
    public static void onTeardown() throws Exception {
        server.stop();
        server.join();
    }

    @Test
    public void configure() {
        JsonObject config = JsonUtils.parseJsonString("{\"key\": \"value\"}").getAsJsonObject();
        client.configure(config);

        // verify that call was made to cloudpool backend
        verify(cloudPool).configure(config);
        verifyNoMoreInteractions(cloudPool);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithIllegalConfig() {
        JsonObject config = JsonUtils.parseJsonString("{\"key\": \"value\"}").getAsJsonObject();

        // cloudpool set up to reject config
        doThrow(new IllegalArgumentException("bad config!")).when(cloudPool).configure(config);

        client.configure(config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithNullConfig() {
        client.configure(null);
    }

    @Test
    public void getConfig() {
        JsonObject config = JsonUtils.parseJsonString("{\"key\": \"value\"}").getAsJsonObject();
        when(cloudPool.getConfiguration()).thenReturn(Optional.of(config));

        assertThat(client.getConfiguration(), is(Optional.of(config)));
    }

    @Test
    public void getConfigWhenNoneIsSet() {
        Optional<JsonObject> missingConfig = Optional.absent();
        when(cloudPool.getConfiguration()).thenReturn(missingConfig);

        assertThat(client.getConfiguration(), is(missingConfig));
    }

    @Test
    public void start() {
        client.start();

        // verify that call was made to cloudpool backend
        verify(cloudPool).start();
        verifyNoMoreInteractions(cloudPool);
    }

    @Test(expected = NotConfiguredException.class)
    public void startBeforeConfigured() {
        doThrow(new NotConfiguredException("no config set!")).when(cloudPool).start();

        client.start();
    }

    @Test
    public void stop() {
        client.stop();

        // verify that call was made to cloudpool backend
        verify(cloudPool).stop();
        verifyNoMoreInteractions(cloudPool);
    }

    @Test
    public void getStatus() {
        when(cloudPool.getStatus()).thenReturn(new CloudPoolStatus(false, true));

        assertThat(client.getStatus(), is(new CloudPoolStatus(false, true)));
    }

    @Test
    public void getMachinePool() {
        MachinePool pool = MachinePool.emptyPool(UtcTime.now());
        when(cloudPool.getMachinePool()).thenReturn(pool);

        assertThat(client.getMachinePool(), is(pool));
    }

    @Test
    public void getPoolSize() {
        DateTime time = UtcTime.parse("2015-01-01T12:00:00.000Z");
        when(cloudPool.getPoolSize()).thenReturn(new PoolSizeSummary(time, 3, 2, 1));

        assertThat(client.getPoolSize(), is(new PoolSizeSummary(time, 3, 2, 1)));
    }

    @Test
    public void setDesiredSize() {
        client.setDesiredSize(10);

        // verify that call was made to cloudpool backend
        verify(cloudPool).setDesiredSize(10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setDesiredSizeWithIllegalInput() {
        doThrow(new IllegalArgumentException("negative value!")).when(cloudPool).setDesiredSize(-1);

        client.setDesiredSize(-1);
    }

    @Test
    public void terminateMachine() {
        client.terminateMachine("i-1", true);

        // verify that call was made to cloudpool backend
        verify(cloudPool).terminateMachine("i-1", true);
    }

    @Test(expected = NotFoundException.class)
    public void terminateInvalidMachine() {
        doThrow(new NotFoundException("not recognized!")).when(cloudPool).terminateMachine("i-X", true);

        client.terminateMachine("i-X", true);
    }

    @Test
    public void setServiceStateMachine() {
        client.setServiceState("i-1", ServiceState.UNHEALTHY);

        // verify that call was made to cloudpool backend
        verify(cloudPool).setServiceState("i-1", ServiceState.UNHEALTHY);
    }

    @Test(expected = NotFoundException.class)
    public void setServiceStateOnInvalidMachine() {
        doThrow(new NotFoundException("not recognized!")).when(cloudPool).setServiceState("i-X",
                ServiceState.OUT_OF_SERVICE);

        client.setServiceState("i-X", ServiceState.OUT_OF_SERVICE);
    }

    @Test
    public void setMembershipStatus() {
        client.setMembershipStatus("i-1", MembershipStatus.blessed());

        // verify that call was made to cloudpool backend
        verify(cloudPool).setMembershipStatus("i-1", MembershipStatus.blessed());
    }

    @Test(expected = NotFoundException.class)
    public void setMembershipStatusOnInvalidMachine() {
        doThrow(new NotFoundException("not recognized!")).when(cloudPool).setMembershipStatus("i-X",
                MembershipStatus.blessed());

        client.setMembershipStatus("i-X", MembershipStatus.blessed());
    }

    @Test
    public void attachMachine() {
        client.attachMachine("i-1");

        // verify that call was made to cloudpool backend
        verify(cloudPool).attachMachine("i-1");
    }

    @Test(expected = NotFoundException.class)
    public void attachInvalidMachine() {
        doThrow(new NotFoundException("not recognized!")).when(cloudPool).attachMachine("i-X");

        client.attachMachine("i-X");
    }

    @Test
    public void detachMachine() {
        client.detachMachine("i-1", false);

        // verify that call was made to cloudpool backend
        verify(cloudPool).detachMachine("i-1", false);
    }

    @Test(expected = NotFoundException.class)
    public void detachInvalidMachine() {
        doThrow(new NotFoundException("not recognized!")).when(cloudPool).detachMachine("i-X", false);

        client.detachMachine("i-X", false);
    }

    @Test
    public void getMetadata() {
        CloudPoolMetadata metadata = new CloudPoolMetadata("AWS/EC2", Arrays.asList(ApiVersion.LATEST));
        when(cloudPool.getMetadata()).thenReturn(metadata);

        assertThat(client.getMetadata(), is(metadata));
    }
}
