package com.elastisys.scale.cloudadapters.aws.commons.tasks;

import static java.lang.String.format;

import com.amazonaws.services.ec2.model.InstanceState;
import com.elastisys.scale.commons.net.retryable.Action;
import com.elastisys.scale.commons.net.retryable.RetryHandler;
import com.elastisys.scale.commons.net.retryable.RetryLimitExceededException;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;

/**
 * A {@link RetryHandler} that waits for a Amazon EC2 machine instance to reach
 * {@code running} state. It is intended to be used in a
 * {@link RetryableRequest} in concert with the {@link InstanceStateRequester}.
 *
 * @see RetryableRequest
 * @see InstanceStateRequester
 *
 *
 */
public class RetryUntilRunning extends
AbstractAmazonLimitedRetryHandler<InstanceState> {

	public RetryUntilRunning(int maxRetries) {
		super(maxRetries);
	}

	@Override
	public boolean isSuccessful(InstanceState response) {
		return response.getName().equalsIgnoreCase("running");
	}

	@Override
	public Action<InstanceState> maxRetriesExceeded(InstanceState withResponse) {
		String message = format("Maximum number of retries (%d) exceeded. "
				+ "Last response: %s", this.maxRetries, withResponse);
		RetryLimitExceededException failureReason = new RetryLimitExceededException(
				message);
		return Action.fail(failureReason);
	}
}
