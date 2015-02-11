package com.elastisys.scale.cloudadapters.splitter.requests;

import static com.elastisys.scale.cloudadapters.splitter.testutils.TestUtils.machine;
import static com.elastisys.scale.cloudadapters.splitter.testutils.TestUtils.pool;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.http.client.HttpResponseException;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.server.CloudAdapterOptions;
import com.elastisys.scale.cloudadapers.api.server.CloudAdapterServer;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapers.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudadapers.api.types.ServiceState;
import com.elastisys.scale.cloudadapters.splitter.config.PrioritizedCloudAdapter;
import com.elastisys.scale.cloudadapters.splitter.requests.http.HttpRequestFactory;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.google.common.io.Resources;

/**
 * Exercises the requests produced the {@link HttpRequestFactory} against a fake
 * cloud adapter server.
 * <p/>
 * All tests are performed with neither client using nor server requiring
 * authentication.
 */
public class TestHttpRequestFactory {
	private static final String SERVER_KEYSTORE = Resources.getResource(
			"security/server/server_keystore.p12").toString();
	private static final String SERVER_KEYSTORE_PASSWORD = "serverpass";

	/** Web server to use throughout the tests. */
	private static Server server;
	/** Server port to use for HTTPS. */
	private static int httpsPort;

	private static CloudAdapter cloudAdapterMock = mock(CloudAdapter.class);

	/**
	 * Starts up a cloud adapter server running with the cloud adapter mock
	 * prior to running the test methods.
	 */
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

