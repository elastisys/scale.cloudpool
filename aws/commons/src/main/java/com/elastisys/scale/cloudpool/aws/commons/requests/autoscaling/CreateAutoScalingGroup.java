package com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling;

import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;

/**
 * A {@link Callable} task that, when executed, requests the creation of an AWS
 * Auto Scaling Group.
 */
public class CreateAutoScalingGroup extends AmazonAutoScalingRequest<Void> {

	/** The name of the auto-scaling group to be created. */
	private final String autoScalingGroupName;

	/**
	 * The name of the launch configuration to use for the Auto Scaling group to
	 * be created.
	 */
	private final String launchConfigurationName;

	/**
	 * The availability zone(s) (within region) where member machines of the
	 * Auto Scaling Group to be created can be placed.
	 */
	private final List<String> availabilityZones;

	/**
	 * The load-balancer(s) that will front the Auto Scaling Group (can be
	 * empty).
	 */
	private final List<String> loadBalancerNames;

	/** The initial minimum size of the Auto Scaling Group to be created. */
	private final int minSize;
	/** The initial maximum size of the Auto Scaling Group to be created. */
	private final int maxSize;
	/** The initial desired size of the Auto Scaling Group to be created. */
	private final int desiredSize;
	/**
	 * The termination policy to use for the Auto Scaling Group to be created.
	 * The valid values are: OldestInstance, OldestLaunchConfiguration,
	 * NewestInstance, ClosestToNextInstanceHour, and Default.
	 * <p/>
	 * See <a href=
	 * "http://docs.aws.amazon.com/AutoScaling/latest/DeveloperGuide/us-termination-policy.html"
	 * >the documentation</a> for details.
	 * */
	private final String terminationPolicy;

	public CreateAutoScalingGroup(AWSCredentials awsCredentials, String region,
			String autoScalingGroupName, String launchConfigurationName,
			List<String> availabilityZones, List<String> loadBalancerNames,
			int minSize, int maxSize, int desiredSize, String terminationPolicy) {
		super(awsCredentials, region);
		this.autoScalingGroupName = autoScalingGroupName;
		this.launchConfigurationName = launchConfigurationName;
		this.availabilityZones = availabilityZones;
		this.loadBalancerNames = loadBalancerNames;
		this.minSize = minSize;
		this.maxSize = maxSize;
		this.desiredSize = desiredSize;
		this.terminationPolicy = terminationPolicy;
	}

	@Override
	public Void call() {
		CreateAutoScalingGroupRequest request = new CreateAutoScalingGroupRequest()
				.withAutoScalingGroupName(this.autoScalingGroupName)
				.withMinSize(this.minSize).withMaxSize(this.maxSize)
				.withDesiredCapacity(this.desiredSize)
				.withLaunchConfigurationName(this.launchConfigurationName)
				.withAvailabilityZones(this.availabilityZones)
				.withTerminationPolicies(this.terminationPolicy)
				.withLoadBalancerNames(this.loadBalancerNames)
				.withDefaultCooldown(0);
		getClient().getApi().createAutoScalingGroup(request);
		return null;
	}

}
