package com.elastisys.scale.cloudpool.aws.spot.server.multipool;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.impl.AwsSpotClient;
import com.elastisys.scale.cloudpool.aws.spot.driver.SpotPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.multipool.impl.CloudPoolFactory;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;

/**
 * A {@link CloudPoolFactory} that creates AWS Spot {@link CloudPool} instances.
 */
public class SpotCloudPoolFactory implements CloudPoolFactory {
    private static final Logger LOG = LoggerFactory.getLogger(SpotCloudPoolFactory.class);

    /** The number of threads allocated to each {@link CloudPool} instance. */
    private static final int THREADS_PER_CLOUDPOOL = 2;

    @Override
    public CloudPool create(ThreadFactory threadFactory, File stateDir) throws CloudPoolException {

        StateStorage stateStorage = StateStorage.builder(stateDir).build();

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(THREADS_PER_CLOUDPOOL, threadFactory);
        // event bus on which to send alerts are to be distributed to registered
        // email/webhook recipients
        EventBus eventBus = new SynchronousEventBus(LOG);

        CloudPoolDriver driver = new SpotPoolDriver(new AwsSpotClient(), executor, eventBus);

        return new BaseCloudPool(stateStorage, driver, executor);
    }

}
