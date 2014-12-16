package com.elastisys.scale.cloudadapters.aws.commons.requests.autoscaling;

import static java.lang.String.format;

import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.Activity;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.TerminateInstanceInAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.TerminateInstanceInAutoScalingGroupResult;
import com.elastisys.scale.cloudadapters.aws.commons.tasks.AutoScalingGroupRequester;
import com.elastisys.scale.cloudadapters.aws.commons.tasks.RetryUntilScalingGroupSizeReached;
import com.elastisys.scale.commons.net.retryable.Requester;
import com.elastisys.scale.commons.net.retryable.RetryHandler;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;

/**
 * A {@link Callable} task that, when executed, requests a particular Auto
 * Scaling Group member instance to be terminated. As a side-effect, the desired
 * capacity of the Auto Scaling Group is reduced as well. The task blocks until
 * the Auto Scaling group has reached its new size.
 * <p/>
 * The termination is a long-running {@link Activity} which can be tracked
 * through the returned object.
 *
 *
 */
public class TerminateAutoScalingGroupInstance extends
AmazonAutoScalingRequest<Void> {

	/** The name of the Auto Scaling Group from which to remove an instance. */
	private final String autoScalingGroup;
	/** The machine instance identifier to be terminated. */
	private final String instanceId;

	public TerminateAutoScalingGroupInstance(AWSCredentials awsCredentials,
			String region, String autoScalingGroup, String instanceId) {
		super(awsCredentials, region);
		this.autoScalingGroup = autoScalingGroup;
		this.instanceId = instanceId;
	}

	@Override
	public Void call() {
		AutoScalingGroup scalingGroup = new GetAutoScalingGroup(
				getAwsCredentials(), getRegion(), this.autoScalingGroup).call();
		int currentSize = scalingGroup.getDesiredCapacity();

		TerminateInstanceInAutoScalingGroupRequest request = new TerminateInstanceInAutoScalingGroupRequest()
		.withInstanceId(this.instanceId)
		.withShouldDecrementDesiredCapacity(true);
		TerminateInstanceInAutoScalingGroupResult result = getClient().getApi()
				.terminateInstanceInAutoScalingGroup(request);

		awaitGroupSize(currentSize - 1);
		return null;
	}

	/**
	 * Waits for the Auto Scaling group to reach the desired size.
	 *
	 * @param desiredSize
	 *
	 * @throws RuntimeException
	 */
	private void awaitGroupSize(int desiredSize) throws RuntimeException {
		Requester<AutoScalingGroup> requester = new AutoScalingGroupRequester(
				getClient(), this.autoScalingGroup);
		RetryHandler<AutoScalingGroup> retryHandler = new RetryUntilScalingGroupSizeReached(
				desiredSize, 30);
		String taskName = String.format("await-group-size{%d}", desiredSize);
		Callable<AutoScalingGroup> retryableRequest = new RetryableRequest<AutoScalingGroup>(
				requester, retryHandler, taskName);
		try {
			AutoScalingGroup group = retryableRequest.call();
			this.logger.info("group " + group.getAutoScalingGroupName()
					+ " reached size " + group.getInstances().size());
		} catch (Exception e) {
			throw new RuntimeException(format(
					"Gave up waiting for scaling group to reach "
							+ "capacity %d. You may want to consult the "
							+ "'as-describe-scaling-activities' command "
							+ "for troubleshooting. Failure message: %s",
							desiredSize, e.getMessage()), e);
		}
	}
}
