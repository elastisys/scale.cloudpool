package com.elastisys.scale.cloudpool.aws.spot.server;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolOptions;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.impl.AwsSpotClient;
import com.elastisys.scale.cloudpool.aws.spot.driver.SpotPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.google.common.eventbus.EventBus;

/**
 * Main class for starting the REST API server for the AWS Spot Instance
 * {@link CloudPool}.
 */
public class Main {

	public static void main(String[] args) throws Exception {
		CloudPoolOptions options = CloudPoolServer.parseArgs(args);
		StateStorage stateStorage = StateStorage.builder(options.storageDir)
				.build();
		// event bus on which to send alerts are to be distributed to registered
		// email/webhook recipients
		EventBus eventBus = new EventBus();
		CloudPoolDriver driver = new SpotPoolDriver(new AwsSpotClient(),
				eventBus);
		CloudPoolServer.main(new BaseCloudPool(stateStorage, driver, eventBus),
				args);
	}
}
