package com.elastisys.scale.cloudpool.multipool.server;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotConfiguredException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.NotStartedException;
import com.elastisys.scale.cloudpool.api.restapi.types.DetachMachineRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetDesiredSizeRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetMembershipStatusRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.SetServiceStateRequest;
import com.elastisys.scale.cloudpool.api.restapi.types.TerminateMachineRequest;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.multipool.api.CloudPoolCreateException;
import com.elastisys.scale.cloudpool.multipool.api.CloudPoolInstance;
import com.elastisys.scale.cloudpool.multipool.api.MultiCloudPool;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.ErrorType;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.rest.client.RestClients;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Optional;
import com.google.gson.JsonObject;

/**
 * Tests that verify the behavior of the {@link MultiCloudPoolServer} REST API
 * backed by a mocked {@link MultiCloudPool}.
 */
public class TestRestApi {

    /** Web server to use throughout the tests. */
    private static Server server;
    /** Server port to use. */
    private static int httpPort;
    /** Storage dir for configurations. */
    private static final String storageDir = Paths.get("target", "multipool").toString();

    private static MultiCloudPool multiCloudPool = mock(MultiCloudPool.class);

    @BeforeClass
    public static void onSetup() throws Exception {
        List<Integer> freePorts = HostUtils.findFreePorts(1);
        httpPort = freePorts.get(0);

        MultiCloudPoolOptions options = new MultiCloudPoolOptions();
        options.httpPort = httpPort;
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
     * {@code GET /cloudpools} should list the full URL of every
     * {@link CloudPoolInstance}s.
     */
    @Test
    public void listInstances() {
        when(multiCloudPool.list()).thenReturn(asList("pool1", "pool2"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools")).request().get();

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(response.readEntity(new GenericType<List<String>>() {
        }), is(asList(url("/cloudpools/pool1"), url("/cloudpools/pool2"))));

        verify(multiCloudPool).list();
    }

    /**
     * On an internal error, a 500 (Internal Server Error) should be returned.
     */
    @Test
    public void listInstancesOnInternalError() {
        when(multiCloudPool.list()).thenThrow(new NullPointerException());

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools")).request().get();

        assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));

        verify(multiCloudPool).list();
    }

    /**
     * {@code POST /cloudpools/<name>} should return the full URL of the created
     * {@link CloudPoolInstance} int the {@code Location} header.
     */
    @Test
    public void createInstance() {
        CloudPoolInstance instance = mock(CloudPoolInstance.class);
        when(multiCloudPool.create("pool1")).thenReturn(instance);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool1")).request().post(null);
        assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        assertThat(response.getHeaderString("Location"), is(url("/cloudpools/pool1")));

        verify(multiCloudPool).create("pool1");
    }

    /**
     * An illegal cloud pool name should result in a 400 (Bad Request).
     */
    @Test
    public void createInstanceOnIllegalArgumentException() {
        when(multiCloudPool.create("pool%1")).thenThrow(new IllegalArgumentException("bad name"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool%1")).request().post(null);
        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("bad name")));

        verify(multiCloudPool).create("pool%1");
    }

    /**
     * A failure to create the {@link CloudPoolInstance} should result in a 500
     * (Internal Server Error).
     */
    @Test
    public void createInstanceOnCloudPoolCreateException() {
        when(multiCloudPool.create("pool-1")).thenThrow(new CloudPoolCreateException("something failed"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1")).request().post(null);
        assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("something failed")));

        verify(multiCloudPool).create("pool-1");
    }

    /**
     * {@code DELETE /cloudpools} should call
     * {@link MultiCloudPool#delete(String)}.
     */
    @Test
    public void deleteInstance() {
        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1")).request().delete();

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));

