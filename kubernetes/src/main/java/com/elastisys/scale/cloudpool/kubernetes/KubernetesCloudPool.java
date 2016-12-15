package com.elastisys.scale.cloudpool.kubernetes;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.ApiVersion;
import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotConfiguredException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.NotStartedException;
import com.elastisys.scale.cloudpool.api.types.CloudPoolMetadata;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.api.types.CloudProviders;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.kubernetes.client.KubernetesClient;
import com.elastisys.scale.cloudpool.kubernetes.config.KubernetesCloudPoolConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.concurrent.RestartableScheduledExecutorService;
import com.elastisys.scale.commons.util.concurrent.StandardRestartableScheduledExecutorService;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonObject;

/**
 * A {@link CloudPool} that manages the size of Kubernetes replication
 * controller, which translates into controlling the number of "pods" of a given
 * kind.
 * <p/>
 * The {@link KubernetesCloudPool} operates in a fully synchronous manner,
 * meaning that it carries out each operation, when asked to, against the
 * backing Kubernetes apiserver in a blocking fashion.
 */
public class KubernetesCloudPool implements CloudPool {
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesCloudPool.class);
    private static final CloudPoolMetadata METADATA = new CloudPoolMetadata(CloudProviders.KUBERNETES,
            Arrays.asList(ApiVersion.LATEST));
    private static final int MAX_THREADS = 5;

    public static final String DESCRIPTION = "Kubernetes ReplicationController Cloudpool";

    /** The currently set configuration. */
    private KubernetesCloudPoolConfig config;

    /** <code>true</code> if cloudpool is in a started state. */
    private boolean started;

    /** Kubernetes apiserver client. */
    private KubernetesClient kubernetesClient;

    /**
     * The currently set desired size. <code>null</code> means that no initial
     * size has been set and no initial size could be determined at startup.
     */
    private Integer desiredSize = null;

    /** Periodical executor of {@link PoolUpdateTask}. */
    private final RestartableScheduledExecutorService executor;

    /**
     * Creates a new {@link KubernetesCloudPool}.
     */
    public KubernetesCloudPool(KubernetesClient apiClient) {
        this.config = null;
        this.started = false;
        this.kubernetesClient = apiClient;

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("pool-updater-%d").setDaemon(true)
                .build();
        this.executor = new StandardRestartableScheduledExecutorService(MAX_THREADS, threadFactory);
    }

    @Override
    public synchronized void configure(JsonObject configuration) throws IllegalArgumentException, CloudPoolException {
        KubernetesCloudPoolConfig newConfig = parseAndValidate(configuration);

        LOG.info("applying new configuration: {}", JsonUtils.toPrettyString(configuration));

        boolean mustRestart = isStarted();
        if (mustRestart) {
            stop();
        }

        // apply config
        this.config = newConfig;
        this.kubernetesClient.configure(newConfig);

        if (mustRestart) {
            start();
        }
    }

    private KubernetesCloudPoolConfig parseAndValidate(JsonObject configuration) {
        KubernetesCloudPoolConfig newConfig = null;
        try {
            newConfig = JsonUtils.toObject(configuration, KubernetesCloudPoolConfig.class);
            newConfig.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("invalid config: %s", e.getMessage()), e);
        }
        return newConfig;
    }

    @Override
    public Optional<JsonObject> getConfiguration() {
        if (this.config == null) {
            return Optional.absent();
        }

        return Optional.fromNullable(JsonUtils.toJson(this.config).getAsJsonObject());
    }

    @Override
    public void start() throws NotConfiguredException {
        ensureConfigured();
        LOG.info("starting {} ...", getClass().getSimpleName());

        tryToDetermineDesiredSize();

        this.executor.start();
        TimeInterval interval = this.config.getPoolUpdate().getUpdateInterval();
        this.executor.scheduleWithFixedDelay(new PoolUpdateTask(this.kubernetesClient, this.desiredSize),
                interval.getTime(), interval.getTime(), interval.getUnit());

        this.started = true;
        LOG.info("{} started.", getClass().getSimpleName());
    }

    /**
     * Makes an attempt to determine the initial desired size by looking at the
     * size of the ReplicationController.
     */
    private void tryToDetermineDesiredSize() {
        ensureConfigured();
        try {
            LOG.info("determining initial desired size ...");
            this.desiredSize = this.kubernetesClient.getPoolSize().getDesiredSize();
            LOG.info("initial desired size determined: {}", this.desiredSize);
        } catch (Exception e) {
            LOG.warn("failed to determine initial size: {}", e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        LOG.info("stopping {} ...", getClass().getSimpleName());
        try {
            this.executor.stop(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("interrupted while waiting for tasks to stop: {}", e.getMessage());
        }

        this.started = false;
        LOG.info("{} stopped.", getClass().getSimpleName());
    }

    @Override
    public CloudPoolStatus getStatus() {
        return new CloudPoolStatus(this.started, this.config != null);
    }

    @Override
    public MachinePool getMachinePool() throws CloudPoolException, NotStartedException {
        ensureStarted();
        return this.kubernetesClient.getMachinePool();
    }

    @Override
    public PoolSizeSummary getPoolSize() throws CloudPoolException, NotStartedException {
        ensureStarted();
        PoolSizeSummary poolSize = this.kubernetesClient.getPoolSize();
        return poolSize;
    }

    @Override
    public void setDesiredSize(int desiredSize)
            throws IllegalArgumentException, CloudPoolException, NotStartedException {
        ensureStarted();
        checkArgument(desiredSize >= 0, "desiredSize cannot be negative");
        this.desiredSize = desiredSize;
        this.kubernetesClient.setDesiredSize(desiredSize);
    }

    @Override
    public void terminateMachine(String machineId, boolean decrementDesiredSize)
            throws NotFoundException, CloudPoolException, NotStartedException {
        throwUnsupportedOperation();
    }

    @Override
    public void setServiceState(String machineId, ServiceState serviceState)
            throws NotFoundException, CloudPoolException, NotStartedException {
        throwUnsupportedOperation();
    }

    @Override
    public void setMembershipStatus(String machineId, MembershipStatus membershipStatus)
            throws NotFoundException, CloudPoolException, NotStartedException {
        throwUnsupportedOperation();
    }

    @Override
    public void attachMachine(String machineId) throws NotFoundException, CloudPoolException, NotStartedException {
        throwUnsupportedOperation();
    }

    @Override
    public void detachMachine(String machineId, boolean decrementDesiredSize)
            throws NotFoundException, CloudPoolException, NotStartedException {
        throwUnsupportedOperation();
    }

    @Override
    public CloudPoolMetadata getMetadata() {
        return METADATA;
    }

    /**
     * Returns the currently set configuration.
     *
     * @return
     */
    KubernetesCloudPoolConfig config() {
        return this.config;
    }

    /**
     * Thrown to indicate that a certain operation is not supported by this
     * {@link CloudPool}, which cannot implement the full API.
     */
    private void throwUnsupportedOperation() {
        throw new UnsupportedOperationException(
                String.format("this operation is not supported by the %s", DESCRIPTION));
    }

    /**
     * Throws an {@link IllegalStateException} if called before the cloudpool
     * has been configured.
     *
     * @throws IllegalStateException
     */
    private void ensureConfigured() throws IllegalStateException {
        if (this.config == null) {
            throw new IllegalStateException("cloudpool has not been configured");
        }
    }

    /**
     * Throws a {@link NotStartedException} if called before {@link #start()}
     * has been called on the {@link CloudPool}.
     *
     * @throws NotStartedException
     */
    private void ensureStarted() throws NotStartedException {
        if (!isStarted()) {
            throw new NotStartedException("cannot use cloudpool in stopped state");
        }
    }

    private boolean isStarted() {
        return this.started;
    }

    private static class PoolUpdateTask implements Runnable {
        private final KubernetesClient apiClient;
        private final Integer desiredSize;

        public PoolUpdateTask(KubernetesClient apiClient, Integer desiredSize) {
            this.apiClient = apiClient;
            this.desiredSize = desiredSize;
        }

        @Override
        public void run() {
            try {
                LOG.info("running pool update task ...");
                if (this.desiredSize == null) {
                    LOG.info("pool update task: no desiredSize set. aborting.");
                }
                LOG.info("setting desired pool size to {}", this.desiredSize);
                this.apiClient.setDesiredSize(this.desiredSize);
            } catch (Exception e) {
                LOG.error("failed to update pool size: {}", e.getMessage(), e);
            }

        }

    }

}
