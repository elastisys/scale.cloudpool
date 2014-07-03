package com.elastisys.scale.cloudadapters.aws.commons.tasks;

import static java.lang.String.format;

import com.amazonaws.services.ec2.model.InstanceStatus;
import com.elastisys.scale.commons.net.retryable.Action;
import com.elastisys.scale.commons.net.retryable.RetryHandler;
import com.elastisys.scale.commons.net.retryable.RetryLimitExceededException;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;
import com.elastisys.scale.commons.net.retryable.retryhandlers.AbstractLimitedRetryHandler;

/**
 * A {@link RetryHandler} that waits for a Amazon EC2 machine instance to become
 * reachable. That is, it waits for the instance and system reachability tests
 * to succeed.
 * <p/>
 * It is intended to be used in a {@link RetryableRequest} in concert with the
 * {@link InstanceStatusRequester}.
 *
 * @see RetryableRequest
 * @see InstanceStatusRequester
 *
 * 
 *
 */
public class RetryUntilReachable extends
		AbstractLimitedRetryHandler<InstanceStatus> {

	public RetryUntilReachable(int maxRetries, long delay) {
		super(maxRetries, delay);
	}

	@Override
	public boolean isSuccessful(InstanceStatus instanceStatus) {
		return instanceStatus.getInstanceStatus().getStatus()
				.equalsIgnoreCase("ok")
				&& instanceStatus.getSystemStatus().getStatus()
						.equalsIgnoreCase("ok");
	}

	@Override
	public Action<InstanceStatus> maxRetriesExceeded(InstanceStatus withResponse) {
		String message = format("Maximum number of retries (%d) exceeded. "
				+ "Last response: %s", this.maxRetries, withResponse);
		RetryLimitExceededException failureReason = new RetryLimitExceededException(
				message);
		return Action.fail(failureReason);
	}

}
