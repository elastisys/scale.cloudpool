package com.elastisys.scale.cloudpool.aws.autoscaling.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.server.CloudPoolOptions;
import com.elastisys.scale.cloudpool.api.server.CloudPoolServer;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.AwsAsPoolDriver;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.client.AwsAutoScalingClient;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;

/**
 * Main class for starting the REST API server for the AWS Auto Scaling Group
 * {@link CloudPool}.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        CloudPoolOptions options = CloudPoolServer.parseArgs(args);
        StateStorage stateStorage = StateStorage.builder(options.storageDir).build();
        AwsAsPoolDriver driver = new AwsAsPoolDriver(new AwsAutoScalingClient());
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

        CloudPoolServer.main(new BaseCloudPool(stateStorage, driver, executor), args);
    }
}