		server = CloudAdapterServer.createServer(cloudAdapterMock, options);
		server.start();
	}

	/**
	 * Reset cloud adapter mock between every test method.
	 */
	@Before
	public void beforeTestMethod() {
		reset(cloudAdapterMock);
	}

	/**
	 * Stop the server when all test methods are done.
	 */
	@AfterClass
	public static void onTeardown() throws Exception {
		server.stop();
		server.join();
	}

	@Test
	public void getMachinePoolRequest() throws Exception {
		// prepare mock to receive call
		MachinePool pool = pool(machine(1));
		when(cloudAdapterMock.getMachinePool()).thenReturn(pool);

		// call cloud adapter
		PrioritizedCloudAdapter cloudAdapter = prioritizedCloudAdapter(100,
				"localhost", httpsPort);
		Callable<MachinePool> request = new HttpRequestFactory()
				.newGetMachinePoolRequest(cloudAdapter);
		assertThat(request.call(), is(pool));

		// verify that mock received call
		verify(cloudAdapterMock).getMachinePool();
		verifyNoMoreInteractions(cloudAdapterMock);
	}

	@Test
	public void getMachinePoolRequestOnError() throws Exception {
		// prepare mock to fail
		doThrow(new CloudAdapterException("something went wrong!")).when(
				cloudAdapterMock).getMachinePool();

		// call cloud adapter
		PrioritizedCloudAdapter cloudAdapter = prioritizedCloudAdapter(100,
				"localhost", httpsPort);
		Callable<MachinePool> request = new HttpRequestFactory()
				.newGetMachinePoolRequest(cloudAdapter);
		try {
			request.call();
			fail("call should fail!");
		} catch (CloudAdapterException e) {
			Throwable cause = e.getCause();
			assertThat(cause, is(instanceOf(HttpResponseException.class)));
			assertThat(HttpResponseException.class.cast(cause).getStatusCode(),
					is(500));
		}
	}

	@Test
	public void getPoolSizeRequest() throws Exception {
		// prepare mock to receive call
		PoolSizeSummary poolSize = new PoolSizeSummary(2, 3, 1);
		when(cloudAdapterMock.getPoolSize()).thenReturn(poolSize);

		// call cloud adapter
		PrioritizedCloudAdapter cloudAdapter = prioritizedCloudAdapter(100,
				"localhost", httpsPort);
		Callable<PoolSizeSummary> request = new HttpRequestFactory()
				.newGetPoolSizeRequest(cloudAdapter);
		assertThat(request.call(), is(poolSize));

		// verify that mock received call
		verify(cloudAdapterMock).getPoolSize();
		verifyNoMoreInteractions(cloudAdapterMock);
	}

	@Test
	public void getPoolSizeRequestOnError() throws Exception {
		// prepare mock to fail
		doThrow(new CloudAdapterException("something went wrong!")).when(
				cloudAdapterMock).getPoolSize();

		// call cloud adapter
		PrioritizedCloudAdapter cloudAdapter = prioritizedCloudAdapter(100,
				"localhost", httpsPort);
		Callable<PoolSizeSummary> request = new HttpRequestFactory()
				.newGetPoolSizeRequest(cloudAdapter);
		try {
			request.call();
			fail("call should fail!");
		} catch (CloudAdapterException e) {
			Throwable cause = e.getCause();
			assertThat(cause, is(instanceOf(HttpResponseException.class)));
			assertThat(HttpResponseException.class.cast(cause).getStatusCode(),
					is(500));
		}
	}

	@Test
	public void setDesiredSizeRequest() throws Exception {
		// prepare mock to receive call
		doNothing().when(cloudAdapterMock).setDesiredSize(5);

		// call cloud adapter
		PrioritizedCloudAdapter cloudAdapter = prioritizedCloudAdapter(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newSetDesiredSizeRequest(cloudAdapter, 5);
		request.call();

		// verify that mock received call
		verify(cloudAdapterMock).setDesiredSize(5);
		verifyNoMoreInteractions(cloudAdapterMock);
	}

	@Test
	public void setDesiredSizeRequestOnError() throws Exception {
		// prepare mock to fail
		doThrow(new CloudAdapterException("something went wrong!")).when(
				cloudAdapterMock).setDesiredSize(5);

		// call cloud adapter
		PrioritizedCloudAdapter cloudAdapter = prioritizedCloudAdapter(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newSetDesiredSizeRequest(cloudAdapter, 5);
		try {
			request.call();
			fail("call should fail!");
		} catch (CloudAdapterException e) {
			Throwable cause = e.getCause();
			assertThat(cause, is(instanceOf(HttpResponseException.class)));
			assertThat(HttpResponseException.class.cast(cause).getStatusCode(),
					is(500));
		}
	}

	@Test
	public void terminateMachineRequest() throws Exception {
		// prepare mock to receive call
		doNothing().when(cloudAdapterMock).terminateMachine("i-1", true);

		// call cloud adapter
		PrioritizedCloudAdapter cloudAdapter = prioritizedCloudAdapter(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newTerminateMachineRequest(cloudAdapter, "i-1", true);
		request.call();

		// verify that mock received call
		verify(cloudAdapterMock).terminateMachine("i-1", true);
		verifyNoMoreInteractions(cloudAdapterMock);
	}

	@Test
	public void terminateMachineRequestOnError() throws Exception {
		// prepare mock to fail
		doThrow(new CloudAdapterException("something went wrong!")).when(
				cloudAdapterMock).terminateMachine("i-1", true);

		// call cloud adapter
		PrioritizedCloudAdapter cloudAdapter = prioritizedCloudAdapter(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newTerminateMachineRequest(cloudAdapter, "i-1", true);
		try {
			request.call();
			fail("call should fail!");
		} catch (CloudAdapterException e) {
			Throwable cause = e.getCause();
			assertThat(cause, is(instanceOf(HttpResponseException.class)));
			assertThat(HttpResponseException.class.cast(cause).getStatusCode(),
					is(500));
		}
	}

	@Test
	public void setServiceStateRequest() throws Exception {
		// prepare mock to receive call
		doNothing().when(cloudAdapterMock).setServiceState("i-1",
				ServiceState.IN_SERVICE);

		// call cloud adapter
		PrioritizedCloudAdapter cloudAdapter = prioritizedCloudAdapter(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newSetServiceStateRequest(cloudAdapter, "i-1",
						ServiceState.IN_SERVICE);
		request.call();

		// verify that mock received call
		verify(cloudAdapterMock)
				.setServiceState("i-1", ServiceState.IN_SERVICE);
		verifyNoMoreInteractions(cloudAdapterMock);
	}

	@Test
	public void setServiceStateRequestOnError() throws Exception {
		// prepare mock to fail
		doThrow(new CloudAdapterException("something went wrong!")).when(
				cloudAdapterMock).setServiceState("i-1", ServiceState.BOOTING);

		// call cloud adapter
		PrioritizedCloudAdapter cloudAdapter = prioritizedCloudAdapter(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newSetServiceStateRequest(cloudAdapter, "i-1",
						ServiceState.BOOTING);
		try {
			request.call();
			fail("call should fail!");
		} catch (CloudAdapterException e) {
			Throwable cause = e.getCause();
			assertThat(cause, is(instanceOf(HttpResponseException.class)));
			assertThat(HttpResponseException.class.cast(cause).getStatusCode(),
					is(500));
		}
	}

	@Test
	public void attachMachineRequest() throws Exception {
		// prepare mock to receive call
		doNothing().when(cloudAdapterMock).attachMachine("i-1");

		// call cloud adapter
		PrioritizedCloudAdapter cloudAdapter = prioritizedCloudAdapter(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newAttachMachineRequest(cloudAdapter, "i-1");
		request.call();

		// verify that mock received call
		verify(cloudAdapterMock).attachMachine("i-1");
		verifyNoMoreInteractions(cloudAdapterMock);
	}

	@Test
	public void attachMachineRequestOnError() throws Exception {
		// prepare mock to fail
		doThrow(new CloudAdapterException("something went wrong!")).when(
				cloudAdapterMock).attachMachine("i-1");

		// call cloud adapter
		PrioritizedCloudAdapter cloudAdapter = prioritizedCloudAdapter(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newAttachMachineRequest(cloudAdapter, "i-1");
		try {
			request.call();
			fail("call should fail!");
		} catch (CloudAdapterException e) {
			Throwable cause = e.getCause();
			assertThat(cause, is(instanceOf(HttpResponseException.class)));
			assertThat(HttpResponseException.class.cast(cause).getStatusCode(),
					is(500));
		}
	}

	@Test
	public void detachMachineRequest() throws Exception {
		// prepare mock to receive call
		doNothing().when(cloudAdapterMock).detachMachine("i-1", false);

		// call cloud adapter
		PrioritizedCloudAdapter cloudAdapter = prioritizedCloudAdapter(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newDetachMachineRequest(cloudAdapter, "i-1", false);
		request.call();

		// verify that mock received call
		verify(cloudAdapterMock).detachMachine("i-1", false);
		verifyNoMoreInteractions(cloudAdapterMock);
	}

	@Test
	public void detachMachineRequestOnError() throws Exception {
		// prepare mock to fail
		doThrow(new CloudAdapterException("something went wrong!")).when(
				cloudAdapterMock).detachMachine("i-1", true);

		// call cloud adapter
		PrioritizedCloudAdapter cloudAdapter = prioritizedCloudAdapter(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newDetachMachineRequest(cloudAdapter, "i-1", true);
		try {
			request.call();
			fail("call should fail!");
		} catch (CloudAdapterException e) {
			Throwable cause = e.getCause();
			assertThat(cause, is(instanceOf(HttpResponseException.class)));
			assertThat(HttpResponseException.class.cast(cause).getStatusCode(),
					is(500));
		}
	}

	/**
	 * Creates a {@link PrioritizedCloudAdapter} without any authentication
	 * credentials specified.
	 *
	 * @param priority
	 * @param host
	 * @param port
	 * @return
	 */
	private PrioritizedCloudAdapter prioritizedCloudAdapter(int priority,
			String host, int port) {
		return new PrioritizedCloudAdapter(priority, host, port, null, null);
	}
}
