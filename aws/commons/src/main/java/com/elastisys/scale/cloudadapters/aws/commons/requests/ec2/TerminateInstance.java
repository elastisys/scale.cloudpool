package com.elastisys.scale.cloudadapters.aws.commons.requests.ec2;

import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.google.common.collect.Iterables;

/**
 * A {@link Callable} task that, when executed, requests a AWS EC2 machine
 * instance to be terminated.
 * 
 * 
 */
public class TerminateInstance extends AmazonEc2Request<InstanceStateChange> {

	/** The machine instance id to be terminated. */
	private final String instanceId;

	public TerminateInstance(AWSCredentials awsCredentials, String region,
			String instanceId) {
		super(awsCredentials, region);
		this.instanceId = instanceId;
	}

	@Override
	public InstanceStateChange call() {
		TerminateInstancesRequest request = new TerminateInstancesRequest()
				.withInstanceIds(this.instanceId);
		TerminateInstancesResult result = getClient().getApi()
				.terminateInstances(request);
		return Iterables.getOnlyElement(result.getTerminatingInstances());
	}

}
