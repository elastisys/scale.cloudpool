package com.elastisys.scale.cloudpool.api.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.SocketException;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.ProcessingException;
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
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;
import com.elastisys.scale.commons.rest.client.RestClients;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.io.Resources;
import com.google.gson.JsonObject;

/**
 * Verifies the behavior of the {@link CloudPoolServer} when configured to
 * require client certificate authentication.
 *
 *
 *
 */
public class TestRestApiSecurityCertAuth {
	private static final String SERVER_KEYSTORE = Resources.getResource(
			"security/server/server_keystore.p12").toString();
	private static final String SERVER_KEYSTORE_PASSWORD = "serverpass";

	private static final String SERVER_TRUSTSTORE = Resources.getResource(
			"security/server/server_truststore.jks").toString();
	private static final String SERVER_TRUSTSTORE_PASSWORD = "trustpass";

	private static final String CLIENT_KEYSTORE = "src/test/resources/security/client/client_keystore.p12";
	private static final String CLIENT_KEYSTORE_PASSWORD = "clientpass";

	private static final String UNTRUSTED_CLIENT_KEYSTORE = "src/test/resources/security/untrusted/untrusted_keystore.p12";
	private static final String UNTRUSTED_CLIENT_KEYSTORE_PASSWORD = "untrustedpass";

	/** Web server to use throughout the tests. */
	private static Server server;
	/** Server port to use for HTTPS. */
	private static int httpsPort;

	private static CloudPool cloudPool = mock(CloudPool.class);

	@BeforeClass
	public static void onSetup() throws Exception {
		List<Integer> freePorts = HostUtils.findFreePorts(2);
		httpsPort = freePorts.get(0);

		// set up mocked cloud pool that will back the created server
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

	@AfterClass
	public static void onTeardown() throws Exception {
		server.stop();
		server.join();
	}

	/**
	 * Connect with a client that doesn't authenticate. This should fail, since
	 * server requires a client certificate.
	 */
	@Test
	public void connectWithNoAuthentication() {
		try {
			Client noAuthClient = RestClients.httpsNoAuth();
			noAuthClient.target(getUrl()).request().get();
		} catch (ProcessingException e) {
			assertSslHandshakeFailure(e);
		}
	}

	/**
	 * Connect with a client that uses basic authentication. This should fail,
	 * since server requires a client certificate.
	 */
	@Test
	public void connectWithBasicAuthentication() {
		try {
			Client basicAuthClient = RestClients.httpsBasicAuth("admin",
					"adminpassword");
			basicAuthClient.target(getUrl()).request().get();
		} catch (ProcessingException e) {
			assertSslHandshakeFailure(e);
		}
	}

	/**
	 * Test connecting with a client certificate that is trusted by the server.
	 * This should succeed.
	 */
	@Test
	public void connectWithTrustedCertificate() {
		Client certAuthClient = RestTestUtils.httpsCertAuth(CLIENT_KEYSTORE,
				CLIENT_KEYSTORE_PASSWORD, KeyStoreType.PKCS12);
		Response response = certAuthClient.target(getUrl()).request().get();
		assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
		assertNotNull(response.readEntity(JsonObject.class));
	}

	/**
	 * Test connecting with a client certificate that is <i>not</i> trusted by
	 * the server. This should fail.
	 */
	@Test
	public void connectWithUntrustedCertificate() {
		try {
			Client certAuthClient = RestTestUtils.httpsCertAuth(
					UNTRUSTED_CLIENT_KEYSTORE,
					UNTRUSTED_CLIENT_KEYSTORE_PASSWORD, KeyStoreType.PKCS12);
			certAuthClient.target(getUrl()).request().get();
		} catch (ProcessingException e) {
			assertSslHandshakeFailure(e);
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

	/**
	 * URL to do a {@code GET /pool} request.
	 *
	 * @return
	 */
	private static String getUrl() {
		return String.format("https://localhost:%d/pool", httpsPort);
	}
}
