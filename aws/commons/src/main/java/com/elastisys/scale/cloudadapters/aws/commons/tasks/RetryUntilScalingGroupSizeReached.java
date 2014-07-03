package com.elastisys.scale.cloudadapters.aws.commons.tasks;

import static java.lang.String.format;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.elastisys.scale.commons.net.retryable.Action;
import com.elastisys.scale.commons.net.retryable.RetryHandler;
import com.elastisys.scale.commons.net.retryable.RetryLimitExceededException;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;
import com.elastisys.scale.commons.net.retryable.retryhandlers.AbstractLimitedRetryHandler;

/**
 * A {@link RetryHandler} that waits for a Amazon Auto Scaling group to reach a
 * given size. It is intended to be used in a {@link RetryableRequest} in
 * concert with the {@link AutoScalingGroupRequester}.
 *
 * @see RetryableRequest
 *
 * 
 */
public class RetryUntilScalingGroupSizeReached extends
		AbstractLimitedRetryHandler<AutoScalingGroup> {

	/** The target Auto Scaling group size to wait for. */
	private final int targetGroupSize;

	/**
	 * Constructs a new {@link RetryUntilScalingGroupSizeReached} that will wait
	 * a limited number of retries for the group to reach the target size.
	 *
	 * @param targetGroupSize
	 *            The target Auto Scaling group size to wait for.
	 * @param maxRetries
	 *            Maximum number of retries. A value less than or equal to
	 *            {@code 0} signifies an infinite number of retries.
	 * @param delay
	 *            Delay (in ms) between poll attempts.
	 */
	public RetryUntilScalingGroupSizeReached(int targetGroupSize,
			int maxRetries, long delay) {
		super(maxRetries, delay);
		this.targetGroupSize = targetGroupSize;
	}

	@Override
	public boolean isSuccessful(AutoScalingGroup response) {
		return response.getInstances().size() == this.targetGroupSize;
	}

	@Override
	public Action<AutoScalingGroup> maxRetriesExceeded(
			AutoScalingGroup withResponse) {
		String message = format("Maximum number of retries (%d) exceeded. "
				+ "Last response: %s", this.maxRetries, withResponse);
		RetryLimitExceededException failureReason = new RetryLimitExceededException(
				message);
		return Action.fail(failureReason);
	}
}
