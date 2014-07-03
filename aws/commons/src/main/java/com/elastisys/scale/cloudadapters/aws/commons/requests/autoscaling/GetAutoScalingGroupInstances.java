package com.elastisys.scale.cloudadapters.aws.commons.requests.autoscaling;

import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.elastisys.scale.cloudadapters.aws.commons.functions.AwsAutoScalingFunctions;
import com.elastisys.scale.cloudadapters.aws.commons.requests.ec2.GetInstances;
import com.google.common.collect.Lists;

/**
 * A {@link Callable} task that, when executed, requests details about all
 * instances belonging to a particular AWS Auto Scaling Group in a region.
 *
 * @see AutoScalingInstance
 * 
 */
public class GetAutoScalingGroupInstances extends
		AmazonAutoScalingRequest<List<Instance>> {

	/** The name of the {@link AutoScalingGroup} of interest. */
	private final String groupName;

	/**
	 * Constructs a new {@link GetAutoScalingGroupInstances} task.
	 *
	 * @param awsCredentials
	 * @param region
	 * @param groupName
	 */
	public GetAutoScalingGroupInstances(AWSCredentials awsCredentials,
			String region, String groupName) {
		super(awsCredentials, region);
		this.groupName = groupName;
	}

	@Override
	public List<Instance> call() {
		AutoScalingGroup autoScalingGroup = new GetAutoScalingGroup(
				getAwsCredentials(), getRegion(), this.groupName).call();

		return listGroupInstances(autoScalingGroup);
	}

	private List<Instance> listGroupInstances(AutoScalingGroup autoScalingGroup) {
		List<String> instanceIds = Lists.transform(
				autoScalingGroup.getInstances(),
				AwsAutoScalingFunctions.toAutoScalingInstanceId());
		GetInstances listInstances = new GetInstances(getAwsCredentials(),
				getRegion(), new Filter("instance-id", instanceIds));
		return listInstances.call();
	}
}
