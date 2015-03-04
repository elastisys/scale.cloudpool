package com.elastisys.scale.cloudpool.aws.commons.client.lab;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.List;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.autoscaling.model.AlreadyExistsException;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.CreateAutoScalingGroup;

public class CreateAutoScalingGroupMain extends AbstractClient {

	// TODO: set to the name of the auto-scaling group you want to create
	private static final String autoScalingGroupName = "end2end-scalinggroup";

	// TODO: set to the name of the auto-scaling launch configuration create
	private static final String launchConfigurationName = "end2endtest-launch-config";

	// TODO: set to region where you want machine to be hosted
	private static final String region = "us-east-1";
	// TODO: set to zones (within region) to place machine in
	private static final List<String> availabilityZones = Arrays.asList(
			"us-east-1a", "us-east-1b");

	// TODO: set to min size of group
	private static final int minSize = 0;
	// TODO: set to max size of group
	private static final int maxSize = 4;
	// TODO: set to initial size of group
	private static final int initialSize = 0;
	// TODO: set to desired termination policy: OldestInstance,
	// OldestLaunchConfiguration, NewestInstance, ClosestToNextInstanceHour,
	// Default. See
	// http://docs.aws.amazon.com/AutoScaling/latest/DeveloperGuide/us-termination-policy.html
	private static final String terminationPolicy = "OldestInstance";
	// TODO: set to the list of load-balancers that will front the group (can be
	// empty)
	private static final List<String> loadBalancerNames = Arrays
			.asList("end2endtest-load-balancer");

	public static void main(String[] args) throws Exception {
		logger.info(format("Creating auto-scaling group '%s'",
				autoScalingGroupName));
		try {
			new CreateAutoScalingGroup(new PropertiesCredentials(
					credentialsFile), region, autoScalingGroupName,
					launchConfigurationName, availabilityZones,
					loadBalancerNames, minSize, maxSize, initialSize,
					terminationPolicy).call();
		} catch (AlreadyExistsException e) {
			logger.warn(format("Ignoring creation of scaling group %s,"
					+ " which already exists.", autoScalingGroupName));
		}
	}
}
