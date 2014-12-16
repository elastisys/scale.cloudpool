package com.elastisys.scale.cloudadapters.aws.commons.requests.autoscaling;

import static java.lang.String.format;

import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityRequest;
import com.elastisys.scale.cloudadapters.aws.commons.tasks.AutoScalingGroupRequester;
import com.elastisys.scale.cloudadapters.aws.commons.tasks.RetryUntilScalingGroupSizeReached;
import com.elastisys.scale.commons.net.retryable.Requester;
import com.elastisys.scale.commons.net.retryable.RetryHandler;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;

/**
 * A {@link Callable} task that, when executed, requests the desired capacity
 * (size) of an AWS Auto Scaling Group to be changed. The task blocks until the
 * Auto Scaling group has reached its new size.
 * <p/>
 * In case of shrinking the group size, the termination victim selection is
 * carried out according to the <a href=
 * "http://docs.aws.amazon.com/AutoScaling/latest/DeveloperGuide/us-termination-policy.html"
 * >termination policy</a> of the Auto Scaling group. Note that the victim
 * instance is terminated immediately; nothing is done to keep it until the end
 * of its billing hour. To pick a particular instance for termination, use
 * {@link TerminateAutoScalingGroupInstance}.
 *
 *
 *
 */
public class SetDesiredAutoScalingGroupSize extends
AmazonAutoScalingRequest<Void> {

	/** The name of the Auto Scaling Group whose size is to be changed. */
	private final String autoScalingGroup;
	/** The new desired capacity of the Auto Scaling group. */
	private final int desiredCapacity;

	public SetDesiredAutoScalingGroupSize(AWSCredentials awsCredentials,
			String region, String autoScalingGroup, int desiredCapacity) {
		super(awsCredentials, region);
		this.autoScalingGroup = autoScalingGroup;
		this.desiredCapacity = desiredCapacity;
	}

	@Override
	public Void call() throws RuntimeException {
		setDesiredSize();
		awaitGroupSize();
		return null;
	}

	/**
	 * Updates the Auto Scaling group size by (re-)setting the desired capacity
	 * only.
	 */
	private void setDesiredSize() {
		SetDesiredCapacityRequest request = new SetDesiredCapacityRequest()
		.withAutoScalingGroupName(this.autoScalingGroup)
		.withDesiredCapacity(this.desiredCapacity)
		.withHonorCooldown(false);
		getClient().getApi().setDesiredCapacity(request);
	}

	/**
	 * Waits for the Auto Scaling group to reach the desired size.
	 *
	 * @throws RuntimeException
	 */
	private void awaitGroupSize() throws RuntimeException {
		Requester<AutoScalingGroup> requester = new AutoScalingGroupRequester(
				getClient(), this.autoScalingGroup);
		RetryHandler<AutoScalingGroup> retryHandler = new RetryUntilScalingGroupSizeReached(
				this.desiredCapacity, 30);
		String taskName = String.format("await-group-size{%d}",
				this.desiredCapacity);
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
							+ "scaling activities history of you Auto Scaling "
							+ "Group for troubleshooting. Failure message: %s",
							this.desiredCapacity, e.getMessage()), e);
		}
	}
}
