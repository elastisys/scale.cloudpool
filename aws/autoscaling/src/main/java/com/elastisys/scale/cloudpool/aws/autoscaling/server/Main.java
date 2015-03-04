package com.elastisys.scale.cloudpool.aws.autoscaling.server;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.AwsAsPoolDriver;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.client.AwsAutoScalingClient;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;

/**
 * Main class for starting the REST API server for the AWS Auto Scaling Group
 * {@link CloudPool}.
 */
public class Main {

	public static void main(String[] args) throws Exception {
		AwsAsPoolDriver driver = new AwsAsPoolDriver(new AwsAutoScalingClient());
		CloudPoolServer.main(new BaseCloudPool(driver), args);
	}
}
