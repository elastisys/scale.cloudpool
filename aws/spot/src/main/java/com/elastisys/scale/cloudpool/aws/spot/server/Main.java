package com.elastisys.scale.cloudpool.aws.spot.server;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.impl.AwsSpotClient;
import com.elastisys.scale.cloudpool.aws.spot.driver.SpotPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.google.common.eventbus.EventBus;

/**
 * Main class for starting the REST API server for the AWS Spot Instance
 * {@link CloudPool}.
 */
public class Main {

	public static void main(String[] args) throws Exception {
		// event bus on which to send alerts are to be distributed to registered
		// email/webhook recipients
		EventBus eventBus = new EventBus();
		CloudPoolDriver driver = new SpotPoolDriver(new AwsSpotClient(),
				eventBus);
		CloudPoolServer.main(new BaseCloudPool(driver, eventBus), args);
	}
}
