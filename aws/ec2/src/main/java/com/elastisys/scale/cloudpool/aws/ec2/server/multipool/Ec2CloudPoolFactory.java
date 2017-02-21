package com.elastisys.scale.cloudpool.aws.ec2.server.multipool;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.impl.AwsEc2Client;
import com.elastisys.scale.cloudpool.aws.ec2.driver.Ec2PoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.StateStorage;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.multipool.impl.CloudPoolFactory;

/**
 * A {@link CloudPoolFactory} that creates AWS EC2 {@link CloudPool} instances.
 */
public class Ec2CloudPoolFactory implements CloudPoolFactory {

    /** The number of threads allocated to each {@link CloudPool} instance. */
    private static final int THREADS_PER_CLOUDPOOL = 2;

    @Override
    public CloudPool create(ThreadFactory threadFactory, File stateDir) throws CloudPoolException {

        StateStorage stateStorage = StateStorage.builder(stateDir).build();
        CloudPoolDriver driver = new Ec2PoolDriver(new AwsEc2Client());

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(THREADS_PER_CLOUDPOOL, threadFactory);

        return new BaseCloudPool(stateStorage, driver, executor);
    }

}
