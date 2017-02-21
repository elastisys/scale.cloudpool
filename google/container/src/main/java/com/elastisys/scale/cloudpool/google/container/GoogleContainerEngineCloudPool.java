package com.elastisys.scale.cloudpool.google.container;

import static com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics.RESIZE;
import static com.elastisys.scale.commons.net.alerter.AlertSeverity.ERROR;
import static com.elastisys.scale.commons.net.alerter.AlertSeverity.INFO;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

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
import com.elastisys.scale.cloudpool.google.commons.api.compute.functions.InstanceToMachine;
import com.elastisys.scale.cloudpool.google.container.client.ClusterSnapshot;
import com.elastisys.scale.cloudpool.google.container.client.ContainerClusterClient;
import com.elastisys.scale.cloudpool.google.container.client.InstanceGroupSnapshot;
import com.elastisys.scale.cloudpool.google.container.client.NodePoolSnapshot;
import com.elastisys.scale.cloudpool.google.container.config.GoogleContainerEngineCloudPoolConfig;
import com.elastisys.scale.cloudpool.google.container.scalingstrategy.ResizePlan;
import com.elastisys.scale.cloudpool.google.container.scalingstrategy.ScalingStrategy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertBuilder;
import com.elastisys.scale.commons.net.alerter.multiplexing.MultiplexingAlerter;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.elastisys.scale.commons.net.url.UrlUtils;
import com.google.api.services.container.model.Cluster;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A {@link CloudPool} that manages the size of a Google Container Engine (GKE)
 * container cluster. The cloud pool modifies the size of the node pool(s) that
 * the cluster is made up of to ensure that the actual cluster size is kept in
 * sync with the desired size that the cloud pool has been instructed to
 * maintain.
 */
public class GoogleContainerEngineCloudPool implements CloudPool {

    private static final int THREAD_POOL_SIZE = 5;

    private static Logger LOG = LoggerFactory.getLogger(GoogleContainerEngineCloudPool.class);
    private final ScheduledExecutorService executor;
    private final EventBus eventBus;

    /** Client used to access the */
    private final ContainerClusterClient client;

    /** The currently set config. */
    private GoogleContainerEngineCloudPoolConfig config = null;
    /** Indicates if the cloudpool has been started. */
    private boolean started = false;

    /**
     * The currently set desiredSize. Its value is set on calls to
     * {@link #setDesiredSize(int)}. If not explicitly set, it will be
     * initialized when the first {@link ClusterSnapshot} is taken.
     */
    private Integer desiredSize;

    /**
     * Sends {@link Alert}s received on {@link EventBus} if prescribed by
     * {@link #config}.
     */
    private final MultiplexingAlerter alerter;

    /**
     * The most recent snapshot taken of the cluster. This snapshot is refreshed
     * periodically when the pool update task runs.
     */
    private ClusterSnapshot clusterSnapshot;

    /** Task that executes periodically to call {@link #updateCluster()}. */
    private ScheduledFuture<?> poolUpdateTask;

    /** Used to prevent concurrent {@link #configure(JsonObject)} calls. */
    private final Object configLock = new Object();
    /** Used to prevent concurrent {@link #updateCluster()} calls. */
    private final Object updateLock = new Object();

    public GoogleContainerEngineCloudPool(ContainerClusterClient apiClient, ScheduledExecutorService executor) {
        this(apiClient, null, executor);
    }

    public GoogleContainerEngineCloudPool(ContainerClusterClient gkeClient, EventBus eventBus,
            ScheduledExecutorService executor) {
        this.client = gkeClient;
        this.executor = executor;
        this.eventBus = Optional.fromNullable(eventBus).or(new AsyncEventBus(this.executor));

        this.alerter = new MultiplexingAlerter();
        this.eventBus.register(this.alerter);

        this.desiredSize = null;
    }

    @Override
    public void configure(JsonObject configuration) throws IllegalArgumentException, CloudPoolException {
        checkArgument(configuration != null, "a null configuration was received");

        GoogleContainerEngineCloudPoolConfig validatedConfig = parseAndValidate(configuration);

        synchronized (this.configLock) {
            boolean needsRestart = isStarted();
            if (needsRestart) {
                stop();
            }

            LOG.debug("applying new configuration ...");
            this.config = validatedConfig;
            this.client.configure(this.config.getCloudApiSettings());

            if (needsRestart) {
                start();
            }
        }
    }

