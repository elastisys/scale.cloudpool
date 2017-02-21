package com.elastisys.scale.cloudpool.commons.basepool.poolupdater.impl;

import static com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics.RESIZE;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.max;
import static java.lang.String.format;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.FetchOption;
import com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.PoolFetcher;
import com.elastisys.scale.cloudpool.commons.basepool.poolupdater.PoolUpdater;
import com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlan;
import com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlanner;
import com.elastisys.scale.cloudpool.commons.termqueue.ScheduledTermination;
import com.elastisys.scale.cloudpool.commons.termqueue.TerminationQueue;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertBuilder;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.net.alerter.Alerter;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonElement;

public class StandardPoolUpdater implements PoolUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(StandardPoolUpdater.class);

    /** A cloud-specific management driver for the cloud pool. */
    private final CloudPoolDriver cloudDriver;

    /** A tracker of current pool members. */
    private final PoolFetcher poolFetcher;

    /**
     * {@link EventBus} used to post {@link Alert} events that are to be
     * forwarded by configured {@link Alerter}s (if any).
     */
    private final EventBus eventBus;

    private final BaseCloudPoolConfig config;

    /**
     * The desired size of the machine pool. Will be <code>null</code> until
     * set/determined.
     */
    private Integer desiredSize;
    /** Lock to prevent concurrent modification of {@link #desiredSize}. */
    private final Object desiredSizeLock = new Object();

    /**
     * The queue of already termination-marked instances (these will be used to
     * filter out instances already scheduled for termination from the candidate
     * set).
     */
    private final TerminationQueue terminationQueue;
    /** Lock to protect the machine pool from concurrent modifications. */
    private final Object poolUpdateLock = new Object();

    /** Task that periodically updates the size of the {@link MachinePool}. */
    private final ScheduledFuture<?> poolUpdateTask;

    public StandardPoolUpdater(CloudPoolDriver cloudDriver, PoolFetcher poolFetcher, ScheduledExecutorService executor,
            EventBus eventBus, BaseCloudPoolConfig config) {
        this.cloudDriver = cloudDriver;
        this.poolFetcher = poolFetcher;
        this.eventBus = eventBus;
        this.config = config;

        this.terminationQueue = new TerminationQueue();
        this.desiredSize = null;

        // start periodical cache update task
        TimeInterval updateInterval = config.getPoolUpdate().getUpdateInterval();
        this.poolUpdateTask = executor.scheduleWithFixedDelay(new PoolUpdateTask(this), updateInterval.getTime(),
                updateInterval.getTime(), updateInterval.getUnit());
        LOG.debug("started {}", getClass().getSimpleName());
    }

    @Override
    public void close() {
        // stop periodical execution of cache update task
        LOG.debug("shutting down {} ...", getClass().getSimpleName());
        if (this.poolUpdateTask != null) {
            this.poolUpdateTask.cancel(true);
        }
    }

    @Override
    public void setDesiredSize(int desiredSize) throws IllegalArgumentException, CloudPoolException {
        checkArgument(desiredSize >= 0, "negative desired pool size");

        // prevent concurrent pool modifications
        synchronized (this.desiredSizeLock) {
            LOG.info("set desiredSize to {}", desiredSize);
            this.desiredSize = desiredSize;
        }
    }

    @Override
    public int getDesiredSize() throws CloudPoolException {
        ensureDesiredSizeSet();
        return this.desiredSize;
    }

    @Override
    public void resize(BaseCloudPoolConfig config) throws CloudPoolException {
        try {
            updateMachinePool(config);
        } catch (Throwable e) {
            String message = format("failed to resize machine pool %s: %s", config.getName(), e.getMessage());
            Alert alert = AlertBuilder.create().topic(RESIZE.name()).severity(AlertSeverity.WARN).message(message)
                    .build();
            this.eventBus.post(alert);
            LOG.warn(message, e);
            throw new CloudPoolException(message, e);
        }
    }

    @Override
    public void terminateMachine(String machineId, boolean decrementDesiredSize)
            throws NotFoundException, CloudPoolException {
        ensurePoolReachable();
        ensureDesiredSizeSet();

        // prevent concurrent pool modifications
        synchronized (this.poolUpdateLock) {
            LOG.info("terminating {}", machineId);
            this.cloudDriver.terminateMachine(machineId);
            if (decrementDesiredSize) {
                synchronized (this.desiredSizeLock) {
                    // note: decrement unless desiredSize has been set to 0
                    // (without having been effectuated yet)
                    int newSize = max(this.desiredSize - 1, 0);
                    LOG.debug("decrementing desiredSize to {}", newSize);
                    setDesiredSize(newSize);
                }
            }
        }
        terminationAlert(machineId);
    }

    @Override
    public void setServiceState(String machineId, ServiceState serviceState)
            throws NotFoundException, CloudPoolException {
        LOG.info("service state {} assigned to {}", serviceState.name(), machineId);
        this.cloudDriver.setServiceState(machineId, serviceState);
        serviceStateAlert(machineId, serviceState);
    }

    @Override
    public void setMembershipStatus(String machineId, MembershipStatus membershipStatus)
            throws NotFoundException, CloudPoolException {
        LOG.info("membership status {} assigned to {}", membershipStatus, machineId);
        this.cloudDriver.setMembershipStatus(machineId, membershipStatus);
        membershipStatusAlert(machineId, membershipStatus);
    }

    @Override
    public void attachMachine(String machineId) throws NotFoundException, CloudPoolException {
        ensurePoolReachable();
        ensureDesiredSizeSet();

        // prevent concurrent pool modifications
        synchronized (this.poolUpdateLock) {
            LOG.info("attaching instance {} to pool", machineId);
            this.cloudDriver.attachMachine(machineId);
            synchronized (this.desiredSizeLock) {
                // implicitly increases pool size
                setDesiredSize(this.desiredSize + 1);
            }
        }
        attachAlert(machineId);
    }

    @Override
    public void detachMachine(String machineId, boolean decrementDesiredSize)
            throws NotFoundException, CloudPoolException {
        ensurePoolReachable();
        ensureDesiredSizeSet();

        // prevent concurrent pool modifications
        synchronized (this.poolUpdateLock) {
            LOG.info("detaching {} from pool", machineId);
            this.cloudDriver.detachMachine(machineId);
            if (decrementDesiredSize) {
                synchronized (this.desiredSizeLock) {
                    // note: decrement unless desiredSize has been set to 0
                    // (without having been effectuated yet)
                    int newSize = max(this.desiredSize - 1, 0);
                    LOG.debug("decrementing desiredSize to {}", newSize);
                    setDesiredSize(newSize);
                }
            }
        }
        detachAlert(machineId);
    }

    /**
     * Ensures that the cloud pool can be reached. If not, a
     * {@link CloudPoolException} is thrown.
     *
     * @throws CloudPoolException
     */
    private void ensurePoolReachable() throws CloudPoolException {
        try {
            this.poolFetcher.get();
        } catch (CloudPoolException e) {
            throw new CloudPoolException(
                    String.format("Cannot complete operation: cloud pool is unreachable: %s", e.getMessage()), e);
        }

    }

    /**
     * Ensures that the desired size has been set or determined. If not, an
     * attempt is made to determine the desired size from the current machine
     * pool.
     *
     * @return
     * @throws CloudPoolException
     *             If the desired size could not be determined.
     */
    private void ensureDesiredSizeSet() throws CloudPoolException {
        if (this.desiredSize != null) {
            return;
        }

        try {
            LOG.debug("determining initial desired pool size ...");
            MachinePool pool = this.poolFetcher.get();
            setDesiredSizeIfUnset(pool);
        } catch (Exception e) {
            throw new CloudPoolException(String.format(
                    "Cannot complete operation: " + "desired size could not be determined: %s", e.getMessage()), e);
        }
    }

    /**
     * Updates the size of the machine pool to match the currently set desired
     * size. This may involve terminating termination-due machines and placing
     * new server requests to replace terminated servers.
     * <p/>
     * Waits for the {@link #poolUpdateLock} to avoid concurrent pool updates.
     *
     * @param config
     *            Configuration that governs how to perform scaling actions.
     *
     * @throws CloudPoolException
     */
    void updateMachinePool(BaseCloudPoolConfig config) throws CloudPoolException {
        LOG.debug("updating machine pool ...");
        // we need to make use of fresh pool data since cached pool data could
        // make us start an excessive amount of machines (for example if the
        // pool fetcher haven't seen our latest started machines yet)
        MachinePool pool = this.poolFetcher.get(FetchOption.FORCE_REFRESH);
        // check if we need to determine desired size (it may not have been
        // possible on startup, e.g., due to cloud API being unreachable)
        setDesiredSizeIfUnset(pool);
        int targetSize = getDesiredSize();

        // prevent multiple threads from concurrently updating pool
        synchronized (this.poolUpdateLock) {
            doPoolUpdate(pool, config, targetSize);
        }
    }

    /**
     * Updates the machine pool to the given {@code targetSize}.
     *
     * @param pool
     *            An up-to-date {@link MachinePool} observation.
     * @param config
     *            Configuration that governs how to perform scaling actions.
     * @param targetSize
     *            The desired size of the pool.
     * @throws CloudPoolException
     */
    private void doPoolUpdate(MachinePool pool, BaseCloudPoolConfig config, int targetSize) throws CloudPoolException {
        LOG.info("updating pool size to desired size {}", targetSize);

        LOG.debug("current pool members: {}", Lists.transform(pool.getMachines(), Machine.toShortString()));
        this.terminationQueue.filter(pool.getActiveMachines());
        ResizePlanner resizePlanner = new ResizePlanner(pool, this.terminationQueue,
                config.getScaleInConfig().getVictimSelectionPolicy(),
                config.getScaleInConfig().getInstanceHourMargin());
        int netSize = resizePlanner.getNetSize();

        ResizePlan resizePlan = resizePlanner.calculateResizePlan(targetSize);
        if (resizePlan.hasScaleOutActions()) {
            scaleOut(resizePlan);
        }
        if (resizePlan.hasScaleInActions()) {
            List<ScheduledTermination> terminations = resizePlan.getToTerminate();
            LOG.info("scheduling {} machine(s) for termination", terminations.size());
            for (ScheduledTermination termination : terminations) {
                this.terminationQueue.add(termination);
                LOG.debug("scheduling machine {} for termination at {}", termination.getInstance().getId(),
                        termination.getTerminationTime());
            }
            LOG.debug("termination queue: {}", this.terminationQueue);
        }
        if (resizePlan.noChanges()) {
            LOG.info("pool is already properly sized ({})", netSize);
        }
        // effectuate scheduled terminations that are (over)due
        terminateOverdueMachines();
    }

    private List<Machine> scaleOut(ResizePlan resizePlan) throws StartMachinesException {
        LOG.info("sparing {} machine(s) from termination, " + "placing {} new request(s)", resizePlan.getToSpare(),
                resizePlan.getToRequest());
        this.terminationQueue.spare(resizePlan.getToSpare());

        try {
            List<Machine> startedMachines = this.cloudDriver.startMachines(resizePlan.getToRequest());
            startAlert(startedMachines);
            return startedMachines;
        } catch (StartMachinesException e) {
            // may have failed part-way through. notify of machines that were
            // started before error occurred.
            startAlert(e.getStartedMachines());
            throw e;
        }
    }

    private List<Machine> terminateOverdueMachines() {
        LOG.debug("checking termination queue for overdue machines: {}", this.terminationQueue);
        List<ScheduledTermination> overdueInstances = this.terminationQueue.popOverdueInstances();
        if (overdueInstances.isEmpty()) {
            return Collections.emptyList();
        }

        List<Machine> terminated = Lists.newArrayList();
        LOG.info("terminating {} overdue machine(s): {}", overdueInstances.size(), overdueInstances);
        for (ScheduledTermination overdueInstance : overdueInstances) {
            String victimId = overdueInstance.getInstance().getId();
            try {
                this.cloudDriver.terminateMachine(victimId);
                terminated.add(overdueInstance.getInstance());
            } catch (Exception e) {
                // only warn, since a failure to terminate an instance is not
                // necessarily an error condition, as the machine, e.g., may
                // have been terminated by external means since we last checked
                // the pool members
                String message = format("failed to terminate instance '%s': %s", victimId, e.getMessage());
                Alert alert = AlertBuilder.create().topic(RESIZE.name()).severity(AlertSeverity.WARN).message(message)
                        .build();
                this.eventBus.post(alert);
                LOG.warn(message, e);
            }
        }
        if (!terminated.isEmpty()) {
            terminationAlert(terminated);
        }

        return terminated;
    }

    /**
     * Post an {@link Alert} that new machines have been started in the pool.
     *
     * @param startedMachines
     *            The new machine instances that have been started.
     */
    void startAlert(List<Machine> startedMachines) {
        if (startedMachines.isEmpty()) {
            return;
        }

        String message = String.format("%d machine(s) were requested from cloud pool", startedMachines.size());
        LOG.info(message);
        Map<String, JsonElement> tags = Maps.newHashMap();
        List<String> startedMachineIds = Lists.transform(startedMachines, Machine.toId());
        tags.put("requestedMachines", JsonUtils.toJson(startedMachineIds));
        tags.put("poolMembers", poolMembersTag());
        this.eventBus
                .post(new Alert(AlertTopics.RESIZE.name(), AlertSeverity.INFO, UtcTime.now(), message, null, tags));
    }

    /**
     * In case no {@link #desiredSize} has been explicitly set (or previously
     * determined), this method determines the (initial) desired size from the
     * supplied {@link MachinePool}.
     *
     * @param pool
     *            An up-to-date {@link MachinePool} observation.
     */
    private void setDesiredSizeIfUnset(MachinePool pool) {
        if (this.desiredSize != null) {
            return;
        }

        LOG.debug("determining initial desired size from pool: {}", pool);
        // exclude inactive instances since they aren't actually part
        // of the desiredSize (they are to be replaced)
        int effectiveSize = pool.getActiveMachines().size();
        int allocated = pool.getAllocatedMachines().size();
        setDesiredSize(effectiveSize);
        LOG.info("initial desiredSize set to {} (allocated: {}, effective: {})", effectiveSize, allocated,
                effectiveSize);
    }

    /**
     * Post an {@link Alert} that a machine was terminated from the pool.
     *
     * @param machineId
     */
    void terminationAlert(String machineId) {
        Map<String, JsonElement> tags = Maps.newHashMap();
        List<String> machineIdList = Lists.newArrayList(machineId);
        tags.put("terminatedMachines", JsonUtils.toJson(machineIdList));
        tags.put("poolMembers", poolMembersTag());
        String message = String.format("Terminated machine %s.", machineId);
        this.eventBus
                .post(new Alert(AlertTopics.RESIZE.name(), AlertSeverity.INFO, UtcTime.now(), message, null, tags));
    }

    /**
     * Post an {@link Alert} that the members have been terminated from the
     * pool.
     *
     * @param terminatedMachines
     *            The machine instances that were terminated.
     */
    void terminationAlert(List<Machine> terminatedMachines) {
        String message = String.format("%d machine(s) were terminated in cloud pool", terminatedMachines.size());
        LOG.info(message);
        Map<String, JsonElement> tags = Maps.newHashMap();
        List<String> terminatedMachineIds = Lists.transform(terminatedMachines, Machine.toId());
        tags.put("terminatedMachines", JsonUtils.toJson(terminatedMachineIds));
        tags.put("poolMembers", poolMembersTag());
        this.eventBus
                .post(new Alert(AlertTopics.RESIZE.name(), AlertSeverity.INFO, UtcTime.now(), message, null, tags));
    }

    /**
     * Post an {@link Alert} that a machine was attached to the pool.
     *
     * @param machineId
     */
    void attachAlert(String machineId) {
        Map<String, JsonElement> tags = ImmutableMap.of("attachedMachines", JsonUtils.toJson(Arrays.asList(machineId)));
        String message = String.format("Attached machine %s to pool.", machineId);
        this.eventBus
                .post(new Alert(AlertTopics.RESIZE.name(), AlertSeverity.INFO, UtcTime.now(), message, null, tags));
    }

    /**
     * Post an {@link Alert} that a machine was detached from the pool.
     *
     * @param machineId
     */
    void detachAlert(String machineId) {
        Map<String, JsonElement> tags = ImmutableMap.of("detachedMachines", JsonUtils.toJson(Arrays.asList(machineId)));
        String message = String.format("Detached machine %s from pool.", machineId);
        this.eventBus
                .post(new Alert(AlertTopics.RESIZE.name(), AlertSeverity.INFO, UtcTime.now(), message, null, tags));
    }

    /**
     * Post an {@link Alert} that a pool member had its {@link ServiceState}
     * set.
     *
     * @param machineId
     * @param state
     */
    void serviceStateAlert(String machineId, ServiceState state) {
        Map<String, JsonElement> tags = ImmutableMap.of();
        String message = String.format("Service state set to %s for machine %s.", state.name(), machineId);
        this.eventBus.post(
                new Alert(AlertTopics.SERVICE_STATE.name(), AlertSeverity.DEBUG, UtcTime.now(), message, null, tags));
    }

    /**
     * Post an {@link Alert} that a pool member had its {@link MembershipStatus}
     * set.
     *
     * @param machineId
     * @param membershipStatus
     */
    void membershipStatusAlert(String machineId, MembershipStatus membershipStatus) {
        Map<String, JsonElement> tags = ImmutableMap.of();
        String message = String.format("Membership status set to %s for machine %s.", membershipStatus, machineId);
        this.eventBus.post(new Alert(AlertTopics.MEMBERSHIP_STATUS.name(), AlertSeverity.DEBUG, UtcTime.now(), message,
                null, tags));
    }

    private JsonElement poolMembersTag() {
        try {
            List<Machine> poolMembers = this.poolFetcher.get().getMachines();
            // exclude metadata field (noisy)
            List<Machine> shortFormatMembers = Lists.transform(poolMembers, Machine.toShortFormat());
            return JsonUtils.toJson(shortFormatMembers);
        } catch (Exception e) {
            LOG.warn("failed to retrieve pool members: {}", e.getMessage());
            return JsonUtils.toJson(String.format("N/A (call failed: %s)", e.getMessage()));
        }
    }

    /**
     * Returns the currently set configuration.
     *
     * @return
     */
    BaseCloudPoolConfig config() {
        return this.config;
    }

    /**
     * Task that, when executed, asks the {@link PoolUpdater} to resize the
     * pool.
     */
    private static class PoolUpdateTask implements Runnable {
        private final StandardPoolUpdater poolUpdater;

        public PoolUpdateTask(StandardPoolUpdater poolUpdater) {
            this.poolUpdater = poolUpdater;
        }

        @Override
        public void run() {
            try {
                this.poolUpdater.resize(this.poolUpdater.config());
            } catch (CloudPoolException e) {
                // just catch exception to prevent periodical execution from
                // aborting
            }
        }
    }

}