        verify(multiCloudPool).delete("pool-1");
    }

    /**
     * {@code GET /cloudpools/pool/config} should call
     * {@link CloudPool#getConfiguration()} on the right
     * {@link CloudPoolInstance}.
     */
    @Test
    public void getConfig() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        CloudPoolInstance instance2 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        when(multiCloudPool.get("pool-2")).thenReturn(instance2);
        when(instance1.getConfiguration()).thenReturn(Optional.of(asJson("{\"pool\": 1}")));
        when(instance2.getConfiguration()).thenReturn(Optional.of(asJson("{\"pool\": 2}")));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/config")).request().get();

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(response.readEntity(JsonObject.class), is(asJson("{\"pool\": 1}")));

        // verify that a configuration was retrieved for the right cloudpool
        verify(multiCloudPool).get("pool-1");
        verify(instance1).getConfiguration();
    }

    /**
     * {@code GET /cloudpools/pool/config} for a cloudpool instance that does
     * not exist should result in a 404 (Not Found).
     */
    @Test
    public void getConfigOnNonExistentInstance() {
        when(multiCloudPool.get("pool-1")).thenThrow(new NotFoundException("no such pool"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/config")).request().get();

        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("no such pool")));
    }

    /**
     * {@code GET /cloudpools/pool/config} for a cloudpool instance which does
     * not (yet) have a config should also result in a 404 (Not Found).
     */
    @Test
    public void getConfigOnInstanceWithoutConfig() {
        CloudPoolInstance instance = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance);
        when(instance.getConfiguration()).thenReturn(Optional.absent());

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/config")).request().get();

        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("no cloud pool configuration has been set")));
    }

    /**
     * {@code POST /cloudpools/pool/config} should call
     * {@link CloudPool#configure(JsonObject)} on the right
     * {@link CloudPoolInstance}.
     */
    @Test
    public void setConfig() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        CloudPoolInstance instance2 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        when(multiCloudPool.get("pool-2")).thenReturn(instance2);

        JsonObject config = asJson("{\"dryrun\": true}");
        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/config")).request().post(Entity.json(config));

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));

        // verify that right instance was called
        verify(multiCloudPool).get("pool-1");
        verify(instance1).configure(config);
    }

    /**
     * {@code POST /cloudpools/pool/config} for a cloudpool instance that does
     * not exist should result in a 404 (Not Found).
     */
    @Test
    public void setConfigOnNonExistentInstance() {
        when(multiCloudPool.get("pool-1")).thenThrow(new NotFoundException("no such pool"));

        JsonObject config = asJson("{\"dryrun\": true}");
        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/config")).request().post(Entity.json(config));

        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("no such pool")));
    }

    /**
     * A {@code POST /cloudpools/pool/config} with a config rejected by the
     * cloudpool instance should result in a 400 (Bad Request).
     */
    @Test
    public void setConfigRejectedByInstance() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new IllegalArgumentException("bad config")).when(instance1)
                .configure(argThat(is(any(JsonObject.class))));

        JsonObject config = asJson("{\"dryrun\": true}");
        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/config")).request().post(Entity.json(config));

        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("bad config")));
    }

    /**
     * {@code POST /cloudpools/pool/start} should call {@link CloudPool#start()}
     * on the right {@link CloudPoolInstance}.
     */
    @Test
    public void startInstance() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        CloudPoolInstance instance2 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        when(multiCloudPool.get("pool-2")).thenReturn(instance2);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/start")).request().post(null);

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));

        // verify that right instance was called
        verify(multiCloudPool).get("pool-1");
        verify(instance1).start();
    }

    /**
     * When the requested {@link CloudPoolInstance} does not exist, a 404 (Not
     * Found) response should be returned.
     */
    @Test
    public void startOnNonExistentInstance() {
        when(multiCloudPool.get("pool-1")).thenThrow(new NotFoundException("no such pool"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/start")).request().post(null);

        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("no such pool")));
    }

    /**
     * A {@link NotConfiguredException} should result in a 400 (Bad Request)
     * error.
     */
    @Test
    public void startOnNotConfiguredError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new NotConfiguredException("no conf set")).when(instance1).start();

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/start")).request().post(null);

        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("no conf set")));
    }

    /**
     * If the {@link CloudPoolInstance} throws an unexpected error, it should be
     * translated to a 500 (Internal Server Error) response.
     */
    @Test
    public void startInstanceOnUnexpectedError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new RuntimeException("weirdness!")).when(instance1).start();

        Response response = RestClients.httpNoAuth().target(url("/cloudpools/pool-1/start")).request().post(null);

        assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("weirdness!")));
    }

    /**
     * {@code POST /cloudpools/pool/stop} should call {@link CloudPool#start()}
     * on the right {@link CloudPoolInstance}.
     */
    @Test
    public void stopInstance() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        CloudPoolInstance instance2 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        when(multiCloudPool.get("pool-2")).thenReturn(instance2);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/stop")).request().post(null);

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));

        // verify that right instance was called
        verify(multiCloudPool).get("pool-1");
        verify(instance1).stop();
    }

    /**
     * When the requested {@link CloudPoolInstance} does not exist, a 404 (Not
     * Found) response should be returned.
     */
    @Test
    public void stopOnNonExistentInstance() {
        when(multiCloudPool.get("pool-1")).thenThrow(new NotFoundException("no such pool"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/stop")).request().post(null);

        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("no such pool")));
    }

    /**
     * If the {@link CloudPoolInstance} throws an unexpected error, it should be
     * translated to a 500 (Internal Server Error) response.
     */
    @Test
    public void stopInstanceOnUnexpectedError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new RuntimeException("weirdness!")).when(instance1).stop();

        Response response = RestClients.httpNoAuth().target(url("/cloudpools/pool-1/stop")).request().post(null);

        assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("weirdness!")));
    }

    /**
     * {@code GET /cloudpools/pool/status} should call
     * {@link CloudPool#getStatus()} on the right {@link CloudPoolInstance}.
     */
    @Test
    public void getInstanceStatus() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        CloudPoolInstance instance2 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        when(multiCloudPool.get("pool-2")).thenReturn(instance2);
        when(instance1.getStatus()).thenReturn(new CloudPoolStatus(true, true));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/status")).request().get();

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(response.readEntity(CloudPoolStatus.class), is(new CloudPoolStatus(true, true)));

        // verify that right instance was called
        verify(multiCloudPool).get("pool-1");
        verify(instance1).getStatus();
    }

    /**
     * When the requested {@link CloudPoolInstance} does not exist, a 404 (Not
     * Found) response should be returned.
     */
    @Test
    public void getStatusOnNonExistentInstance() {
        when(multiCloudPool.get("pool-1")).thenThrow(new NotFoundException("no such pool"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/status")).request().get();

        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("no such pool")));
    }

    /**
     * If the {@link CloudPoolInstance} throws an unexpected error, it should be
     * translated to a 500 (Internal Server Error) response.
     */
    @Test
    public void getInstanceOnUnexpectedError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new RuntimeException("weirdness!")).when(instance1).getStatus();

        Response response = RestClients.httpNoAuth().target(url("/cloudpools/pool-1/status")).request().get();

        assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("weirdness!")));
    }

    /**
     * {@code GET /cloudpools/<pool>/pool} should call
     * {@link CloudPool#getMachinePool()} on the right
     * {@link CloudPoolInstance}.
     */
    @Test
    public void getInstancePool() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        CloudPoolInstance instance2 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        when(multiCloudPool.get("pool-2")).thenReturn(instance2);
        MachinePool machinePool = MachinePool.emptyPool(UtcTime.parse("2017-01-01T12:00:00.000Z"));
        when(instance1.getMachinePool()).thenReturn(machinePool);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/pool")).request().get();

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(response.readEntity(MachinePool.class), is(machinePool));

        // verify that right instance was called
        verify(multiCloudPool).get("pool-1");
        verify(instance1).getMachinePool();
    }

    /**
     * When the requested {@link CloudPoolInstance} does not exist, a 404 (Not
     * Found) response should be returned.
     */
    @Test
    public void getPoolOnNonExistentInstance() {
        when(multiCloudPool.get("pool-1")).thenThrow(new NotFoundException("no such pool"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/pool")).request().get();

        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("no such pool")));
    }

    /**
     * A {@link NotStartedException} should result in a 400 (Bad Request) error.
     */
    @Test
    public void getPoolOnNotStartedInstance() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new NotStartedException("not started")).when(instance1).getMachinePool();

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/pool")).request().get();

        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("not started")));
    }

    /**
     * If the {@link CloudPoolInstance} throws an unexpected error, it should be
     * translated to a 500 (Internal Server Error) response.
     */
    @Test
    public void getPoolOnUnexpectedError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new RuntimeException("weirdness!")).when(instance1).getMachinePool();

        Response response = RestClients.httpNoAuth().target(url("/cloudpools/pool-1/pool")).request().get();

        assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("weirdness!")));
    }

    /**
     * If the {@link CloudPoolInstance} fails with a {@link CloudPoolException},
     * it should be translated to a 502 (Bad Gateway) response.
     */
    @Test
    public void getInstancePoolOnCloudPoolException() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        when(instance1.getMachinePool()).thenThrow(new CloudPoolException("cloud api error"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/pool")).request().get();

        assertThat(response.getStatus(), is(Status.BAD_GATEWAY.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("cloud api error")));
    }

    /**
     * {@code POST /cloudpools/<pool>/pool/size} should call
     * {@link CloudPool#setDesiredSize(int)} on the right
     * {@link CloudPoolInstance}.
     */
    @Test
    public void setDesiredSize() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        CloudPoolInstance instance2 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        when(multiCloudPool.get("pool-2")).thenReturn(instance2);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/pool/size")).request()
                .post(Entity.json(new SetDesiredSizeRequest(10)));

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));

        // verify that right instance was called
        verify(multiCloudPool).get("pool-1");
        verify(instance1).setDesiredSize(10);
    }

    /**
     * When the requested {@link CloudPoolInstance} does not exist, a 404 (Not
     * Found) response should be returned.
     */
    @Test
    public void setDesiredSizeOnNonExistentInstance() {
        when(multiCloudPool.get("pool-1")).thenThrow(new NotFoundException("no such pool"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/pool/size")).request()
                .post(Entity.json(new SetDesiredSizeRequest(10)));

        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("no such pool")));
    }

    /**
     * A {@link NotStartedException} should result in a 400 (Bad Request) error.
     */
    @Test
    public void setDesiredSizeOnNotStartedInstance() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new NotStartedException("not started")).when(instance1).setDesiredSize(10);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/pool/size")).request()
                .post(Entity.json(new SetDesiredSizeRequest(10)));

        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("not started")));
    }

    /**
     * An {@link IllegalArgumentException} should be translated to a 400 (Bad
     * Request) error.
     */
    @Test
    public void setDesiredSizeOnIllegalArgumentError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new IllegalArgumentException("unacceptable!")).when(instance1).setDesiredSize(-1);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/pool/size")).request()
                .post(Entity.json(new SetDesiredSizeRequest(-1)));

        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("unacceptable!")));
    }

    /**
     * If the {@link CloudPoolInstance} raises a {@link CloudPoolException}, it
     * should result in a 502 (Bad Gateway) error.
     */
    @Test
    public void setDesiredSizeOnCloudPoolError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new CloudPoolException("cloud api error")).when(instance1).setDesiredSize(1);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/pool/size")).request()
                .post(Entity.json(new SetDesiredSizeRequest(1)));

        assertThat(response.getStatus(), is(Status.BAD_GATEWAY.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("cloud api error")));
    }

    /**
     * If the {@link CloudPoolInstance} throws an unexpected error, it should be
     * translated to a 500 (Internal Server Error) response.
     */
    @Test
    public void setDesiredSizeOnUnexpectedError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new RuntimeException("weirdness!")).when(instance1).setDesiredSize(10);

        Response response = RestClients.httpNoAuth().target(url("/cloudpools/pool-1/pool/size")).request()
                .post(Entity.json(new SetDesiredSizeRequest(10)));

        assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("weirdness!")));
    }

    /**
     * {@code GET /cloudpools/<pool>/pool/size} should call
     * {@link CloudPool#getPoolSize()} on the right {@link CloudPoolInstance}.
     */
    @Test
    public void getPoolSize() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        CloudPoolInstance instance2 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        when(multiCloudPool.get("pool-2")).thenReturn(instance2);
        PoolSizeSummary poolSize = new PoolSizeSummary(1, 1, 1);
        when(instance1.getPoolSize()).thenReturn(poolSize);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/pool/size")).request().get();

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(response.readEntity(PoolSizeSummary.class), is(poolSize));

        // verify that right instance was called
        verify(multiCloudPool).get("pool-1");
        verify(instance1).getPoolSize();
    }

    /**
     * When the requested {@link CloudPoolInstance} does not exist, a 404 (Not
     * Found) response should be returned.
     */
    @Test
    public void getPoolSizeOnNonExistentInstance() {
        when(multiCloudPool.get("pool-1")).thenThrow(new NotFoundException("no such pool"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/pool/size")).request().get();

        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("no such pool")));
    }

    /**
     * A {@link NotStartedException} should result in a 400 (Bad Request) error.
     */
    @Test
    public void getPoolSizeOnNotStartedInstance() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new NotStartedException("not started")).when(instance1).getPoolSize();

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/pool/size")).request().get();

        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("not started")));
    }

    /**
     * If the {@link CloudPoolInstance} raises a {@link CloudPoolException}, it
     * should result in a 502 (Bad Gateway) error.
     */
    @Test
    public void getPoolSizeOnCloudPoolError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new CloudPoolException("cloud api error")).when(instance1).getPoolSize();

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/pool/size")).request().get();

        assertThat(response.getStatus(), is(Status.BAD_GATEWAY.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("cloud api error")));
    }

    /**
     * If the {@link CloudPoolInstance} throws an unexpected error, it should be
     * translated to a 500 (Internal Server Error) response.
     */
    @Test
    public void getPoolSizeOnUnexpectedError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new RuntimeException("weirdness!")).when(instance1).getPoolSize();

        Response response = RestClients.httpNoAuth().target(url("/cloudpools/pool-1/pool/size")).request().get();

        assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("weirdness!")));
    }

    /**
     * {@code POST /cloudpools/<pool>/<machine>/terminate} should call
     * {@link CloudPool#terminateMachine(String, boolean)} on the right
     * {@link CloudPoolInstance}.
     */
    @Test
    public void terminateMachine() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        CloudPoolInstance instance2 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        when(multiCloudPool.get("pool-2")).thenReturn(instance2);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/terminate")).request()
                .post(Entity.json(new TerminateMachineRequest(true)));

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));

        // verify that right instance was called
        verify(multiCloudPool).get("pool-1");
        verify(instance1).terminateMachine("vm1", true);
    }

    /**
     * When the requested {@link CloudPoolInstance} does not exist, a 404 (Not
     * Found) response should be returned.
     */
    @Test
    public void terminateMachineOnNonExistentInstance() {
        when(multiCloudPool.get("pool-1")).thenThrow(new NotFoundException("no such pool"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/terminate")).request()
                .post(Entity.json(new TerminateMachineRequest(true)));

        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("no such pool")));
    }

    /**
     * A {@link NotStartedException} should result in a 400 (Bad Request) error.
     */
    @Test
    public void terminateMachineOnNotStartedInstance() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new NotStartedException("not started")).when(instance1).terminateMachine("vm1", true);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/terminate")).request()
                .post(Entity.json(new TerminateMachineRequest(true)));

        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("not started")));
    }

    /**
     * If the {@link CloudPoolInstance} raises a {@link CloudPoolException}, it
     * should result in a 502 (Bad Gateway) error.
     */
    @Test
    public void terminateMachineOnCloudPoolError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new CloudPoolException("cloud api error")).when(instance1).terminateMachine("vm1", true);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/terminate")).request()
                .post(Entity.json(new TerminateMachineRequest(true)));

        assertThat(response.getStatus(), is(Status.BAD_GATEWAY.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("cloud api error")));
    }

    /**
     * If the {@link CloudPoolInstance} throws an unexpected error, it should be
     * translated to a 500 (Internal Server Error) response.
     */
    @Test
    public void terminateMachineOnUnexpectedError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new RuntimeException("weirdness!")).when(instance1).terminateMachine("vm1", true);

        Response response = RestClients.httpNoAuth().target(url("/cloudpools/pool-1/vm1/terminate")).request()
                .post(Entity.json(new TerminateMachineRequest(true)));

        assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("weirdness!")));
    }

    /**
     * {@code POST /cloudpools/<pool>/<machine>/detach} should call
     * {@link CloudPool#detachMachine(String, boolean)} on the right
     * {@link CloudPoolInstance}.
     */
    @Test
    public void detachMachine() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        CloudPoolInstance instance2 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        when(multiCloudPool.get("pool-2")).thenReturn(instance2);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/detach")).request()
                .post(Entity.json(new DetachMachineRequest(true)));

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));

        // verify that right instance was called
        verify(multiCloudPool).get("pool-1");
        verify(instance1).detachMachine("vm1", true);
    }

    /**
     * When the requested {@link CloudPoolInstance} does not exist, a 404 (Not
     * Found) response should be returned.
     */
    @Test
    public void detachMachineOnNonExistentInstance() {
        when(multiCloudPool.get("pool-1")).thenThrow(new NotFoundException("no such pool"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/detach")).request()
                .post(Entity.json(new DetachMachineRequest(true)));

        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("no such pool")));
    }

    /**
     * A {@link NotStartedException} should result in a 400 (Bad Request) error.
     */
    @Test
    public void detachMachineOnNotStartedInstance() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new NotStartedException("not started")).when(instance1).detachMachine("vm1", true);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/detach")).request()
                .post(Entity.json(new DetachMachineRequest(true)));

        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("not started")));
    }

    /**
     * If the {@link CloudPoolInstance} raises a {@link CloudPoolException}, it
     * should result in a 502 (Bad Gateway) error.
     */
    @Test
    public void detachMachineOnCloudPoolError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new CloudPoolException("cloud api error")).when(instance1).detachMachine("vm1", true);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/detach")).request()
                .post(Entity.json(new DetachMachineRequest(true)));

        assertThat(response.getStatus(), is(Status.BAD_GATEWAY.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("cloud api error")));
    }

    /**
     * If the {@link CloudPoolInstance} throws an unexpected error, it should be
     * translated to a 500 (Internal Server Error) response.
     */
    @Test
    public void detachMachineOnUnexpectedError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new RuntimeException("weirdness!")).when(instance1).detachMachine("vm1", true);

        Response response = RestClients.httpNoAuth().target(url("/cloudpools/pool-1/vm1/detach")).request()
                .post(Entity.json(new DetachMachineRequest(true)));

        assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("weirdness!")));
    }

    /**
     * {@code POST /cloudpools/<pool>/<machine>/attach} should call
     * {@link CloudPool#attachMachine(String)} on the right
     * {@link CloudPoolInstance}.
     */
    @Test
    public void attachMachine() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        CloudPoolInstance instance2 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        when(multiCloudPool.get("pool-2")).thenReturn(instance2);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/attach")).request().post(null);

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));

        // verify that right instance was called
        verify(multiCloudPool).get("pool-1");
        verify(instance1).attachMachine("vm1");
    }

    /**
     * When the requested {@link CloudPoolInstance} does not exist, a 404 (Not
     * Found) response should be returned.
     */
    @Test
    public void attachMachineOnNonExistentInstance() {
        when(multiCloudPool.get("pool-1")).thenThrow(new NotFoundException("no such pool"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/attach")).request().post(null);

        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("no such pool")));
    }

    /**
     * A {@link NotStartedException} should result in a 400 (Bad Request) error.
     */
    @Test
    public void attachMachineOnNotStartedInstance() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new NotStartedException("not started")).when(instance1).attachMachine("vm1");

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/attach")).request().post(null);

        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("not started")));
    }

    /**
     * If the {@link CloudPoolInstance} raises a {@link CloudPoolException}, it
     * should result in a 502 (Bad Gateway) error.
     */
    @Test
    public void attachMachineOnCloudPoolError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new CloudPoolException("cloud api error")).when(instance1).attachMachine("vm1");

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/attach")).request().post(null);

        assertThat(response.getStatus(), is(Status.BAD_GATEWAY.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("cloud api error")));
    }

    /**
     * If the {@link CloudPoolInstance} throws an unexpected error, it should be
     * translated to a 500 (Internal Server Error) response.
     */
    @Test
    public void attachMachineOnUnexpectedError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new RuntimeException("weirdness!")).when(instance1).attachMachine("vm1");

        Response response = RestClients.httpNoAuth().target(url("/cloudpools/pool-1/vm1/attach")).request().post(null);

        assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("weirdness!")));
    }

    /**
     * {@code POST /cloudpools/<pool>/<machine>/serviceState} should call
     * {@link CloudPool#setServiceState(String, com.elastisys.scale.cloudpool.api.types.ServiceState)}
     * on the right {@link CloudPoolInstance}.
     */
    @Test
    public void setServiceState() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        CloudPoolInstance instance2 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        when(multiCloudPool.get("pool-2")).thenReturn(instance2);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/serviceState")).request()
                .post(Entity.json(new SetServiceStateRequest(ServiceState.IN_SERVICE)));

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));

        // verify that right instance was called
        verify(multiCloudPool).get("pool-1");
        verify(instance1).setServiceState("vm1", ServiceState.IN_SERVICE);
    }

    /**
     * When the requested {@link CloudPoolInstance} does not exist, a 404 (Not
     * Found) response should be returned.
     */
    @Test
    public void setServiceStateOnNonExistentInstancer() {
        when(multiCloudPool.get("pool-1")).thenThrow(new NotFoundException("no such pool"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/serviceState")).request()
                .post(Entity.json(new SetServiceStateRequest(ServiceState.IN_SERVICE)));

        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("no such pool")));
    }

    /**
     * A {@link NotStartedException} should result in a 400 (Bad Request) error.
     */
    @Test
    public void setServiceStateOnNotStartedInstance() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new NotStartedException("not started")).when(instance1).setServiceState("vm1", ServiceState.BOOTING);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/serviceState")).request()
                .post(Entity.json(new SetServiceStateRequest(ServiceState.BOOTING)));

        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("not started")));
    }

    /**
     * If the {@link CloudPoolInstance} raises a {@link CloudPoolException}, it
     * should result in a 502 (Bad Gateway) error.
     */
    @Test
    public void setServiceStateOnCloudPoolError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new CloudPoolException("cloud api error")).when(instance1).setServiceState("vm1", ServiceState.BOOTING);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/serviceState")).request()
                .post(Entity.json(new SetServiceStateRequest(ServiceState.BOOTING)));

        assertThat(response.getStatus(), is(Status.BAD_GATEWAY.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("cloud api error")));
    }

    /**
     * If the {@link CloudPoolInstance} throws an unexpected error, it should be
     * translated to a 500 (Internal Server Error) response.
     */
    @Test
    public void serServiceStateOnUnexpectedError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new RuntimeException("weirdness!")).when(instance1).setServiceState("vm1", ServiceState.BOOTING);

        Response response = RestClients.httpNoAuth().target(url("/cloudpools/pool-1/vm1/serviceState")).request()
                .post(Entity.json(new SetServiceStateRequest(ServiceState.BOOTING)));

        assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("weirdness!")));
    }

    /**
     * {@code POST /cloudpools/<pool>/<machine>/membershipStatus} should call
     * {@link CloudPool#setMembershipStatus(String, com.elastisys.scale.cloudpool.api.types.MembershipStatus)}
     * on the right {@link CloudPoolInstance}.
     */
    @Test
    public void setMembershipStatus() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        CloudPoolInstance instance2 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        when(multiCloudPool.get("pool-2")).thenReturn(instance2);

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/membershipStatus")).request()
                .post(Entity.json(new SetMembershipStatusRequest(MembershipStatus.awaitingService())));

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));

        // verify that right instance was called
        verify(multiCloudPool).get("pool-1");
        verify(instance1).setMembershipStatus("vm1", MembershipStatus.awaitingService());
    }

    /**
     * When the requested {@link CloudPoolInstance} does not exist, a 404 (Not
     * Found) response should be returned.
     */
    @Test
    public void setMembershipStatusOnNonExistentInstance() {
        when(multiCloudPool.get("pool-1")).thenThrow(new NotFoundException("no such pool"));

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/membershipStatus")).request()
                .post(Entity.json(new SetMembershipStatusRequest(MembershipStatus.blessed())));

        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("no such pool")));
    }

    /**
     * A {@link NotStartedException} should result in a 400 (Bad Request) error.
     */
    @Test
    public void setMembershipStatusOnNotStartedInstance() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new NotStartedException("not started")).when(instance1).setMembershipStatus("vm1",
                MembershipStatus.blessed());

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/membershipStatus")).request()
                .post(Entity.json(new SetMembershipStatusRequest(MembershipStatus.blessed())));

        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("not started")));
    }

    /**
     * If the {@link CloudPoolInstance} raises a {@link CloudPoolException}, it
     * should result in a 502 (Bad Gateway) error.
     */
    @Test
    public void setMembershipStatusOnCloudPoolError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new CloudPoolException("cloud api error")).when(instance1).setMembershipStatus("vm1",
                MembershipStatus.blessed());

        Client client = RestClients.httpNoAuth();
        Response response = client.target(url("/cloudpools/pool-1/vm1/membershipStatus")).request()
                .post(Entity.json(new SetMembershipStatusRequest(MembershipStatus.blessed())));

        assertThat(response.getStatus(), is(Status.BAD_GATEWAY.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("cloud api error")));
    }

    /**
     * If the {@link CloudPoolInstance} throws an unexpected error, it should be
     * translated to a 500 (Internal Server Error) response.
     */
    @Test
    public void serMembershipStatusOnUnexpectedError() {
        CloudPoolInstance instance1 = mock(CloudPoolInstance.class);
        when(multiCloudPool.get("pool-1")).thenReturn(instance1);
        doThrow(new RuntimeException("weirdness!")).when(instance1).setMembershipStatus("vm1",
                MembershipStatus.blessed());

        Response response = RestClients.httpNoAuth().target(url("/cloudpools/pool-1/vm1/membershipStatus")).request()
                .post(Entity.json(new SetMembershipStatusRequest(MembershipStatus.blessed())));

        assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.readEntity(ErrorType.class), is(new ErrorType("weirdness!")));
    }

    private JsonObject asJson(String jsonAsString) {
        return JsonUtils.parseJsonString(jsonAsString).getAsJsonObject();
    }

    /**
     * URL to do a {@code GET /<path>} request.
     *
     * @param path
     *            The resource path on the remote server.
     * @return
     */
    private static String url(String path) {
        return String.format("http://localhost:%d%s", httpPort, path);
    }

}
