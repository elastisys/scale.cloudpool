package com.elastisys.scale.cloudpool.kubernetes;

import static com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics.POOL_FETCH;
import static com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics.RESIZE;
import static com.elastisys.scale.commons.net.alerter.AlertSeverity.INFO;
import static com.elastisys.scale.commons.net.alerter.AlertSeverity.WARN;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotConfiguredException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.NotStartedException;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.KubernetesApiException;
import com.elastisys.scale.cloudpool.kubernetes.config.KubernetesCloudPoolConfig;
import com.elastisys.scale.cloudpool.kubernetes.config.PodPoolConfig;
import com.elastisys.scale.cloudpool.kubernetes.functions.PodToMachine;
import com.elastisys.scale.cloudpool.kubernetes.podpool.PodPool;
import com.elastisys.scale.cloudpool.kubernetes.podpool.PodPoolSize;
import com.elastisys.scale.cloudpool.kubernetes.podpool.impl.DeploymentPodPool;
import com.elastisys.scale.cloudpool.kubernetes.podpool.impl.ReplicaSetPodPool;
import com.elastisys.scale.cloudpool.kubernetes.podpool.impl.ReplicationControllerPodPool;
import com.elastisys.scale.cloudpool.kubernetes.types.Pod;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertBuilder;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.net.alerter.Alerter;
import com.elastisys.scale.commons.net.alerter.multiplexing.MultiplexingAlerter;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A {@link CloudPool} that manages the size of a group of Kubernetes pod
 * replicas, either via a ReplicationController, or via a ReplicaSet, or via a
 * Deployment.
 * <p/>
 * The {@link KubernetesCloudPool} operates in a fully synchronous manner,
 * meaning that it carries out each operation, when asked to, against the
 * backing Kubernetes API server in a blocking fashion.
 */
