package com.elastisys.scale.cloudpool.aws.commons.client.lab;

import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.TerminateAutoScalingGroupInstance;

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

	// TODO: set to instance id of machine to terminate
	private static final String instanceId = "i-ff37c58d";

	public static void main(String[] args) throws Exception {
		logger.info("Terminating instance {} from its Auto Scaling Group",
				instanceId);
		new TerminateAutoScalingGroupInstance(AWS_CREDENTIALS, region,
				instanceId).call();
	}

}
