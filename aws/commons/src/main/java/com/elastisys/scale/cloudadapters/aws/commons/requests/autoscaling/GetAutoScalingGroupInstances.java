package com.elastisys.scale.cloudadapters.aws.commons.requests.autoscaling;

import static com.elastisys.scale.cloudadapters.aws.commons.predicates.InstancePredicates.instancesPresent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.elastisys.scale.cloudadapters.aws.commons.functions.AwsAutoScalingFunctions;
import com.elastisys.scale.cloudadapters.aws.commons.requests.ec2.GetInstances;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.google.common.collect.Lists;

/**
 * A {@link Callable} task that, when executed, requests details about all
 * instances belonging to a particular AWS Auto Scaling Group in a region.
 *
 * @see AutoScalingInstance
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

		try {
			return listGroupInstances(autoScalingGroup);
		} catch (Exception e) {
			throw new RuntimeException(String.format(
					"failed waiting for auto scaling group members: %s",
					e.getMessage()), e);
		}
	}

	private List<Instance> listGroupInstances(AutoScalingGroup autoScalingGroup)
			throws Exception {
		List<String> instanceIds = Lists.transform(
				autoScalingGroup.getInstances(),
				AwsAutoScalingFunctions.toAutoScalingInstanceId());
		if (instanceIds.isEmpty()) {
			// note: we don't want to call get instances with an emtpy list
			// since this causes DescribeInstances to get *all* instances in the
			// region (not just the ones in our Auto Scaling Group, which is
			// what we want)
			return new ArrayList<>();
		}

		List<Filter> filters = Collections.emptyList();
		Callable<List<Instance>> requester = new GetInstances(
				getAwsCredentials(), getRegion(), instanceIds, filters);

		int initialDelay = 1;
		int maxAttempts = 10; // max 2 ^ 9 - 1 seconds = 511 seconds
		String name = String.format("await-describe-instances");
		Retryable<List<Instance>> retryer = Retryers.exponentialBackoffRetryer(
				name, requester, initialDelay, TimeUnit.SECONDS, maxAttempts,
				instancesPresent(instanceIds));

		return retryer.call();
	}
}