public class KubernetesCloudPool implements CloudPool {
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesCloudPool.class);

    /** The currently set configuration. */
    private KubernetesCloudPoolConfig config;

    /** <code>true</code> if cloudpool is in a started state. */
    private boolean started;

    /**
     * A client that can be configured to execute (authenticated) HTTP requests
     * against the REST API of a certain Kubernetes API server.
     */
    private final ApiServerClient apiServerClient;

    /**
     * The currently configured {@link PodPool} through which we manage the
     * group of pods.
     */
    private PodPool podPool;

    /**
     * The currently set desired size. <code>null</code> means that no initial
     * size has been set and no initial size could be determined at startup.
     */
    private Integer desiredSize = null;

    /** Periodical executor of {@link PoolUpdateTask}. */
    private final ScheduledExecutorService executor;
    /** Used to push {@link Alert}s. */
    private final EventBus eventBus;
    /**
     * Dispatches {@link Alert}s sent on the {@link EventBus} to configured
     * {@link Alerter}s.
     */
    private final MultiplexingAlerter alerter;
    /** Periodically executed pool update task. */
    private ScheduledFuture<?> poolUpdateTask;

    /** Lock to prevent concurrent access to critical sections. */
    private final Object lock = new Object();

    /**
     * Creates a {@link KubernetesCloudPool}.
     *
     * @param apiServerClient
     *            A client that can be configured to execute (authenticated)
     *            HTTP requests against the REST API of a certain Kubernetes API
     *            server.
     * @param executor
     *            Executor used to schedule periodical tasks.
     */
    public KubernetesCloudPool(ApiServerClient apiServerClient, ScheduledExecutorService executor) {
        this(apiServerClient, executor, null);
    }

    /**
     * Creates a {@link KubernetesCloudPool}.
     *
     * @param apiServerClient
     *            A client that can be configured to execute (authenticated)
     *            HTTP requests against the REST API of a certain Kubernetes API
     *            server.
     * @param executor
     *            Executor used to schedule periodical tasks.
     * @param eventBus
     *            {@link EventBus} used to push {@link Alert}s. May be
     *            <code>null</code>, in which case a default one is created.
     */
    public KubernetesCloudPool(ApiServerClient apiServerClient, ScheduledExecutorService executor, EventBus eventBus) {
        checkArgument(apiServerClient != null, "apiServerClient cannot be null");
        checkArgument(executor != null, "executor cannot be null");

        this.apiServerClient = apiServerClient;
        this.executor = executor;
        this.eventBus = eventBus != null ? eventBus : new AsyncEventBus(executor);

        this.alerter = new MultiplexingAlerter();
        this.eventBus.register(this.alerter);

        this.podPool = null;
        this.config = null;
        this.started = false;
    }

    @Override
    public void configure(JsonObject configuration) throws IllegalArgumentException, CloudPoolException {
        KubernetesCloudPoolConfig newConfig = parseAndValidate(configuration);

        LOG.info("applying new configuration ...");
        synchronized (this.lock) {
            apply(newConfig);
        }
    }

    private void apply(KubernetesCloudPoolConfig newConfig) {
        boolean mustRestart = isStarted();
        if (mustRestart) {
            stop();
        }

        KubernetesCloudPoolConfig oldConfig = this.config;
        this.config = newConfig;
        this.apiServerClient.configure(newConfig.getApiServerUrl(), newConfig.getAuth());
        // if podPool is different, instantiate a new podPool
        if (oldConfig == null || podPoolChanged(oldConfig, newConfig)) {
            this.podPool = newPodPool(newConfig.getPodPool());
        }
        // alerters may have changed
        this.alerter.unregisterAlerters();
        this.alerter.registerAlerters(config().getAlerts(), standardAlertMetadata());

        if (mustRestart) {
            start();
        }
    }

    /**
     * Returns <code>true</code> if the {@link PodPoolConfig} settings have
     * changed from one config to another.
     *
     * @param config1
     * @param config2
     * @return
     */
    private boolean podPoolChanged(KubernetesCloudPoolConfig config1, KubernetesCloudPoolConfig config2) {
        return !config1.getPodPool().equals(config2.getPodPool());
    }

    /**
     * Instantiates a {@link PodPool} of the kind given by a
     * {@link PodPoolConfig} (either a replication controller, or a replica set,
     * or a deployment).
     *
     * @param config
     * @return
     */
    private PodPool newPodPool(PodPoolConfig config) {
        String namespace = config.getNamespace();

        if (config.getReplicationController() != null) {
            return new ReplicationControllerPodPool().configure(this.apiServerClient, namespace,
                    config.getReplicationController());
        } else if (config.getReplicaSet() != null) {
            return new ReplicaSetPodPool().configure(this.apiServerClient, namespace, config.getReplicaSet());
        } else if (config.getDeployment() != null) {
            return new DeploymentPodPool().configure(this.apiServerClient, namespace, config.getDeployment());
        } else {
            throw new IllegalStateException("configuration does not specify a replica construct to manage "
                    + "[replicationController, replicaSet, deployment]");
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
            return Optional.empty();
        }

        return Optional.ofNullable(JsonUtils.toJson(this.config).getAsJsonObject());
    }

    @Override
    public void start() throws NotConfiguredException {
        ensureConfigured();

        synchronized (this.lock) {

            if (this.started) {
                return;
            }

            LOG.info("starting {} ...", getClass().getSimpleName());

            TimeInterval interval = this.config.getUpdateInterval();
            this.poolUpdateTask = this.executor.scheduleWithFixedDelay(new PoolUpdateTask(this), interval.getTime(),
                    interval.getTime(), interval.getUnit());

            this.started = true;
        }
        LOG.info("{} started.", getClass().getSimpleName());
    }

    @Override
    public void stop() {
        synchronized (this.lock) {
            if (!this.started) {
                return;
            }

            LOG.info("stopping {} ...", getClass().getSimpleName());
            if (this.poolUpdateTask != null) {
                this.poolUpdateTask.cancel(true);
            }

            this.started = false;
        }

        LOG.info("{} stopped.", getClass().getSimpleName());
    }

    @Override
    public CloudPoolStatus getStatus() {
        return new CloudPoolStatus(this.started, this.config != null);
    }

    @Override
    public MachinePool getMachinePool() throws CloudPoolException, NotStartedException {
        ensureStarted();
        DateTime now = UtcTime.now();
        try {
            List<Pod> pods = this.podPool.getPods();
            List<Machine> machines = pods.stream().map(new PodToMachine()).collect(Collectors.toList());
            return new MachinePool(machines, now);
        } catch (Exception e) {
            String message = "failed to get pod pool: " + e.getMessage();
            postAlert(POOL_FETCH, WARN, message);
            throw new CloudPoolException(message, e);
        }
    }

    @Override
    public PoolSizeSummary getPoolSize() throws CloudPoolException, NotStartedException {
        ensureStarted();
        try {
            PodPoolSize podPoolSize = this.podPool.getSize();
            if (this.desiredSize == null) {
                // initialize if not set yet
                this.desiredSize = podPoolSize.getDesiredReplicas();
            }
            return new PoolSizeSummary(this.desiredSize, podPoolSize.getActualReplicas(),
                    podPoolSize.getActualReplicas());
        } catch (Exception e) {
            String message = "failed to get pool size: " + e.getMessage();
            postAlert(POOL_FETCH, WARN, message);
            throw new CloudPoolException(message, e);
        }
    }

    @Override
    public void setDesiredSize(int desiredSize)
            throws IllegalArgumentException, CloudPoolException, NotStartedException {
        ensureStarted();
        checkArgument(desiredSize >= 0, "desiredSize cannot be negative");

        try {
            Integer priorSize = this.desiredSize;
            this.desiredSize = desiredSize;
            this.podPool.setDesiredSize(desiredSize);

            if (priorSize == null || priorSize != desiredSize) {
                // size change alert
                String message = String.format("desired size changed from %s to %d",
                        priorSize != null ? priorSize : "unset", desiredSize);
                postAlert(RESIZE, INFO, message);
            }
        } catch (Exception e) {
            String message = "failed to set desired size: " + e.getMessage();
            postAlert(RESIZE, WARN, message);
            throw new CloudPoolException(message, e);
        }
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

    /**
     * Returns the currently set configuration.
     *
     * @return
     */
    KubernetesCloudPoolConfig config() {
        return this.config;
    }

    /**
     * Returns the currently set {@link PodPool}.
     *
     * @return
     */
    PodPool podPool() {
        return this.podPool;
    }

    /**
     * Thrown to indicate that a certain operation is not supported by this
     * {@link CloudPool}, which cannot implement the full API.
     */
    private void throwUnsupportedOperation() {
        throw new UnsupportedOperationException(
                String.format("this operation is not supported by the %s", getClass().getSimpleName()));
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

    /**
     * Standard tags that are to be included in all sent out {@link Alert}s (in
     * addition to those already set on the {@link Alert} itself).
     *
     * @return
     */
    private Map<String, JsonElement> standardAlertMetadata() {
        Map<String, JsonElement> standardTags = new HashMap<>();
        standardTags.put("apiServer", JsonUtils.toJson(config().getApiServerUrl()));
        standardTags.put("namespace", JsonUtils.toJson(config().getPodPool().getNamespace()));
        standardTags.put("apiObject", JsonUtils.toJson(config().getPodPool().getApiObject()));
        return standardTags;
    }

    /**
     * Posts an {@link Alert} on the {@link EventBus} for dispatch to any
     * configured alert recipients.
     */
    private void postAlert(AlertTopics topic, AlertSeverity severity, String message) {
        this.eventBus.post(AlertBuilder.create().topic(topic.name()).severity(severity).message(message)
                .addMetadata(standardAlertMetadata()).build());
    }

    /**
     * Makes an attempt to determine the initial desired size by looking at the
     * size of the {@link PodPool}.
     */
    private void determineDesiredSize() {
        ensureConfigured();
        try {
            int desiredReplicas = this.podPool.getSize().getDesiredReplicas();
            this.desiredSize = desiredReplicas;
            LOG.info("initial desired size determined: {}", desiredReplicas);
        } catch (Exception e) {
            throw new KubernetesApiException("failed to determine pod pool size: " + e.getMessage(), e);
        }
    }

    private void updateDesiredSize() {
        if (this.desiredSize == null) {
            LOG.info("pool update: no desiredSize set. trying to initialize ...");
            determineDesiredSize();
        }
        LOG.info("pool update: setting desired pool size to {}", this.desiredSize);
        this.podPool.setDesiredSize(this.desiredSize);
    }

    /**
     * Periodical task that, when called, ensures that the {@link PodPool} is
     * instructed to request the desired number of pod replicas.
     */
    private static class PoolUpdateTask implements Runnable {
        private final KubernetesCloudPool cloudPool;

        public PoolUpdateTask(KubernetesCloudPool cloudPool) {
            this.cloudPool = cloudPool;
        }

        @Override
        public void run() {
            try {
                this.cloudPool.updateDesiredSize();
            } catch (Exception e) {
                LOG.error("failed to update pool size: {}", e.getMessage(), e);
            }
        }
    }

}
