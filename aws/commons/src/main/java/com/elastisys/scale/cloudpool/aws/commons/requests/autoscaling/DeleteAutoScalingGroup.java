package com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling;

import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;

/**
 * A {@link Callable} task that, when executed, requests the deletion of an AWS
 * Auto Scaling Group.
 * <p/>
 * <b>Note: all member instances of the group will be terminated.</b>
 */
public class DeleteAutoScalingGroup extends AmazonAutoScalingRequest<Void> {

	/** The Auto Scaling Group to be deleted. */
	private final String autoScalingGroupName;

	public DeleteAutoScalingGroup(AWSCredentials awsCredentials, String region,
			String autoScalingGroupName) {
		super(awsCredentials, region);
		this.autoScalingGroupName = autoScalingGroupName;
	}

	@Override
	public Void call() {
		DeleteAutoScalingGroupRequest request = new DeleteAutoScalingGroupRequest()
				.withAutoScalingGroupName(this.autoScalingGroupName)
				.withForceDelete(true);
		getClient().getApi().deleteAutoScalingGroup(request);
		return null;
	}
}
