package com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.lang.String.format;

import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;

/**
 * A {@link Callable} task that, when executed, requests details about a
 * particular AWS Auto Scaling Group in a region.
 */
public class GetAutoScalingGroup extends AmazonAutoScalingRequest<AutoScalingGroup> {

    /** The name of the {@link AutoScalingGroup} of interest. */
    private final String groupName;

    /**
     * Constructs a new {@link GetAutoScalingGroup} task.
     *
     * @param awsCredentials
     * @param region
     * @param clientConfig
     *            Client configuration options such as connection timeout, etc.
     * @param groupName
     */
    public GetAutoScalingGroup(AWSCredentials awsCredentials, String region, ClientConfiguration clientConfig,
            String groupName) {
        super(awsCredentials, region, clientConfig);
        this.groupName = groupName;
    }

    @Override
    public AutoScalingGroup call() {
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest()
                .withAutoScalingGroupNames(this.groupName);
        DescribeAutoScalingGroupsResult result = getClient().getApi().describeAutoScalingGroups(request);
        List<AutoScalingGroup> autoScalingGroups = result.getAutoScalingGroups();
        if (autoScalingGroups.isEmpty()) {
            throw new IllegalArgumentException(format("Auto Scaling Group '%s' doesn't exist in region '%s'.",
                    this.groupName, getClient().getRegion()));
        }
        return getOnlyElement(autoScalingGroups);
    }

}
