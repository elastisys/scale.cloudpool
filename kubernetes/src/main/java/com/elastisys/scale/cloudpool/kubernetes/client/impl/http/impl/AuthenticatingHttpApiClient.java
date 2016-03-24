package com.elastisys.scale.cloudpool.kubernetes.client.impl.http.impl;

import java.io.IOException;
import java.security.KeyStore;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.kubernetes.client.KubernetesApiException;
import com.elastisys.scale.cloudpool.kubernetes.client.impl.http.HttpApiClient;
import com.elastisys.scale.cloudpool.kubernetes.config.AuthConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.http.Http;
import com.elastisys.scale.commons.net.http.HttpBuilder;
import com.elastisys.scale.commons.net.http.HttpRequestResponse;
import com.elastisys.scale.commons.security.pem.PemUtils;
import com.google.common.base.Charsets;
import com.google.gson.JsonObject;

/**
 * A {@link HttpApiClient} that is set up to authentiacte with the remote server
 * according to an {@link AuthConfig}.
 *
 * @see HttpApiClient
 */
public class AuthenticatingHttpApiClient implements HttpApiClient {
	private static final Logger LOG = LoggerFactory
			.getLogger(AuthenticatingHttpApiClient.class);

	/**
	 * The currently set {@link AuthConfig} which declares how to authenticate
	 * for the HTTP requests.
	 */
	private AuthConfig config;

	@Override
	public void configure(AuthConfig config) throws IllegalArgumentException {
		LOG.debug("new auth config set: {}",
				JsonUtils.toPrettyString(JsonUtils.toJson(config)));
		this.config = config;
	}

	@Override
	public JsonObject get(String url)
			throws HttpResponseException, IOException {
		Http client = buildHttpClient();
		HttpRequestResponse response = client.execute(new HttpGet(url));
		LOG.debug("status: {}", response.getStatusCode());
		LOG.debug("body: {}", response.getResponseBody());
		return JsonUtils.parseJsonString(response.getResponseBody())
				.getAsJsonObject();
	}

	@Override
	public JsonObject patch(String url, JsonObject data)
			throws HttpResponseException, IOException {
		Http client = buildHttpClient();
		HttpPatch patch = new HttpPatch(url);
		patch.addHeader("Content-Type", "application/merge-patch+json");
		patch.setEntity(new StringEntity(JsonUtils.toPrettyString(data),
				Charsets.UTF_8));

		HttpRequestResponse response = client.execute(patch);
		LOG.debug("status: {}", response.getStatusCode());
		LOG.debug("body: {}", response.getResponseBody());
		return JsonUtils.parseJsonString(response.getResponseBody())
				.getAsJsonObject();
	}

	private void ensureConfigured() throws IllegalStateException {
		if (this.config == null) {
			throw new IllegalStateException(
					"attempt to use http client before being configured");
		}
	}

	/**
	 * Sets up a {@link Http} client according to the configured authentication
	 * parameters.
	 *
	 * @return
	 */
	private Http buildHttpClient()
			throws KubernetesApiException, IllegalStateException {
		ensureConfigured();

		AuthConfig auth = this.config;

		HttpBuilder clientBuilder = Http.builder();
		clientBuilder.verifyHostname(false);

		try {
			// client cert auth
			if (auth.hasClientCert()) {
				// password doesn't matter, only used here
				String keyPassword = "secret";
				KeyStore clientCertKeystore = PemUtils.keyStoreFromCertAndKey(
						auth.getClientCert(), auth.getClientKey(), keyPassword);
				clientBuilder.clientCertAuth(clientCertKeystore, keyPassword);
			}

			// client token auth
			if (auth.hasClientToken()) {
				clientBuilder.clientJwtAuth(auth.getClientToken());
			}

			// server cert auth
			if (auth.hasServerCert()) {
				clientBuilder.verifyHostCert(true);
				KeyStore trustStore = PemUtils
						.keyStoreFromCert(auth.getServerCert());
				clientBuilder.serverAuthTrustStore(trustStore);
			}
		} catch (Exception e) {
			throw new KubernetesApiException(
					String.format("failed to set up auth: %s", e.getMessage()),
					e);
		}

		clientBuilder.contentType(ContentType.APPLICATION_JSON);
		return clientBuilder.build();
	}

}
