package com.elastisys.scale.cloudpool.google.compute.server.multipool;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.google.commons.api.compute.impl.StandardComputeClient;
import com.elastisys.scale.cloudpool.google.compute.driver.GoogleComputeEnginePoolDriver;
import com.elastisys.scale.cloudpool.multipool.impl.CloudPoolFactory;

/**
 * A {@link CloudPoolFactory} that creates Google Compute Engine
 * {@link CloudPool} instances.
 */
public class GoogleComputeEngineCloudPoolFactory implements CloudPoolFactory {

    /** The number of threads allocated to each {@link CloudPool} instance. */
    private static final int THREADS_PER_CLOUDPOOL = 2;

    @Override
    public CloudPool create(ThreadFactory threadFactory, File stateDir) throws CloudPoolException {

        StateStorage stateStorage = StateStorage.builder(stateDir).build();
        CloudPoolDriver driver = new GoogleComputeEnginePoolDriver(new StandardComputeClient());

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(THREADS_PER_CLOUDPOOL, threadFactory);

        return new BaseCloudPool(stateStorage, driver, executor);
    }

}
