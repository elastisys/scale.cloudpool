package com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling;

import java.util.concurrent.Callable;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.DetachInstancesRequest;

/**
 * A {@link Callable} task that, when executed, requests a particular member
 * instance to be detached from an Auto Scaling Group. As a side-effect, the
 * desired capacity of the Auto Scaling Group is decremented.
 */
public class DetachAutoScalingGroupInstance
		extends AmazonAutoScalingRequest<Void> {

	/** The name of the Auto Scaling Group. */
	private final String autoScalingGroup;
	/** The machine instance identifier to be detached. */
	private final String instanceId;

	public DetachAutoScalingGroupInstance(AWSCredentials awsCredentials,
			String region, ClientConfiguration clientConfig,
			String autoScalingGroup, String instanceId) {
		super(awsCredentials, region, clientConfig);
		this.autoScalingGroup = autoScalingGroup;
		this.instanceId = instanceId;
	}

	@Override
	public Void call() {
		DetachInstancesRequest request = new DetachInstancesRequest()
				.withAutoScalingGroupName(this.autoScalingGroup)
				.withInstanceIds(this.instanceId)
				.withShouldDecrementDesiredCapacity(true);
		getClient().getApi().detachInstances(request);

		return null;
	}
}
