package com.elastisys.scale.cloudadapters.openstack.tasks;

import static java.lang.String.format;

import org.jclouds.openstack.nova.v2_0.domain.Address;

import com.elastisys.scale.commons.net.retryable.Action;
import com.elastisys.scale.commons.net.retryable.RetryHandler;
import com.elastisys.scale.commons.net.retryable.RetryLimitExceededException;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;
import com.elastisys.scale.commons.net.retryable.retryhandlers.AbstractLimitedRetryHandler;
import com.google.common.collect.Multimap;

/**
 * A {@link RetryHandler} that waits for a server to be assigned at least one IP
 * address. It is intended to be combined with a
 * {@link ServerIpAddressRequester} in a {@link RetryableRequest}.
 * 
 * @see RetryableRequest
 * @see ServerIpAddressRequester
 * 
 * 
 * 
 */
public class RetryUntilAssignedIpAddress extends
		AbstractLimitedRetryHandler<Multimap<String, Address>> {

	public RetryUntilAssignedIpAddress(int maxRetries, long delay) {
		super(maxRetries, delay);
	}

	@Override
	public boolean isSuccessful(Multimap<String, Address> response) {
		return isAssignedIp(response);
	}

	@Override
	public Action<Multimap<String, Address>> maxRetriesExceeded(
			Multimap<String, Address> withResponse) {
		String message = format("Maximum number of retries (%d) exceeded. "
				+ "Last response: %s", this.maxRetries, withResponse);
		RetryLimitExceededException failureReason = new RetryLimitExceededException(
				message);
		return Action.fail(failureReason);
	}

	private boolean isAssignedIp(Multimap<String, Address> addresses) {
		return !addresses.isEmpty();
	}

}
