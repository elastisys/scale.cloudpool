package com.elastisys.scale.cloudadapters.aws.commons.requests.autoscaling;

import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;

/**
 * A {@link Callable} task that, when executed, requests the listing of all AWS
 * Auto Scaling Groups in a region.
 */
public class GetAutoScalingGroups extends
		AmazonAutoScalingRequest<List<AutoScalingGroup>> {

	public GetAutoScalingGroups(AWSCredentials awsCredentials, String region) {
		super(awsCredentials, region);
	}

	@Override
	public List<AutoScalingGroup> call() {
		DescribeAutoScalingGroupsResult result = getClient().getApi()
				.describeAutoScalingGroups();
		return result.getAutoScalingGroups();
	}

}
