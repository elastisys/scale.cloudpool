package com.elastisys.scale.cloudadapters.aws.commons.requests.autoscaling;

import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.elastisys.scale.cloudadapters.aws.commons.tasks.AutoScalingGroupRequester;
import com.elastisys.scale.cloudadapters.aws.commons.tasks.RetryUntilScalingGroupSizeReached;
import com.elastisys.scale.commons.net.retryable.Requester;
import com.elastisys.scale.commons.net.retryable.RetryHandler;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;

/**
 * A {@link Callable} task that, when executed, waits for an AWS Auto Scaling
 * Group to reach a certain size.
 *
 *
 */
public class AwaitAutoScalingGroupSize extends
AmazonAutoScalingRequest<AutoScalingGroup> {

	/** The Auto Scaling Group of interest. */
	private final String autoScalingGroupName;
	/**
	 * The target size of the Auto Scaling Group. When executed, the task will
	 * wait for the group to reach this size.
	 */
	private final int targetSize;

	/** Maximum number of times to poll for group size. */
	private final int maxRetries;

	/**
	 * Constructs a new {@link AwaitAutoScalingGroupSize} task.
	 *
	 * @param awsCredentials
	 *            AWS security credentials for the account to be used.
	 * @param region
	 *            The AWS region that the request will be sent to.
	 * @param autoScalingGroupName
	 *            The Auto Scaling Group of interest.
	 * @param targetSize
	 *            The target size of the Auto Scaling Group. When executed, the
	 *            task will wait for the group to reach this size.
	 * @param maxRetries
	 *            Maximum number of times to poll for group size.
	 * @param retryDelay
	 *            Delay (in ms) between each group size poll.
	 */
	public AwaitAutoScalingGroupSize(AWSCredentials awsCredentials,
			String region, String autoScalingGroupName, int targetSize,
			int maxRetries) {
		super(awsCredentials, region);
		this.autoScalingGroupName = autoScalingGroupName;
		this.targetSize = targetSize;
		this.maxRetries = maxRetries;
	}

	@Override
	public AutoScalingGroup call() throws Exception {
		GetAutoScalingGroup getTask = new GetAutoScalingGroup(
				getAwsCredentials(), getRegion(), this.autoScalingGroupName);
		AutoScalingGroup autoScalingGroup = getTask.call();
		return awaitGroupSize(autoScalingGroup, this.targetSize);
	}

	/**
	 * Waits for the Auto Scaling group to reach a given size.
	 *
	 * @param autoScalingGroup
	 * @param targetSize
	 * @return
	 * @throws RuntimeException
	 */
	private AutoScalingGroup awaitGroupSize(AutoScalingGroup autoScalingGroup,
			int targetSize) throws RuntimeException {
		String groupName = autoScalingGroup.getAutoScalingGroupName();
		Requester<AutoScalingGroup> requester = new AutoScalingGroupRequester(
				getClient(), groupName);
		RetryHandler<AutoScalingGroup> retryHandler = new RetryUntilScalingGroupSizeReached(
				targetSize, this.maxRetries);
		Callable<AutoScalingGroup> retryableRequest = new RetryableRequest<AutoScalingGroup>(
				requester, retryHandler, "await group size");
		try {
			return retryableRequest.call();
		} catch (Exception e) {
			throw new RuntimeException(
					"Failed to wait for scaling group to reach capacity "
							+ targetSize + ": " + e.getMessage(), e);
		}
	}
}
