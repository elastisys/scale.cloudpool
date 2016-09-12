package com.elastisys.scale.cloudpool.openstack.requests;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.concurrent.Callable;

import org.openstack4j.api.OSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.commons.openstack.ApiAccessConfig;
import com.elastisys.scale.commons.openstack.OSClientFactory;

/**
 * An abstract base class for implementing OpenStack request clients.
 * <p/>
 * Sub-classes need to implement {@link #doRequest(OSClient)}.
 *
 * @param <R>
 *            the response type
 */
public abstract class AbstractOpenstackRequest<R> implements Callable<R> {

	static Logger LOG = LoggerFactory.getLogger(AbstractOpenstackRequest.class);

	/** OpenStack API client factory. */
	private final OSClientFactory clientFactory;

	/**
	 * Constructs a new {@link AbstractOpenstackRequest} with a given
	 * {@link OSClientFactory}.
	 *
	 * @param clientFactory
	 *            OpenStack API client factory
	 */
	public AbstractOpenstackRequest(OSClientFactory clientFactory) {
		checkArgument(clientFactory != null, "clientFactory cannot be null");
		this.clientFactory = clientFactory;
	}

	@Override
	public R call() throws RuntimeException {
		OSClient api = this.clientFactory.authenticatedClient();
		return doRequest(api);
	}

	/**
	 * Returns the OpenStack API client factory.
	 *
	 * @return
	 */
	public OSClientFactory getClientFactory() {
		return this.clientFactory;
	}

	/**
	 * Returns the API access configuration that describes how to authenticate
	 * with and communicate over the OpenStack API.
	 *
	 * @return
	 */
	public ApiAccessConfig getApiAccessConfig() {
		return this.clientFactory.getApiAccessConfig();
	}

	/**
	 * Carries out the request and returns the response.
	 *
	 * @param api
	 *            An authenticated OpenStack API client that is instructed to
	 *            operate against the configured region.
	 * @return The response.
	 * @throws RuntimeException
	 *             if the request failed.
	 */
	public abstract R doRequest(OSClient api) throws RuntimeException;
}
