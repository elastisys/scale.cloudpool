package com.elastisys.scale.cloudpool.multipool.server.lab;

import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotConfiguredException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.NotStartedException;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.multipool.api.MultiCloudPool;
import com.elastisys.scale.cloudpool.multipool.impl.CloudPoolFactory;
import com.elastisys.scale.cloudpool.multipool.impl.DiskBackedMultiCloudPool;
import com.elastisys.scale.cloudpool.multipool.server.MultiCloudPoolOptions;
import com.elastisys.scale.cloudpool.multipool.server.MultiCloudPoolServer;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

/**
 * Simple lab program for experimenting with a {@link MultiCloudPoolServer} that
 * only creates {@link DummyCloudPool} instances.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        MultiCloudPoolOptions options = new MultiCloudPoolOptions();
        options.httpPort = 8080;
        options.storageDir = "/tmp/multicloudpool";

        CloudPoolFactory factory = new MyFactory();

        MultiCloudPool multiCloudPool = new DiskBackedMultiCloudPool(new File(options.storageDir), factory);
        Server server = MultiCloudPoolServer.createServer(multiCloudPool, options);
        server.start();
    }

    private static class MyFactory implements CloudPoolFactory {

        @Override
        public CloudPool create(ThreadFactory threadFactory, File stateDir) throws CloudPoolException {
            return new DummyCloudPool(Executors.newScheduledThreadPool(5, threadFactory));
        }

    }

    private static class DummyCloudPool implements CloudPool {
        private static Logger LOG = LoggerFactory.getLogger(DummyCloudPool.class);

        private JsonObject config;

        private boolean started;

        private ScheduledExecutorService executor;

        private ScheduledFuture<?> task;

        public DummyCloudPool(ScheduledExecutorService executor) {
            this.executor = executor;
        }

        @Override
        public void configure(JsonObject configuration) throws IllegalArgumentException, CloudPoolException {
            LOG.debug("setting config {}", configuration);
            this.config = configuration;
        }

        @Override
        public Optional<JsonObject> getConfiguration() {
            return Optional.ofNullable(this.config);
        }

        @Override
        public void start() throws NotConfiguredException {
            LOG.debug("starting ...");
            this.started = true;
            this.task = this.executor.scheduleWithFixedDelay(() -> LOG.debug("running task ..."), 5, 5,
                    TimeUnit.SECONDS);
        }

        @Override
        public void stop() {
            LOG.debug("stopping ...");
            if (this.task != null) {
                this.task.cancel(true);
            }
            this.started = false;
        }

        @Override
        public CloudPoolStatus getStatus() {
            return new CloudPoolStatus(this.started, this.config != null);
        }

        @Override
        public MachinePool getMachinePool() throws CloudPoolException, NotStartedException {
            LOG.debug("get machine pool ...");
            return new MachinePool(Collections.emptyList(), UtcTime.now());
        }

        @Override
        public PoolSizeSummary getPoolSize() throws CloudPoolException, NotStartedException {
            LOG.debug("get pool size ...");
            return new PoolSizeSummary(0, 0, 0);
        }

        @Override
        public Future<?> setDesiredSize(int desiredSize)
                throws IllegalArgumentException, CloudPoolException, NotStartedException {
            LOG.debug("setting desired size: {}", desiredSize);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void terminateMachine(String machineId, boolean decrementDesiredSize)
                throws NotFoundException, CloudPoolException, NotStartedException {
            throw new CloudPoolException("failed to terminate machine " + machineId);
        }

        @Override
        public void setServiceState(String machineId, ServiceState serviceState)
                throws NotFoundException, CloudPoolException, NotStartedException {
            LOG.debug("set service state for {}", machineId);
        }

        @Override
        public void setMembershipStatus(String machineId, MembershipStatus membershipStatus)
                throws NotFoundException, CloudPoolException, NotStartedException {
            LOG.debug("set membership status for {}", machineId);
        }

        @Override
        public void attachMachine(String machineId) throws NotFoundException, CloudPoolException, NotStartedException {
            throw new NotStartedException("not yet started");
            // LOG.debug("attach machine {}", machineId);
        }

        @Override
        public void detachMachine(String machineId, boolean decrementDesiredSize)
                throws NotFoundException, CloudPoolException, NotStartedException {
            LOG.debug("detach machine {}", machineId);
        }

    }
}
