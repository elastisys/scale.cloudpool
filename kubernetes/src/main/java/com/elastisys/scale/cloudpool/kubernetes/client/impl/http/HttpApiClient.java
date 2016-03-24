package com.elastisys.scale.cloudpool.kubernetes.client.impl.http;

import java.io.IOException;

import org.apache.http.client.HttpResponseException;

import com.elastisys.scale.cloudpool.kubernetes.client.impl.StandardKubernetesClient;
import com.elastisys.scale.cloudpool.kubernetes.config.AuthConfig;
import com.google.gson.JsonObject;

/**
 * An interface for performing HTTP operations against a remote server. Only the
 * subset of operations needed by the {@link StandardKubernetesClient} is
 * implemented.
 * <p/>
 * Before use, the {@link #configure(AuthConfig)} method must be called.
 *
 * @see StandardKubernetesClient
 */
public interface HttpApiClient {

	/**
	 * Re-configures the {@link HttpApiClient} with a given set of
	 * authentication credentials.
	 *
	 * @param config
	 *            Authentication configuration.
	 * @throws IllegalArgumentException
	 */
	public void configure(AuthConfig config) throws IllegalArgumentException;

	/**
	 * Performs a {@code GET} for a particular server URL. Returns the resulting
	 * JSON response.
	 *
	 * @param url
	 * @return
	 * @throws HttpResponseException
	 * @throws IOException
	 */
	JsonObject get(String url) throws HttpResponseException, IOException;

	/**
	 * Performs a {@code PATCH} of the given data object against a particular
	 * server URL. Returns the resulting JSON response.
	 *
	 * @param url
	 * @return
	 * @throws HttpResponseException
	 * @throws IOException
	 */
	JsonObject patch(String url, JsonObject data)
			throws HttpResponseException, IOException;
}
