package com.elastisys.scale.cloudpool.openstack.requests;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.concurrent.Callable;

import org.openstack4j.api.OSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.openstack.driver.client.OSClientFactory;
import com.elastisys.scale.cloudpool.openstack.driver.config.OpenStackPoolDriverConfig;

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

	/** OpenStack API access configuration. */
	private final OpenStackPoolDriverConfig accessConfig;

	/**
	 * Constructs a new {@link AbstractOpenstackRequest} with an access
	 * configuration object that describes how to connect with the OpenStack
	 * API.
	 *
	 * @param accessConfig
	 *            OpenStack API access configuration.
	 *
	 */
	public AbstractOpenstackRequest(OpenStackPoolDriverConfig accessConfig) {
		checkArgument(accessConfig != null, "accessCsonfig cannot be null");
		this.accessConfig = accessConfig;
	}

	/**
	 * Returns the configuration details for accessing the targeted OpenStack
	 * account.
	 *
	 * @return
	 */
	public OpenStackPoolDriverConfig getAccessConfig() {
		return this.accessConfig;
	}

	@Override
	public R call() throws RuntimeException {
		OSClientFactory clientFactory = new OSClientFactory(
				this.accessConfig.getConnectionTimeout(),
				this.accessConfig.getSocketTimeout());
		OSClient api = clientFactory
				.createAuthenticatedClient(this.accessConfig.getAuth());
		api.useRegion(this.accessConfig.getRegion());

		return doRequest(api);
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
