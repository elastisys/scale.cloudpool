package com.elastisys.scale.cloudadapters.aws.commons.tasks;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.lang.String.format;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.elastisys.scale.cloudadapters.aws.commons.client.AutoScalingClient;
import com.elastisys.scale.commons.net.retryable.Requester;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;

/**
 * A {@link Requester} that returns details regarding a specific Amazon
 * {@link AutoScalingGroup}.
 * <p/>
 * It can, for example, be used in a {@link RetryableRequest} in concert with
 * the {@link RetryUntilScalingGroupSizeReached} to wait for an Auto Scaling
 * group to reach a given size.
 * 
 * 
 */
public class AutoScalingGroupRequester implements Requester<AutoScalingGroup> {

	static Logger logger = LoggerFactory
			.getLogger(AutoScalingGroupRequester.class);

	/** The API client to use. */
	private final AutoScalingClient client;
	/** The name of the Auto Scaling group. */
	private final String autoScalingGroup;

	/**
	 * Constructs a new {@link AutoScalingGroupRequester}.
	 * 
	 * @param autoScalingClient
	 *            The API client to use.
	 * @param autoScalingGroup
	 *            The name of the Auto Scaling group.
	 */
	public AutoScalingGroupRequester(AutoScalingClient autoScalingClient,
			String autoScalingGroup) {
		this.client = autoScalingClient;
		this.autoScalingGroup = autoScalingGroup;
	}

	@Override
	public AutoScalingGroup call() throws Exception {
		DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest()
				.withAutoScalingGroupNames(this.autoScalingGroup);
		DescribeAutoScalingGroupsResult result = this.client.getApi()
				.describeAutoScalingGroups(request);
		List<AutoScalingGroup> autoScalingGroups = result
				.getAutoScalingGroups();
		if (autoScalingGroups.isEmpty()) {
			throw new IllegalArgumentException(format(
					"Auto-scaling group '%s' doesn't exist in region '%s'.",
					this.autoScalingGroup, this.client.getRegion()));
		}
		return getOnlyElement(autoScalingGroups);
	}
}
