package com.elastisys.scale.cloudpool.splitter.requests;

import static com.elastisys.scale.cloudpool.splitter.testutils.TestUtils.machine;
import static com.elastisys.scale.cloudpool.splitter.testutils.TestUtils.pool;
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

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.server.CloudPoolOptions;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.elastisys.scale.cloudpool.splitter.requests.http.HttpRequestFactory;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.google.common.io.Resources;

/**
 * Exercises the requests produced the {@link HttpRequestFactory} against a fake
 * cloud pool server.
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

	private static CloudPool cloudPoolMock = mock(CloudPool.class);

	/**
	 * Starts up a cloud pool server running with the cloud pool mock prior to
	 * running the test methods.
	 */
	@BeforeClass
	public static void onSetup() throws Exception {
		List<Integer> freePorts = HostUtils.findFreePorts(1);
		httpsPort = freePorts.get(0);

		CloudPoolOptions options = new CloudPoolOptions();
		options.httpsPort = httpsPort;
		options.sslKeyStore = SERVER_KEYSTORE;
		options.sslKeyStorePassword = SERVER_KEYSTORE_PASSWORD;
		options.requireClientCert = false;
		options.enableConfigHandler = true;

		server = CloudPoolServer.createServer(cloudPoolMock, options);
		server.start();
	}

	/**
	 * Reset cloud pool mock between every test method.
	 */
	@Before
	public void beforeTestMethod() {
		reset(cloudPoolMock);
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
		when(cloudPoolMock.getMachinePool()).thenReturn(pool);

		// call cloud pool
		PrioritizedCloudPool cloudPool = prioritizedCloudPool(100,
				"localhost", httpsPort);
		Callable<MachinePool> request = new HttpRequestFactory()
				.newGetMachinePoolRequest(cloudPool);
		assertThat(request.call(), is(pool));

		// verify that mock received call
		verify(cloudPoolMock).getMachinePool();
		verifyNoMoreInteractions(cloudPoolMock);
	}

	@Test
	public void getMachinePoolRequestOnError() throws Exception {
		// prepare mock to fail
		doThrow(new CloudPoolException("something went wrong!")).when(
				cloudPoolMock).getMachinePool();

		// call cloud pool
		PrioritizedCloudPool cloudPool = prioritizedCloudPool(100,
				"localhost", httpsPort);
		Callable<MachinePool> request = new HttpRequestFactory()
				.newGetMachinePoolRequest(cloudPool);
		try {
			request.call();
			fail("call should fail!");
		} catch (CloudPoolException e) {
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
		when(cloudPoolMock.getPoolSize()).thenReturn(poolSize);

		// call cloud pool
		PrioritizedCloudPool cloudPool = prioritizedCloudPool(100,
				"localhost", httpsPort);
		Callable<PoolSizeSummary> request = new HttpRequestFactory()
				.newGetPoolSizeRequest(cloudPool);
		assertThat(request.call(), is(poolSize));

		// verify that mock received call
		verify(cloudPoolMock).getPoolSize();
		verifyNoMoreInteractions(cloudPoolMock);
	}

	@Test
	public void getPoolSizeRequestOnError() throws Exception {
		// prepare mock to fail
		doThrow(new CloudPoolException("something went wrong!")).when(
				cloudPoolMock).getPoolSize();

		// call cloud pool
		PrioritizedCloudPool cloudPool = prioritizedCloudPool(100,
				"localhost", httpsPort);
		Callable<PoolSizeSummary> request = new HttpRequestFactory()
				.newGetPoolSizeRequest(cloudPool);
		try {
			request.call();
			fail("call should fail!");
		} catch (CloudPoolException e) {
			Throwable cause = e.getCause();
			assertThat(cause, is(instanceOf(HttpResponseException.class)));
			assertThat(HttpResponseException.class.cast(cause).getStatusCode(),
					is(500));
		}
	}

	@Test
	public void setDesiredSizeRequest() throws Exception {
		// prepare mock to receive call
		doNothing().when(cloudPoolMock).setDesiredSize(5);

		// call cloud pool
		PrioritizedCloudPool cloudPool = prioritizedCloudPool(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newSetDesiredSizeRequest(cloudPool, 5);
		request.call();

		// verify that mock received call
		verify(cloudPoolMock).setDesiredSize(5);
		verifyNoMoreInteractions(cloudPoolMock);
	}

	@Test
	public void setDesiredSizeRequestOnError() throws Exception {
		// prepare mock to fail
		doThrow(new CloudPoolException("something went wrong!")).when(
				cloudPoolMock).setDesiredSize(5);

		// call cloud pool
		PrioritizedCloudPool cloudPool = prioritizedCloudPool(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newSetDesiredSizeRequest(cloudPool, 5);
		try {
			request.call();
			fail("call should fail!");
		} catch (CloudPoolException e) {
			Throwable cause = e.getCause();
			assertThat(cause, is(instanceOf(HttpResponseException.class)));
			assertThat(HttpResponseException.class.cast(cause).getStatusCode(),
					is(500));
		}
	}

	@Test
	public void terminateMachineRequest() throws Exception {
		// prepare mock to receive call
		doNothing().when(cloudPoolMock).terminateMachine("i-1", true);

		// call cloud pool
		PrioritizedCloudPool cloudPool = prioritizedCloudPool(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newTerminateMachineRequest(cloudPool, "i-1", true);
		request.call();

		// verify that mock received call
		verify(cloudPoolMock).terminateMachine("i-1", true);
		verifyNoMoreInteractions(cloudPoolMock);
	}

	@Test
	public void terminateMachineRequestOnError() throws Exception {
		// prepare mock to fail
		doThrow(new CloudPoolException("something went wrong!")).when(
				cloudPoolMock).terminateMachine("i-1", true);

		// call cloud pool
		PrioritizedCloudPool cloudPool = prioritizedCloudPool(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newTerminateMachineRequest(cloudPool, "i-1", true);
		try {
			request.call();
			fail("call should fail!");
		} catch (CloudPoolException e) {
			Throwable cause = e.getCause();
			assertThat(cause, is(instanceOf(HttpResponseException.class)));
			assertThat(HttpResponseException.class.cast(cause).getStatusCode(),
					is(500));
		}
	}

	@Test
	public void setServiceStateRequest() throws Exception {
		// prepare mock to receive call
		doNothing().when(cloudPoolMock).setServiceState("i-1",
				ServiceState.IN_SERVICE);

		// call cloud pool
		PrioritizedCloudPool cloudPool = prioritizedCloudPool(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newSetServiceStateRequest(cloudPool, "i-1",
						ServiceState.IN_SERVICE);
		request.call();

		// verify that mock received call
		verify(cloudPoolMock).setServiceState("i-1", ServiceState.IN_SERVICE);
		verifyNoMoreInteractions(cloudPoolMock);
	}

	@Test
	public void setServiceStateRequestOnError() throws Exception {
		// prepare mock to fail
		doThrow(new CloudPoolException("something went wrong!")).when(
				cloudPoolMock).setServiceState("i-1", ServiceState.BOOTING);

		// call cloud pool
		PrioritizedCloudPool cloudPool = prioritizedCloudPool(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newSetServiceStateRequest(cloudPool, "i-1",
						ServiceState.BOOTING);
		try {
			request.call();
			fail("call should fail!");
		} catch (CloudPoolException e) {
			Throwable cause = e.getCause();
			assertThat(cause, is(instanceOf(HttpResponseException.class)));
			assertThat(HttpResponseException.class.cast(cause).getStatusCode(),
					is(500));
		}
	}

	@Test
	public void attachMachineRequest() throws Exception {
		// prepare mock to receive call
		doNothing().when(cloudPoolMock).attachMachine("i-1");

		// call cloud pool
		PrioritizedCloudPool cloudPool = prioritizedCloudPool(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newAttachMachineRequest(cloudPool, "i-1");
		request.call();

		// verify that mock received call
		verify(cloudPoolMock).attachMachine("i-1");
		verifyNoMoreInteractions(cloudPoolMock);
	}

	@Test
	public void attachMachineRequestOnError() throws Exception {
		// prepare mock to fail
		doThrow(new CloudPoolException("something went wrong!")).when(
				cloudPoolMock).attachMachine("i-1");

		// call cloud pool
		PrioritizedCloudPool cloudPool = prioritizedCloudPool(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newAttachMachineRequest(cloudPool, "i-1");
		try {
			request.call();
			fail("call should fail!");
		} catch (CloudPoolException e) {
			Throwable cause = e.getCause();
			assertThat(cause, is(instanceOf(HttpResponseException.class)));
			assertThat(HttpResponseException.class.cast(cause).getStatusCode(),
					is(500));
		}
	}

	@Test
	public void detachMachineRequest() throws Exception {
		// prepare mock to receive call
		doNothing().when(cloudPoolMock).detachMachine("i-1", false);

		// call cloud pool
		PrioritizedCloudPool cloudPool = prioritizedCloudPool(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newDetachMachineRequest(cloudPool, "i-1", false);
		request.call();

		// verify that mock received call
		verify(cloudPoolMock).detachMachine("i-1", false);
		verifyNoMoreInteractions(cloudPoolMock);
	}

	@Test
	public void detachMachineRequestOnError() throws Exception {
		// prepare mock to fail
		doThrow(new CloudPoolException("something went wrong!")).when(
				cloudPoolMock).detachMachine("i-1", true);

		// call cloud pool
		PrioritizedCloudPool cloudPool = prioritizedCloudPool(100,
				"localhost", httpsPort);
		Callable<Void> request = new HttpRequestFactory()
				.newDetachMachineRequest(cloudPool, "i-1", true);
		try {
			request.call();
			fail("call should fail!");
		} catch (CloudPoolException e) {
			Throwable cause = e.getCause();
			assertThat(cause, is(instanceOf(HttpResponseException.class)));
			assertThat(HttpResponseException.class.cast(cause).getStatusCode(),
					is(500));
		}
	}

	/**
	 * Creates a {@link PrioritizedCloudPool} without any authentication
	 * credentials specified.
	 *
	 * @param priority
	 * @param host
	 * @param port
	 * @return
	 */
	private PrioritizedCloudPool prioritizedCloudPool(int priority,
			String host, int port) {
		return new PrioritizedCloudPool(priority, host, port, null, null);
	}
}
