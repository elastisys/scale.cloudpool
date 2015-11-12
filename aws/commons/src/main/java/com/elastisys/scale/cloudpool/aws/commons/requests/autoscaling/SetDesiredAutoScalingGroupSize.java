package com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling;

import java.util.concurrent.Callable;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityRequest;

/**
 * A {@link Callable} task that, when executed, requests the desired capacity of
 * an AWS Auto Scaling Group to be changed. The task sets the new desired
 * capacity and returns immediately. It does *not* wait for the request to take
 * effect, simply because there is no bulletproof method of knowing when this
 * particular desired size request has taken effect. Waiting for the group size
 * to reach the desired size is problematic, since the desired size may be set
 * to some other value while we are waiting.
 * <p/>
 * In case of shrinking the group size, the termination victim selection is
 * carried out according to the <a href=
 * "http://docs.aws.amazon.com/AutoScaling/latest/DeveloperGuide/us-termination-policy.html"
 * >termination policy</a> of the Auto Scaling group. Note that the victim
 * instance is terminated immediately; nothing is done to keep it until the end
 * of its billing hour. To pick a particular instance for termination, use
 * {@link TerminateAutoScalingGroupInstance}.
 */
public class SetDesiredAutoScalingGroupSize
		extends AmazonAutoScalingRequest<Void> {

	/** The name of the Auto Scaling Group whose size is to be changed. */
	private final String autoScalingGroup;
	/** The new desired capacity of the Auto Scaling group. */
	private final int desiredCapacity;

	public SetDesiredAutoScalingGroupSize(AWSCredentials awsCredentials,
			String region, ClientConfiguration clientConfig,
			String autoScalingGroup, int desiredCapacity) {
		super(awsCredentials, region, clientConfig);
		this.autoScalingGroup = autoScalingGroup;
		this.desiredCapacity = desiredCapacity;
	}

	@Override
	public Void call() throws RuntimeException {
		setDesiredSize();
		return null;
	}

	/**
	 * Updates the Auto Scaling group size by setting the desired capacity only.
	 */
	private void setDesiredSize() {
		SetDesiredCapacityRequest request = new SetDesiredCapacityRequest()
				.withAutoScalingGroupName(this.autoScalingGroup)
				.withDesiredCapacity(this.desiredCapacity)
				.withHonorCooldown(false);
		getClient().getApi().setDesiredCapacity(request);
	}
}
