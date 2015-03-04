package com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling;

import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.AttachInstancesRequest;

/**
 * A {@link Callable} task that, when executed, requests a particular instance
 * to be attached to an Auto Scaling Group. As a side-effect, the desired
 * capacity of the Auto Scaling Group is incremented.
 */
public class AttachAutoScalingGroupInstance extends
		AmazonAutoScalingRequest<Void> {

	/** The name of the Auto Scaling Group. */
	private final String autoScalingGroup;
	/** The machine instance identifier to be attached. */
	private final String instanceId;

	public AttachAutoScalingGroupInstance(AWSCredentials awsCredentials,
			String region, String autoScalingGroup, String instanceId) {
		super(awsCredentials, region);
		this.autoScalingGroup = autoScalingGroup;
		this.instanceId = instanceId;
	}

	@Override
	public Void call() {
		AttachInstancesRequest request = new AttachInstancesRequest()
				.withAutoScalingGroupName(this.autoScalingGroup)
				.withInstanceIds(this.instanceId);
		getClient().getApi().attachInstances(request);

		return null;
	}
}
