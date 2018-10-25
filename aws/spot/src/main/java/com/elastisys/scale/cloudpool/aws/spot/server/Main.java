package com.elastisys.scale.cloudpool.aws.spot.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolOptions;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.impl.AwsSpotClient;
import com.elastisys.scale.cloudpool.aws.spot.driver.SpotPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;

/**
 * Main class for starting the REST API server for the AWS Spot Instance
 * {@link CloudPool}.
 */
public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        CloudPoolOptions options = CloudPoolServer.parseArgs(args);
        StateStorage stateStorage = StateStorage.builder(options.storageDir).build();

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
        // event bus on which to send alerts are to be distributed to registered
        // email/webhook recipients
        EventBus eventBus = new SynchronousEventBus(LOG);
        CloudPoolDriver driver = new SpotPoolDriver(new AwsSpotClient(), executor, eventBus);

        CloudPoolServer.main(new BaseCloudPool(stateStorage, driver, executor, eventBus), args);
    }
}
