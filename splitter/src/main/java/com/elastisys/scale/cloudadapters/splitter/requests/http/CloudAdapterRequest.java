package com.elastisys.scale.cloudadapters.splitter.requests.http;

import java.util.concurrent.Callable;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapters.splitter.config.PrioritizedCloudAdapter;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;

/**
 * Base class for cloud adapter HTTP requests created by the
 * {@link HttpRequestFactory}.
 *
 * @see HttpRequestFactory
 *
 * @param <T>
 *            The response type.
 */
public abstract class CloudAdapterRequest<T> implements Callable<T> {

	/** The targeted cloud adapter. */
	private final PrioritizedCloudAdapter cloudAdapter;

	/**
	 * Creates a {@link CloudAdapterRequest} targeting a specific cloud adapter.
	 *
	 * @param cloudAdapter
	 *            The targeted cloud adapter.
	 */
	public CloudAdapterRequest(PrioritizedCloudAdapter cloudAdapter) {
		this.cloudAdapter = cloudAdapter;
	}

	private PrioritizedCloudAdapter cloudAdapter() {
		return this.cloudAdapter;
	}

	/**
	 * Constructs a URL for a particular resource on the remote cloud adapter.
	 *
	 * @param path
	 *            A resource path on the remote cloud adapter.
	 * @return
	 */
	public String url(String path) {
		return String.format("%s%s", adapterBaseUrl(), path);
	}

	private String adapterBaseUrl() {
		return String.format("https://%s:%d",
				this.cloudAdapter.getCloudAdapterHost(),
				this.cloudAdapter.getCloudAdapterPort());
	}

	@Override
	public T call() throws CloudAdapterException {
		try {
			AuthenticatedHttpClient client = new AuthenticatedHttpClient(
					cloudAdapter().getBasicCredentials(), cloudAdapter()
							.getCertificateCredentials());
			return execute(client);
		} catch (Exception e) {
			String requestClassName = getClass().getSimpleName();
			String message = String.format(
					"failed to complete %s against cloud adapter %s: %s",
					requestClassName, adapterBaseUrl(), e.getMessage());
			throw new CloudAdapterException(message, e);
		}
	}

	/**
	 * Execute the request against the remote cloud adapter using a HTTP client
	 * set up according to the authentication mechanisms specified for the
	 * {@link PrioritizedCloudAdapter}.
	 * <p/>
	 * Sub-classes may want to use the {@link #url(String)} method to construct
	 * URLs to reach the cloud adapter.
	 * <p/>
	 * Note: this method is free to throw any kind of exception and does not
	 * need to wrap them in a {@link CloudAdapterException}. The parent class
	 * will take care of that.
	 *
	 * @param client
	 *            A http client set up according to the specified authentication
	 *            credentials in the {@link PrioritizedCloudAdapter}.
	 * @return The response value.
	 * @throws Exception
	 *             on error to complete the request.
	 */
	public abstract T execute(AuthenticatedHttpClient client) throws Exception;
}