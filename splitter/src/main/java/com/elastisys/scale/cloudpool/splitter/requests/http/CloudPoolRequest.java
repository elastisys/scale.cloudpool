package com.elastisys.scale.cloudpool.splitter.requests.http;

import java.util.concurrent.Callable;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;

/**
 * Base class for cloud pool HTTP requests created by the
 * {@link HttpRequestFactory}.
 *
 * @see HttpRequestFactory
 *
 * @param <T>
 *            The response type.
 */
public abstract class CloudPoolRequest<T> implements Callable<T> {

	/** The targeted cloud pool. */
	private final PrioritizedCloudPool cloudPool;

	/**
	 * Creates a {@link CloudPoolRequest} targeting a specific cloud pool.
	 *
	 * @param cloudPool
	 *            The targeted cloud pool.
	 */
	public CloudPoolRequest(PrioritizedCloudPool cloudPool) {
		this.cloudPool = cloudPool;
	}

	private PrioritizedCloudPool cloudPool() {
		return this.cloudPool;
	}

	/**
	 * Constructs a URL for a particular resource on the remote cloud pool.
	 *
	 * @param path
	 *            A resource path on the remote cloud pool.
	 * @return
	 */
	public String url(String path) {
		return String.format("%s%s", poolBaseUrl(), path);
	}

	private String poolBaseUrl() {
		return String.format("https://%s:%d",
				this.cloudPool.getCloudPoolHost(),
				this.cloudPool.getCloudPoolPort());
	}

	@Override
	public T call() throws CloudPoolException {
		try {
			AuthenticatedHttpClient client = new AuthenticatedHttpClient(
					cloudPool().getBasicCredentials(), cloudPool()
							.getCertificateCredentials());
			return execute(client);
		} catch (Exception e) {
			String requestClassName = getClass().getSimpleName();
			String message = String.format(
					"failed to complete %s against cloud pool %s: %s",
					requestClassName, poolBaseUrl(), e.getMessage());
			throw new CloudPoolException(message, e);
		}
	}

	/**
	 * Execute the request against the remote cloud pool using a HTTP client set
	 * up according to the authentication mechanisms specified for the
	 * {@link PrioritizedCloudPool}.
	 * <p/>
	 * Sub-classes may want to use the {@link #url(String)} method to construct
	 * URLs to reach the cloud pool.
	 * <p/>
	 * Note: this method is free to throw any kind of exception and does not
	 * need to wrap them in a {@link CloudPoolException}. The parent class will
	 * take care of that.
	 *
	 * @param client
	 *            A http client set up according to the specified authentication
	 *            credentials in the {@link PrioritizedCloudPool}.
	 * @return The response value.
	 * @throws Exception
	 *             on error to complete the request.
	 */
	public abstract T execute(AuthenticatedHttpClient client) throws Exception;
}