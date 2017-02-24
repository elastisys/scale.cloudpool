package com.elastisys.scale.cloudpool.kubernetes.server.multipool;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.kubernetes.KubernetesCloudPool;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.impl.StandardApiServerClient;
import com.elastisys.scale.cloudpool.multipool.impl.CloudPoolFactory;

/**
 * A {@link CloudPoolFactory} that creates {@link KubernetesCloudPool}
 * instances.
 */
public class KubernetesCloudPoolFactory implements CloudPoolFactory {

    /** The number of threads allocated to each {@link CloudPool} instance. */
    private static final int THREADS_PER_CLOUDPOOL = 2;

    @Override
    public CloudPool create(ThreadFactory threadFactory, File stateDir) throws CloudPoolException {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(THREADS_PER_CLOUDPOOL, threadFactory);

        return new KubernetesCloudPool(new StandardApiServerClient(), executor);
    }

}