    private GoogleContainerEngineCloudPoolConfig parseAndValidate(JsonObject configuration)
            throws IllegalArgumentException {
        GoogleContainerEngineCloudPoolConfig config = JsonUtils.toObject(configuration,
                GoogleContainerEngineCloudPoolConfig.class);
        config.validate();
        return config;
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

        if (isStarted()) {
            return;
        }

        // (re)start pool update task
        TimeInterval updateInterval = this.config.getPoolUpdateInterval();
        this.poolUpdateTask = this.executor.scheduleWithFixedDelay(() -> updateCluster(), updateInterval.getTime(),
                updateInterval.getTime(), updateInterval.getUnit());

        // activate alerter
        this.alerter.registerAlerters(this.config.getAlerts().orElse(null), standardAlertTags());

        this.started = true;
        LOG.info("started {}", getClass().getSimpleName());
    }

    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }
        // stop pool update task
        this.poolUpdateTask.cancel(false);
        this.poolUpdateTask = null;

        // passivate alerter
        this.alerter.unregisterAlerters();

        this.started = false;
        LOG.info("stopped {}", getClass().getSimpleName());
    }

    @Override
    public CloudPoolStatus getStatus() {
        return new CloudPoolStatus(isStarted(), isConfigured());
    }

    @Override
    public MachinePool getMachinePool() throws CloudPoolException, NotStartedException {
        ensureStarted();
        ClusterSnapshot clusterSnapshot = getClusterSnapshotOrUpdate();
        List<Machine> machines = clusterSnapshot.getStartedNodes().stream().map(new InstanceToMachine()::apply)
                .collect(Collectors.toList());
        return new MachinePool(machines, clusterSnapshot.getTimestamp());
    }

    @Override
    public PoolSizeSummary getPoolSize() throws CloudPoolException, NotStartedException {
        ensureStarted();
        ClusterSnapshot clusterSnapshot = getClusterSnapshotOrUpdate();
        return new PoolSizeSummary(this.desiredSize, clusterSnapshot.getTotalSize(), clusterSnapshot.getTotalSize());
    }

    @Override
    public void setDesiredSize(int desiredSize)
            throws IllegalArgumentException, CloudPoolException, NotStartedException {
        ensureStarted();
        this.desiredSize = desiredSize;
        // asynchronously run pool update
        this.executor.submit(() -> updateCluster());
    }

    @Override
    public void terminateMachine(String machineId, boolean decrementDesiredSize)
            throws NotFoundException, CloudPoolException, NotStartedException {
        notImplementedError("terminateMachine");
    }

    @Override
    public void setServiceState(String machineId, ServiceState serviceState)
            throws NotFoundException, CloudPoolException, NotStartedException {
        notImplementedError("setServiceState");
    }

    @Override
    public void setMembershipStatus(String machineId, MembershipStatus membershipStatus)
            throws NotFoundException, CloudPoolException, NotStartedException {
        notImplementedError("setMembershipStatus");
    }

    @Override
    public void attachMachine(String machineId) throws NotFoundException, CloudPoolException, NotStartedException {
        notImplementedError("attachMachine");
    }

    @Override
    public void detachMachine(String machineId, boolean decrementDesiredSize)
            throws NotFoundException, CloudPoolException, NotStartedException {
        notImplementedError("detachMachine");

    }

    private void notImplementedError(String operation) {
        throw new UnsupportedOperationException(operation + " is not implemented for " + getClass().getSimpleName());
    }

    private void ensureConfigured() throws IllegalStateException {
        checkState(isConfigured(), "attempt to use cloudpool before being configured");
    }

    private void ensureStarted() throws IllegalStateException {
        ensureConfigured();
        checkState(isStarted(), "attempt to use cloudpool before being started");
    }

    private boolean isConfigured() {
        return this.config != null;
    }

    private boolean isStarted() {
        return this.started;
    }

    Integer desiredSize() {
        return this.desiredSize;
    }

    GoogleContainerEngineCloudPoolConfig config() {
        return this.config;
    }

    /**
     * Creates metadata tags that are to be added to every {@link Alert} sent.
     * These are used by the receiver to distinguish the sending cloudpools from
     * other cloudpools she may be running.
     *
     * @return
     */
    Map<String, JsonElement> standardAlertTags() {
        Map<String, JsonElement> standardTags = Maps.newHashMap();
        standardTags.put("cloudPoolName", JsonUtils.toJson(this.config.getName()));
        standardTags.put("containerCluster", JsonUtils.toJson(this.config.getCluster()));
        return standardTags;
    }

    /**
     * Returns the latest {@link #clusterSnapshot} or, in case none exists,
     * refreshes the snapshot.
     *
     * @return
     */
    private ClusterSnapshot getClusterSnapshotOrUpdate() {
        if (this.clusterSnapshot == null) {
            refreshClusterSnapshot();
        }
        return this.clusterSnapshot;
    }

    /**
     * Refreshes the {@link #clusterSnapshot} by fetching it anew from the API.
     * If the {@link #desiredSize} of the pool has not been set yet, it is
     * initialized from the current size of the cluster. Note that this
     * operation may require a few seconds to complete.
     *
     * @return
     */
    ClusterSnapshot refreshClusterSnapshot() throws CloudPoolException {
        LOG.debug("refreshing cluster snapshot ...");

        Cluster clusterMetadata = this.client.getCluster(this.config.getCluster().getProject(),
                this.config.getCluster().getZone(), this.config.getCluster().getName());

        // make a few retries on failure to take snapshot. recently added
        // cluster nodes may take some time to appear in API (results in 404
        // responses)
        try {
            String taskName = "cluster-snapshot";
            int maxAttempts = 3;
            int delay = 5;
            ClusterSnapshotter task = new ClusterSnapshotter(this.client, clusterMetadata);
            this.clusterSnapshot = Retryers.fixedDelayRetryer(taskName, task, delay, SECONDS, maxAttempts).call();
        } catch (Exception e) {
            throw new CloudPoolException("failed to take cluster snapshot: " + e.getMessage(), e);
        }

        ensureDesiredSizeSet(this.clusterSnapshot);
        LOG.debug("cluster snapshot updated.");
        return this.clusterSnapshot;
    }

    /**
     * Initializes the {@link #desiredSize}, unless it has already been set, to
     * the total size of the given container cluster.
     *
     * @param cluster
     */
    private void ensureDesiredSizeSet(ClusterSnapshot cluster) {
        if (this.desiredSize == null) {
            this.desiredSize = cluster.getTotalSize();
            LOG.debug("desiredSize initialized to: {}", this.desiredSize);
        }
    }

    /**
     * Updates the container cluster according to the currently set
     * {@link #desiredSize} and the configured {@link ScalingStrategy}.
     */
    void updateCluster() {
        try {
            // prevent concurrent updates
            synchronized (this.updateLock) {
                LOG.debug("updating cluster size ...");

                ClusterSnapshot cluster = refreshClusterSnapshot();
                int targetSize = this.desiredSize;
                if (cluster.getTotalSize() == targetSize) {
                    LOG.debug("cluster already correctly sized ({}).", targetSize);
                    return;
                }
                ensureClusterUpdateable(cluster);

                doUpdate(cluster, targetSize);
                this.eventBus.post(AlertBuilder.create()
                        .topic(RESIZE.name()).severity(INFO).message(String
                                .format("container cluster size updated: %d -> %d", cluster.getTotalSize(), targetSize))
                        .build());
            }
        } catch (Exception e) {
            String message = String.format("failed to update cluster size: %s", e.getMessage());
            LOG.error(message, e);
            this.eventBus.post(AlertBuilder.create().topic(RESIZE.name()).severity(ERROR).message(message).build());
        }
    }

    /**
     * Ensures that a given container cluster can be updated, The criteria for
     * this being that it has at least one node pool with at least one instance
     * group.
     *
     * @param cluster
     * @throws IllegalStateException
     */
    private void ensureClusterUpdateable(ClusterSnapshot cluster) throws IllegalStateException {
        List<NodePoolSnapshot> nodePools = this.clusterSnapshot.getNodePools();
        checkState(nodePools != null && !nodePools.isEmpty(),
                "cannot resize container cluster %s: doesn't have any node pools", cluster.getMetadata().getName());
        for (NodePoolSnapshot nodePool : nodePools) {
            List<InstanceGroupSnapshot> instanceGroups = nodePool.getInstanceGroups();
            checkState(instanceGroups != null && !instanceGroups.isEmpty(),
                    "cannot resize container cluster %s: node pool %s doesn't have any instance group(s)",
                    cluster.getMetadata().getName(), nodePool.getMetadata().getName());
        }
    }

    /**
     * Updates the container cluster from its present size to the given target
     * size using the configured {@link ScalingStrategy}.
     *
     * @param cluster
     *            The present state of the container cluster.
     * @param targetSize
     *            The desired size of the cluster.
     */
    private void doUpdate(ClusterSnapshot cluster, int targetSize) {
        LOG.debug("about to update cluster: {} -> {}", cluster.getTotalSize(), targetSize);
        ResizePlan resizePlan = this.config.getScalingPolicy().getStrategy().planResize(targetSize, cluster);

        for (Entry<URL, Integer> instanceGroupTargetSize : resizePlan.getInstanceGroupTargetSizes().entrySet()) {
            URL instanceGroupUrl = instanceGroupTargetSize.getKey();
            Integer groupTargetSize = instanceGroupTargetSize.getValue();
            LOG.debug("{} -> {}", UrlUtils.basename(instanceGroupUrl.toString()), groupTargetSize);
            this.client.instanceGroup(instanceGroupUrl.toString()).resize(groupTargetSize);
        }
    }

}
