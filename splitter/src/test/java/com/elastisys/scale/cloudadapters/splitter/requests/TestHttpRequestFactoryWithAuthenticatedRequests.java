package com.elastisys.scale.cloudadapters.splitter.requests;

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

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.server.CloudAdapterOptions;
import com.elastisys.scale.cloudadapers.api.server.CloudAdapterServer;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapters.splitter.config.PrioritizedCloudAdapter;
import com.elastisys.scale.cloudadapters.splitter.requests.http.HttpRequestFactory;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.io.Resources;

/**
 * Verify that {@link HttpRequestExecutor} produces authenticated HTTP requests
 * when specified by the {@link PrioritizedCloudAdapter} configuration.
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

	/** Cloud adapter mock backing the server. */
	private static CloudAdapter cloudAdapter = mock(CloudAdapter.class);

	/**
	 * Starts up a cloud adapter server running with the cloud adapter mock
	 * prior to running the test methods. The server requires clients to
	 * authenticate with certificate credentials and is backed by a cloud
	 * adapter that only answers GetMachinePool requests.
	 */
	@BeforeClass
	public static void onSetup() throws Exception {
		List<Integer> freePorts = HostUtils.findFreePorts(2);
		httpsPort = freePorts.get(0);

		// set up mocked cloud adapter that will back the created server. it
		// only responds to getMachinePool calls.
		when(cloudAdapter.getMachinePool()).thenReturn(
				MachinePool.emptyPool(UtcTime.now()));

		CloudAdapterOptions options = new CloudAdapterOptions();
		options.httpsPort = httpsPort;
		options.sslKeyStore = SERVER_KEYSTORE;
		options.sslKeyStorePassword = SERVER_KEYSTORE_PASSWORD;
		options.requireClientCert = true;
		options.sslTrustStore = SERVER_TRUSTSTORE;
		options.sslTrustStorePassword = SERVER_TRUSTSTORE_PASSWORD;

		server = CloudAdapterServer.createServer(cloudAdapter, options);
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
	 * clients (which should be turned down by the cloud adapter server, which
	 * requires cert authentication).
	 */
	@Test
	public void noAuthClient() throws Exception {
		// no client auth specified
		PrioritizedCloudAdapter adapter = new PrioritizedCloudAdapter(10,
				"localhost", httpsPort, null, null);
		try {
			new HttpRequestFactory().newGetMachinePoolRequest(adapter).call();
			fail("should not succeed!");
		} catch (CloudAdapterException e) {
			// certificate authentication should fail during SSH handshake
			assertSslHandshakeFailure(e.getCause());
		}
	}

	/**
	 * Verify that {@link HttpRequestFactory} can create BASIC authentication
	 * clients (which should be turned down by the cloud adapter server, which
	 * requires cert authentication).
	 */
	@Test
	public void basicAuthClient() throws Exception {
		// basic client auth specified
		PrioritizedCloudAdapter adapter = new PrioritizedCloudAdapter(10,
				"localhost", httpsPort, new BasicCredentials("user", "secret"),
				null);
		try {
			new HttpRequestFactory().newGetMachinePoolRequest(adapter).call();
			fail("should not succeed!");
		} catch (CloudAdapterException e) {
			// certificate authentication should fail during SSH handshake
			assertSslHandshakeFailure(e.getCause());
		}
	}

	/**
	 * Verify that {@link HttpRequestFactory} can create certificate
	 * authentication clients (which should be admitted access by the cloud
	 * adapter server).
	 */
	@Test
	public void certAuthClient() throws Exception {
		// cert client auth specified
		PrioritizedCloudAdapter adapter = new PrioritizedCloudAdapter(10,
				"localhost", httpsPort, null, new CertificateCredentials(
						CLIENT_KEYSTORE, CLIENT_KEYSTORE_PASSWORD));
		new HttpRequestFactory().newGetMachinePoolRequest(adapter).call();
	}

	/**
	 * Check that a client using the untrusted certificate isn't granted access
	 * by the cloud adapter server.
	 */
	@Test
	public void certAuthClientWithUntrustedCert() throws Exception {
		// cert client auth specified
		PrioritizedCloudAdapter adapter = new PrioritizedCloudAdapter(10,
				"localhost", httpsPort, null, new CertificateCredentials(
						UNTRUSTED_CLIENT_KEYSTORE,
						UNTRUSTED_CLIENT_KEYSTORE_PASSWORD));
		try {
			new HttpRequestFactory().newGetMachinePoolRequest(adapter).call();
			fail("should not succeed!");
		} catch (CloudAdapterException e) {
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
