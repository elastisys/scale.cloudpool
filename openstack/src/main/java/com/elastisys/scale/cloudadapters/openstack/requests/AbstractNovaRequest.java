package com.elastisys.scale.cloudadapters.openstack.requests;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.io.IOException;

import org.jclouds.compute.ComputeService;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapters.openstack.scalinggroup.OpenStackScalingGroupConfig;
import com.elastisys.scale.cloudadapters.openstack.utils.OpenstackApiUtils;
import com.elastisys.scale.commons.net.retryable.Requester;

/**
 * An abstract base class for implementing OpenStack Nova (compute) request
 * clients.
 * <p/>
 * Sub-classes need to implement {@link #doRequest(NovaApi)}.
 * <p/>
 * This class is intended to be subclassed by clients wishing to work with a
 * native nova API ({@link NovaApi}). Clients wishing to interact with Openstack
 * over the Jclouds {@link ComputeService} API should extend the
 * {@link AbstractOpenstackRequest} class.
 *
 * 
 *
 * @param <R>
 *            the response type
 */
public abstract class AbstractNovaRequest<R> implements Requester<R> {

	static Logger LOG = LoggerFactory.getLogger(AbstractNovaRequest.class);

	/** Connection details for a particular OpenStack account. */
	private final OpenStackScalingGroupConfig account;

	/**
	 * Constructs a new {@link AbstractNovaRequest} for a certain OpenStack
	 * account.
	 *
	 * @param account
	 *            Connection details for a particular OpenStack account.
	 */
	public AbstractNovaRequest(OpenStackScalingGroupConfig account) {
		checkNotNull(account, "account config cannot be null");
		this.account = account;
	}

	/**
	 * Returns the connection details for the targeted OpenStack account.
	 *
	 * @return
	 */
	public OpenStackScalingGroupConfig getAccount() {
		return this.account;
	}

	@Override
	public R call() throws RuntimeException {
		NovaApi api = OpenstackApiUtils.getNativeApi(this.account);
		try {
			return doRequest(api);
		} finally {
			try {
				api.close();
			} catch (IOException e) {
				LOG.warn(format("failed to close NovaApi: %s", e.getMessage()),
						e);
			}
		}
	}

	/**
	 * Carries out the request and returns the response.
	 *
	 * @param api
	 *            The OpenStack Nova (compute) API.
	 * @return The response.
	 * @throws RuntimeException
	 *             if the request failed.
	 */
	public abstract R doRequest(NovaApi api) throws RuntimeException;
}
