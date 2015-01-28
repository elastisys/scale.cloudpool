package com.elastisys.scale.cloudadapters.api.server;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import jersey.repackaged.com.google.common.collect.Lists;

import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.server.CloudAdapterOptions;
import com.elastisys.scale.cloudadapers.api.server.CloudAdapterServer;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.rest.client.RestClients;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.google.gson.JsonObject;

/**
 * Tests that verify the behavior of the {@link CloudAdapterServer} REST API
 * backed by a mocked {@link CloudAdapter}.
 */
public class TestRestApi {

	private static final String SERVER_KEYSTORE = Resources.getResource(
			"security/server/server_keystore.p12").toString();
	private static final String SERVER_KEYSTORE_PASSWORD = "serverpass";

	/** Web server to use throughout the tests. */
	private static Server server;
	/** Server port to use for HTTPS. */
	private static int httpsPort;

	private static CloudAdapter cloudAdapter = mock(CloudAdapter.class);

	@BeforeClass
	public static void onSetup() throws Exception {
		List<Integer> freePorts = HostUtils.findFreePorts(1);
		httpsPort = freePorts.get(0);

		CloudAdapterOptions options = new CloudAdapterOptions();
		options.httpsPort = httpsPort;
		options.sslKeyStore = SERVER_KEYSTORE;
		options.sslKeyStorePassword = SERVER_KEYSTORE_PASSWORD;
		options.requireClientCert = false;
		options.enableConfigHandler = true;

		server = CloudAdapterServer.createServer(cloudAdapter, options);
		server.start();
	}

	@Before
	public void beforeTestMethod() {
		reset(cloudAdapter);
	}

	@AfterClass
	public static void onTeardown() throws Exception {
		server.stop();
		server.join();
	}

	/**
	 * Verifies a {@code 200} response on a successful {@code GET /config}.
	 */
	@Test
	public void testGetConfig() throws IOException {
		// set up mocked cloud adapter response
		String configDoc = "{\"setting\": \"true\"}";
		Optional<JsonObject> config = Optional.of(JsonUtils
				.parseJsonString(configDoc));
		when(cloudAdapter.getConfiguration()).thenReturn(config);

		// run test
		Client client = RestClients.httpsNoAuth();
		Response response = client.target(getUrl("/config")).request().get();
		assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
		assertNotNull(response.readEntity(JsonObject.class));

		// verify dispatch from REST server to cloud adapter
		verify(cloudAdapter).getConfiguration();
		verifyNoMoreInteractions(cloudAdapter);
	}

	/**
	 * Verifies that a {@code GET /config} request against a cloud adapter that
	 * haven't had its configuration set results in a {@code 404} (Not Found)
	 * error response.
	 */
	@Test
	public void testGetConfigBeforeSet() throws IOException {
		// set up mocked cloud adapter response
		Optional<JsonObject> config = Optional.absent();
		when(cloudAdapter.getConfiguration()).thenReturn(config);

		Client client = RestClients.httpsNoAuth();
		Response response = client.target(getUrl("/config")).request().get();
		assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
		assertNotNull(response.readEntity(JsonObject.class));
	}

