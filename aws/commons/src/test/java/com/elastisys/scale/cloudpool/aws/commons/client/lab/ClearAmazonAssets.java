package com.elastisys.scale.cloudpool.aws.commons.client.lab;

/**
 * Deletes/terminates all computing assets in a given Amazon AWS region.
 * 
 * 
 */
public class ClearAmazonAssets extends AbstractClient {

	// TODO: set to region where you want to delete Elastic Load Balancers
	private static final String region = "us-east-1";

	public static void main(String[] args) throws Exception {
		DeleteLoadBalancersMain.main(args);
		DeleteAutoScalingGroupsMain.main(args);
		DeleteLaunchConfigurationsMain.main(args);
		TerminateAllInstancesMain.main(args);
	}
}
