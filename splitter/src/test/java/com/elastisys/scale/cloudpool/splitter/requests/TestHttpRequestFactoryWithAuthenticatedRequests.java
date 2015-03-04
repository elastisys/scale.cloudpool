package com.elastisys.scale.cloudpool.splitter.requests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.SocketException;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.protocol.HttpRequestExecutor;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.server.CloudPoolOptions;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.elastisys.scale.cloudpool.splitter.requests.http.HttpRequestFactory;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.io.Resources;

/**
 * Verify that {@link HttpRequestExecutor} produces authenticated HTTP requests
 * when specified by the {@link PrioritizedCloudPool} configuration.
 */
public class TestHttpRequestFactoryWithAuthenticatedRequests {
	private static final String SERVER_KEYSTORE = Resources.getResource(
			"security/server/server_keystore.p12").toString();
	private static final String SERVER_KEYSTORE_PASSWORD = "serverpass";

	private static final String SERVER_TRUSTSTORE = Resources.getResource(
			"security/server/server_truststore.jks").toString();
	private static final String SERVER_TRUSTSTORE_PASSWORD = "trustpass";

	/** Keystore with trusted client cert (in {@link #SERVER_TRUSTSTORE}). */
	private static final String CLIENT_KEYSTORE = "src/test/resources/security/client/client_keystore.p12";
	private static final String CLIENT_KEYSTORE_PASSWORD = "clientpass";

	/** Keystore with untrusted client cert. */
	private static final String UNTRUSTED_CLIENT_KEYSTORE = "src/test/resources/security/untrusted/untrusted_keystore.p12";
	private static final String UNTRUSTED_CLIENT_KEYSTORE_PASSWORD = "untrustedpass";

	/** Web server to use throughout the tests. */
	private static Server server;
	/** Server port to use for HTTPS. */
	private static int httpsPort;

	/** Cloud pool mock backing the server. */
	private static CloudPool cloudPool = mock(CloudPool.class);

	/**
	 * Starts up a cloud pool server running with the cloud pool mock prior to
	 * running the test methods. The server requires clients to authenticate
	 * with certificate credentials and is backed by a cloud pool that only
	 * answers GetMachinePool requests.
	 */
	@BeforeClass
	public static void onSetup() throws Exception {
		List<Integer> freePorts = HostUtils.findFreePorts(2);
		httpsPort = freePorts.get(0);

		// set up mocked cloud pool that will back the created server. it
		// only responds to getMachinePool calls.
		when(cloudPool.getMachinePool()).thenReturn(
				MachinePool.emptyPool(UtcTime.now()));

		CloudPoolOptions options = new CloudPoolOptions();
		options.httpsPort = httpsPort;
		options.sslKeyStore = SERVER_KEYSTORE;
		options.sslKeyStorePassword = SERVER_KEYSTORE_PASSWORD;
		options.requireClientCert = true;
		options.sslTrustStore = SERVER_TRUSTSTORE;
		options.sslTrustStorePassword = SERVER_TRUSTSTORE_PASSWORD;

		server = CloudPoolServer.createServer(cloudPool, options);
		server.start();
	}

	/**
	 * Stop the server when all test methods are done.
	 */
	@AfterClass
	public static void onTeardown() throws Exception {
		server.stop();
		server.join();
	}

	/**
	 * Verify that {@link HttpRequestFactory} can create non-authenticated
	 * clients (which should be turned down by the cloud pool server, which
	 * requires cert authentication).
	 */
	@Test
	public void noAuthClient() throws Exception {
		// no client auth specified
		PrioritizedCloudPool pool = new PrioritizedCloudPool(10, "localhost",
				httpsPort, null, null);
		try {
			new HttpRequestFactory().newGetMachinePoolRequest(pool).call();
			fail("should not succeed!");
		} catch (CloudPoolException e) {
			// certificate authentication should fail during SSH handshake
			assertSslHandshakeFailure(e.getCause());
		}
	}

	/**
	 * Verify that {@link HttpRequestFactory} can create BASIC authentication
	 * clients (which should be turned down by the cloud pool server, which
	 * requires cert authentication).
	 */
	@Test
	public void basicAuthClient() throws Exception {
		// basic client auth specified
		PrioritizedCloudPool pool = new PrioritizedCloudPool(10, "localhost",
				httpsPort, new BasicCredentials("user", "secret"), null);
		try {
			new HttpRequestFactory().newGetMachinePoolRequest(pool).call();
			fail("should not succeed!");
		} catch (CloudPoolException e) {
			// certificate authentication should fail during SSH handshake
			assertSslHandshakeFailure(e.getCause());
		}
	}

	/**
	 * Verify that {@link HttpRequestFactory} can create certificate
	 * authentication clients (which should be admitted access by the cloud pool
	 * server).
	 */
	@Test
	public void certAuthClient() throws Exception {
		// cert client auth specified
		PrioritizedCloudPool pool = new PrioritizedCloudPool(10, "localhost",
				httpsPort, null, new CertificateCredentials(CLIENT_KEYSTORE,
						CLIENT_KEYSTORE_PASSWORD));
		new HttpRequestFactory().newGetMachinePoolRequest(pool).call();
	}

	/**
	 * Check that a client using the untrusted certificate isn't granted access
	 * by the cloud pool server.
	 */
	@Test
	public void certAuthClientWithUntrustedCert() throws Exception {
		// cert client auth specified
		PrioritizedCloudPool pool = new PrioritizedCloudPool(10, "localhost",
				httpsPort, null, new CertificateCredentials(
						UNTRUSTED_CLIENT_KEYSTORE,
						UNTRUSTED_CLIENT_KEYSTORE_PASSWORD));
		try {
			new HttpRequestFactory().newGetMachinePoolRequest(pool).call();
			fail("should not succeed!");
		} catch (CloudPoolException e) {
			// certificate authentication should fail during SSH handshake
			assertSslHandshakeFailure(e.getCause());
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
}