	/**
	 * An unexpected cloud adapter error on {@code GET /config} should give a
	 * {@code 500} response.
	 */
	@Test
	public void testGetConfigOnCloudAdapterError() throws IOException {
		// set up mocked cloud adapter response
		doThrow(new IllegalStateException("something went wrong!")).when(
				cloudAdapter).getConfiguration();

		Client client = RestClients.httpsNoAuth();
		Response response = client.target(getUrl("/config")).request().get();
		assertThat(response.getStatus(),
				is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
		assertThat(response.readEntity(String.class),
				containsString("something went wrong"));
	}

	/**
	 * Verifies a {@code 200} response on a successful {@code POST /config}.
	 */
	@Test
	public void testPostConfig() throws IOException {
		// set up mocked cloud adapter response
		doNothing().when(cloudAdapter)
				.configure(Matchers.any(JsonObject.class));

		// do test
		String configDoc = "{\"setting\": \"true\"}";
		JsonObject config = JsonUtils.parseJsonString(configDoc);
		Client client = RestClients.httpsNoAuth();
		Response response = client.target(getUrl("/config"))
				.request(MediaType.APPLICATION_JSON).post(Entity.json(config));
		assertThat(response.getStatus(), is(Status.OK.getStatusCode()));

		// verify dispatch from REST server to cloud adapter
		verify(cloudAdapter).configure(Matchers.any(JsonObject.class));
		verifyNoMoreInteractions(cloudAdapter);
	}

	/**
	 * An unexpected cloud adapter error on a {@code POST /config} should give a
	 * {@code 500} response.
	 */
	@Test
	public void testPostConfigOnCloudAdapterError() throws IOException {
		// set up mocked cloud adapter response
		doThrow(new IllegalStateException("something went wrong!")).when(
				cloudAdapter).configure(Matchers.any(JsonObject.class));

		String configDoc = "{\"setting\": \"true\"}";
		JsonObject config = JsonUtils.parseJsonString(configDoc);
		Client client = RestClients.httpsNoAuth();
		Response response = client.target(getUrl("/config"))
				.request(MediaType.APPLICATION_JSON).post(Entity.json(config));
		assertThat(response.getStatus(),
				is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
	}

	/**
	 * Verifies a {@code 200} response on a successful {@code GET /config}.
	 */
	@Test
	public void testGetConfigSchema() throws IOException {
		// set up mocked cloud adapter response
		String schemaDoc = "{\"setting\": \"true\"}";
		Optional<JsonObject> schema = Optional.of(JsonUtils
				.parseJsonString(schemaDoc));
		when(cloudAdapter.getConfigurationSchema()).thenReturn(schema);

		// run test
		Client client = RestClients.httpsNoAuth();
		Response response = client.target(getUrl("/config/schema")).request()
				.get();
		assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
		assertThat(response.readEntity(JsonObject.class), is(schema.get()));

		// verify dispatch from REST server to cloud adapter
		verify(cloudAdapter).getConfigurationSchema();
		verifyNoMoreInteractions(cloudAdapter);
	}

	/**
	 * An unexpected cloud adapter error on {@code GET /config/schema} should
	 * give a {@code 500} response.
	 */
	@Test
	public void testGetConfigSchemaOnCloudAdapterError() throws IOException {
		// set up mocked cloud adapter response
		doThrow(new IllegalStateException("something went wrong!")).when(
				cloudAdapter).getConfigurationSchema();

		Client client = RestClients.httpsNoAuth();
		Response response = client.target(getUrl("/config/schema")).request()
				.get();
		assertThat(response.getStatus(),
				is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
		assertThat(response.readEntity(String.class),
				containsString("something went wrong"));
	}

	/**
	 * Verifies a {@code 200} response on a successful {@code GET /pool}.
	 */
	@Test
	public void testGetPool() {
		// set up mocked cloud adapter response
		List<Machine> machines = Lists.newArrayList();
		machines.add(new Machine("i-1", MachineState.RUNNING, UtcTime.now()
				.minusHours(1), Arrays.asList("1.2.3.4"), null, null));
		MachinePool pool = new MachinePool(machines, UtcTime.now());
		when(cloudAdapter.getMachinePool()).thenReturn(pool);

		// run test
		Client client = RestClients.httpsNoAuth();
		Response response = client.target(getUrl("/pool")).request().get();
		assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
		MachinePool receivedPool = JsonUtils.toObject(
				response.readEntity(JsonObject.class), MachinePool.class);
		assertThat(receivedPool, is(pool));

		// verify dispatch from REST server to cloud adapter
		verify(cloudAdapter).getMachinePool();
		verifyNoMoreInteractions(cloudAdapter);
	}

	/**
	 * An unexpected cloud adapter error on {@code GET /pool} should give a
	 * {@code 500} response.
	 */
	@Test
	public void testGetPoolOnCloudAdapterError() throws IOException {
		// set up mocked cloud adapter response
		doThrow(new IllegalStateException("something went wrong!")).when(
				cloudAdapter).getMachinePool();

		Client client = RestClients.httpsNoAuth();
		Response response = client.target(getUrl("/pool")).request().get();
		assertThat(response.getStatus(),
				is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
		assertThat(response.readEntity(String.class),
				containsString("something went wrong"));
	}

	/**
	 * Verifies a {@code 200} response on a successful {@code POST /pool} resize
	 * request.
	 */
	@Test
	public void testPostPool() {
		// set up expectated call on mock
		doNothing().when(cloudAdapter).resizeMachinePool(15);

		// run test
		Client client = RestClients.httpsNoAuth();
		Entity<String> request = Entity.json("{\"desiredCapacity\": 15}");
		Response response = client.target(getUrl("/pool")).request()
				.post(request);
		assertThat(response.getStatus(), is(Status.OK.getStatusCode()));

		// verify dispatch from REST server to cloud adapter
		verify(cloudAdapter).resizeMachinePool(15);
		verifyNoMoreInteractions(cloudAdapter);
	}

	/**
	 * An unexpected cloud adapter error on a {@code POST /pool} resize request
	 * should give a {@code 500} response.
	 */
	@Test
	public void testPostPoolOnCloudAdapterError() {
		// set up mocked cloud adapter response
		doThrow(new IllegalStateException("something went wrong!")).when(
				cloudAdapter).resizeMachinePool(15);

		// run test
		Client client = RestClients.httpsNoAuth();
		Entity<String> request = Entity.json("{\"desiredCapacity\": 15}");
		Response response = client.target(getUrl("/pool")).request()
				.post(request);
		assertThat(response.getStatus(),
				is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
		assertThat(response.readEntity(String.class),
				containsString("something went wrong"));
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
