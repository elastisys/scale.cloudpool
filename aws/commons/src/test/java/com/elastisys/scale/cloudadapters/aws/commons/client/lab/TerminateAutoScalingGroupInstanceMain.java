package com.elastisys.scale.cloudadapters.aws.commons.client.lab;

import com.amazonaws.auth.PropertiesCredentials;
import com.elastisys.scale.cloudadapters.aws.commons.requests.autoscaling.TerminateAutoScalingGroupInstance;

/**
 * Terminates a particular machine instance in an auto-scaling group and
 * decreases the group's desired capacity and minSize.
 * 
 * 
 * 
 */
public class TerminateAutoScalingGroupInstanceMain extends AbstractClient {

	// TODO: set to region where machine to terminate is hosted
	private static final String region = "us-east-1";

	// TODO: set to name of auto scaling group
	private static final String autoScalingGroup = "MyAutoScalingGroup";

	// TODO: set to instance id of machine to terminate
	private static final String instanceId = "i-ff37c58d";

	public static void main(String[] args) throws Exception {
		logger.info("Terminating instance {} from its Auto Scaling Group",
				instanceId);
		new TerminateAutoScalingGroupInstance(new PropertiesCredentials(
				credentialsFile), region, autoScalingGroup, instanceId).call();
	}

}
