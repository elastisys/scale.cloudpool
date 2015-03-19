package com.elastisys.scale.cloudpool.aws.ec2.server;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.impl.AwsEc2Client;
import com.elastisys.scale.cloudpool.aws.ec2.driver.Ec2PoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;

/**
 * Main class for starting the REST API server for an AWS EC2 {@link CloudPool}.
 */
public class Main {

	public static void main(String[] args) throws Exception {
		CloudPoolDriver driver = new Ec2PoolDriver(new AwsEc2Client());
		CloudPoolServer.main(new BaseCloudPool(driver), args);
	}
}
