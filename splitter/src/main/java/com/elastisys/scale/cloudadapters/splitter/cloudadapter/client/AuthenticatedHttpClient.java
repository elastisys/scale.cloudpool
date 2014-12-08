package com.elastisys.scale.cloudadapters.splitter.cloudadapter.client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;

import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.commons.net.http.HttpRequestResponse;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.google.common.base.Optional;

/**
 * Performs HTTP requests that are authenticated via Basic authentication and/or
 * a client certificate.
 */
public class AuthenticatedHttpClient {

	/** The {@link Logger} instance to make use of. */
	private final Logger logger;
	/** Username/password credentials for basic authentication. */
	private final Optional<BasicCredentials> basicCredentials;
	/** Certificate credentials for certificate-based client authentication. */
	private final Optional<CertificateCredentials> certificateCredentials;

	/**
	 * Constructs a {@link AuthenticatedHttpClient}.
	 *
	 * @param basicCredentials
	 *            Username/password credentials for basic authentication.
	 * @param certificateCredentials
	 *            Certificate credentials for certificate-based client
	 *            authentication.
	 */
	public AuthenticatedHttpClient(Optional<BasicCredentials> basicCredentials,
			Optional<CertificateCredentials> certificateCredentials) {
		this(LoggerFactory.getLogger(AuthenticatedHttpClient.class),
				basicCredentials, certificateCredentials);
	}

	/**
	 * Constructs a {@link AuthenticatedHttpClient}.
	 *
	 * @param logger
	 *            The {@link Logger} instance to make use of.
	 * @param basicCredentials
	 *            Username/password credentials for basic authentication.
	 * @param certificateCredentials
	 *            Certificate credentials for certificate-based client
	 *            authentication.
	 */
	public AuthenticatedHttpClient(Logger logger,
			Optional<BasicCredentials> basicCredentials,
			Optional<CertificateCredentials> certificateCredentials) {
		checkNotNull(logger, "null logger provided");
		checkArgument(
				basicCredentials.isPresent()
				|| certificateCredentials.isPresent(),
				"neither basic credentials nor certificate "
						+ "credentials were provided");
		this.logger = logger;
		this.basicCredentials = basicCredentials;
		this.certificateCredentials = certificateCredentials;
	}

	/**
	 * Prepares a http(s) client configured with {@link CertificateCredentials}
	 * and/or {@link BasicCredentials}.
	 *
	 * @return
	 * @throws CloudAdapterException
	 */
	private CloseableHttpClient prepareAuthenticatingClient() throws Exception {
		// install host name verifier that always approves host names
		AllowAllHostnameVerifier hostnameVerifier = new AllowAllHostnameVerifier();
		// for SSL requests we should accept self-signed host certificates
		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		SSLContextBuilder sslContextBuilder = SSLContexts.custom()
				.loadTrustMaterial(trustStore, new TrustSelfSignedStrategy());

		// first attempt to prepare a https client with certificate credentials
		Optional<CertificateCredentials> certificateCredentials = this.certificateCredentials;

		if (certificateCredentials.isPresent()) {
			String keystorePath = certificateCredentials.get()
					.getKeystorePath();
			String keystorePassword = certificateCredentials.get()
					.getKeystorePassword();
			// fall back to keystore password if key password is missing
			String keyPassword = certificateCredentials.get().getKeyPassword()
					.or(keystorePassword);
			this.logger.debug(
					"using client-side certificate from keystore '{}'",
					keystorePath);
			KeyStore keyStore = KeyStore.getInstance(certificateCredentials
					.get().getKeystoreType().toString());
			keyStore.load(new FileInputStream(keystorePath),
					keystorePassword.toCharArray());
			sslContextBuilder.loadKeyMaterial(keyStore,
					keyPassword.toCharArray());
		}

		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		Optional<BasicCredentials> basicCredentials = this.basicCredentials;
		if (basicCredentials.isPresent()) {
			String username = basicCredentials.get().getUsername();
			String password = basicCredentials.get().getPassword();
			this.logger
			.debug("passing Basic authentication credentials for username '{}'",
					username);
			credentialsProvider.setCredentials(AuthScope.ANY,
					new UsernamePasswordCredentials(username, password));
		}
		CloseableHttpClient httpclient = HttpClients.custom()
				.setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCredentialsProvider(credentialsProvider)
				.setSslcontext(sslContextBuilder.build())
				.setHostnameVerifier(hostnameVerifier).build();
		return httpclient;
	}

	/**
	 * Sends a HTTP request to a remote endpoint and returns a
	 * {@link HttpRequestResponse} object holding the response message status,
	 * body, and headers.
	 *
	 * @param request
	 *            The request to send.
	 * @return The received response.
	 * @throws IOException
	 *             If anything went wrong.
	 */
	public HttpRequestResponse execute(HttpUriRequest request)
			throws IOException {
		CloseableHttpClient client;
		try {
			client = prepareAuthenticatingClient();
		} catch (Exception e) {
			throw new IOException(format(
					"failed to prepare http client for request (%s): %s",
					request, e.getMessage()), e);
		}

		try {
			CloseableHttpResponse httpResponse = null;
			try {
				this.logger.info(format("sending request (%s)", request));
				httpResponse = client.execute(request);
			} catch (Exception e) {
				throw new IOException(format("failed to send request (%s): %s",
						request, e.getMessage()), e);
			}

			HttpRequestResponse response = new HttpRequestResponse(httpResponse);
			int responseCode = response.getStatusCode();
			String responseBody = response.getResponseBody();
			if (responseCode != HttpStatus.SC_OK) {
				throw new IOException(format(
						"error response received from remote endpoint "
								+ "on request (%s): %s: %s", request,
								responseCode, responseBody));
			}
			return response;
		} finally {
			client.close();
		}
	}
}