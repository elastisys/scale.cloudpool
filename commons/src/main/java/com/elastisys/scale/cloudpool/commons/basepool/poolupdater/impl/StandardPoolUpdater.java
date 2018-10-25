package com.elastisys.scale.cloudpool.commons.basepool.poolupdater.impl;

import static com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics.RESIZE;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static java.lang.Math.max;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotEvictableException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.TerminateMachinesException;
import com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.FetchOption;
import com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.PoolFetcher;
import com.elastisys.scale.cloudpool.commons.basepool.poolupdater.PoolUpdater;
import com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlan;
import com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlanner;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertBuilder;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.net.alerter.Alerter;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.time.UtcTime;
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
            String message = format("failed to resize machine pool %s", config.getName());
            String details = format("%s: %s", message, e.getMessage());
            Alert alert = AlertBuilder.create().topic(RESIZE.name()).severity(AlertSeverity.WARN).message(message)
                    .details(details).build();
            this.eventBus.post(alert);
            LOG.warn(details, e);
            throw new CloudPoolException(details, e);
        }
    }

    @Override
    public void terminateMachine(String machineId, boolean decrementDesiredSize)
            throws NotFoundException, CloudPoolException {
        ensurePoolReachable();
        ensureDesiredSizeSet();
        Machine machine = ensurePoolMember(machineId);
        ensureEvictable(machine);

        // prevent concurrent pool modifications
        synchronized (this.poolUpdateLock) {
            // if decrementDesiredSize is true, the intent of this call is to
            // set the desiredSize to (preDesiredSize - 1) on completion.
            // however, setDesiredSize calls during the termination may cause
            // the desiredSize to change, so let's remember the original intent.
            int preDesiredSize;
            synchronized (this.desiredSizeLock) {
                preDesiredSize = getDesiredSize();
            }

            LOG.info("terminating {}", machineId);
            this.cloudDriver.terminateMachines(Arrays.asList(machineId));
            terminationAlert(machineId);

            if (!decrementDesiredSize) {
                return;
            }

            // at this point, the desiredSize may have been updated by
            // setDesiredSize calls made after this call. if so, those calls
            // should reflect the most recent intent of the client and we should
            // respect that. if the situation is unchanged compared to the start
            // of this call, we carry through the original intent and decrement.
            synchronized (this.desiredSizeLock) {
                int postDesiredSize = getDesiredSize();
                if (postDesiredSize != preDesiredSize) {
                    LOG.debug("desiredSize changed during operation (was: {}, is: {}). skipping decrement.",
                            preDesiredSize, postDesiredSize);
                    return;
                }
                int newSize = max(preDesiredSize - 1, 0);
                LOG.debug("decrementing desiredSize to {}", newSize);
                setDesiredSize(newSize);
            }
        }
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
        Machine machine = ensurePoolMember(machineId);
        ensureEvictable(machine);

        // prevent concurrent pool modifications
        synchronized (this.poolUpdateLock) {
            // if decrementDesiredSize is true, the intent of this call is to
            // set the desiredSize to (preDesiredSize - 1) on completion.
            // however, setDesiredSize calls during the termination may cause
            // the desiredSize to change, so let's remember the original intent.
            int preDesiredSize;
            synchronized (this.desiredSizeLock) {
                preDesiredSize = getDesiredSize();
            }

            LOG.info("detaching {} from pool", machineId);
            this.cloudDriver.detachMachine(machineId);
            detachAlert(machineId);

            if (!decrementDesiredSize) {
                return;
            }

            // at this point, the desiredSize may have been updated by
            // setDesiredSize calls made after this call. if so, those calls
            // should reflect the most recent intent of the client and we should
            // respect that. if the situation is unchanged compared to the start
            // of this call, we carry through the original intent and decrement.
            synchronized (this.desiredSizeLock) {
                int postDesiredSize = getDesiredSize();
                if (postDesiredSize != preDesiredSize) {
                    LOG.debug("desiredSize changed during operation (was: {}, is: {}). skipping decrement.",
                            preDesiredSize, postDesiredSize);
                    return;
                }
                int newSize = max(preDesiredSize - 1, 0);
                LOG.debug("decrementing desiredSize to {}", newSize);
                setDesiredSize(newSize);
            }
        }
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
     * Ensures that a given machine is recognized as a pool member. It it is a
     * pool member, the {@link Machine} is returned. If it is not a pool member,
     * a {@link NotFoundException} is thrown.
     *
     * @param machineId
     *            A machine id.
     * @return The {@link Machine} metadata.
     * @throws NotFoundException
     */
    private Machine ensurePoolMember(final String machineId) throws NotFoundException {
        MachinePool pool = this.poolFetcher.get();
        Optional<Machine> match = pool.getAllocatedMachines().stream().filter(m -> m.getId().equals(machineId))
                .findFirst();
        if (!match.isPresent()) {
            throw new NotFoundException(String.format("machine %s is not a pool member", machineId));
        }
        return match.get();
    }

    /**
     * Ensures that the given machine is evictable (according to its
     * {@link MembershipStatus}). If not, a {@link NotEvictableException} is
     * thrown.
     *
     * @param machineId
     * @throws NotEvictableException
     */
    private void ensureEvictable(Machine machine) throws NotEvictableException {
        if (machine.getMembershipStatus() != null && !machine.getMembershipStatus().isEvictable()) {
            throw new NotEvictableException(
                    String.format("machine %s cannot be evicted: prevented by its membership status", machine.getId()));
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

        // prevent multiple threads from concurrently updating pool
        synchronized (this.poolUpdateLock) {
            // it is possible that while waiting for the lock, a different pool
            // update operation changed the pool members and modified the
            // desired size (for example, a terminateMachine call). we should
            // therefore get a fresh pool snapshot and read the present
            // desiredSize value
            MachinePool pool = this.poolFetcher.get(FetchOption.FORCE_REFRESH);
            // check if we need to determine desired size (it may not have been
            // possible on startup, e.g., due to cloud API being unreachable)
            int targetSize = 0;
            synchronized (this.desiredSizeLock) {
                setDesiredSizeIfUnset(pool);
                targetSize = getDesiredSize();
            }

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

        LOG.debug("current pool members: {}",
                pool.getMachines().stream().map(Machine.toShortString()).collect(Collectors.toList()));
        ResizePlanner resizePlanner = new ResizePlanner(pool, config.getScaleInConfig().getVictimSelectionPolicy());
        int activeSize = resizePlanner.getActiveSize();

        ResizePlan resizePlan = resizePlanner.calculateResizePlan(targetSize);
        if (resizePlan.hasScaleOutActions()) {
            scaleOut(resizePlan);
        }
        if (resizePlan.hasScaleInActions()) {
            terminateMachines(resizePlan.getToTerminate());
        }
        if (resizePlan.noChanges()) {
            LOG.info("pool is already properly sized ({})", activeSize);
        }
    }

    private List<Machine> scaleOut(ResizePlan resizePlan) throws StartMachinesException {
        LOG.info("placing {} new machine requests", resizePlan.getToRequest());

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

    /**
     * Attempts to terminate the given collection of victim {@link Machine}s.
     * The collection of {@link Machine}s that were successfully terminated are
     * returned. Any failed terminations results in an {@link Alert} being
     * posted on the {@link EventBus}.
     *
     * @param victims
     * @return
     */
    private void terminateMachines(List<Machine> victims) {
        if (victims.isEmpty()) {
            return;
        }

        List<String> victimIds = victims.stream().map(Machine::getId).collect(Collectors.toList());
        LOG.info("terminating {} machine(s): {}", victimIds.size(), victimIds);
        List<String> terminatedIds = new ArrayList<>();
        try {
            this.cloudDriver.terminateMachines(victimIds);
            terminatedIds.addAll(victimIds);
        } catch (TerminateMachinesException e) {
            terminatedIds.addAll(e.getTerminatedMachines());

            LOG.error(e.getMessage());
            int numFailed = e.getTerminationErrors().size();
            String message = format("%d out of %d machine terminations failed", numFailed, victims.size());
            Alert alert = AlertBuilder.create().topic(RESIZE.name()).severity(AlertSeverity.WARN).message(message) //
                    .details(e.getMessage()).addMetadata("terminated", e.getTerminatedMachines()) //
                    .addMetadata("terminationErrors", JsonUtils.toJson(e.getTerminationErrorMessages())).build();
            this.eventBus.post(alert);
        } catch (Exception e) {
            String message = format("failed to terminate machines %s", victimIds);
            String detail = format("%s: %s", message, e.getMessage());
            LOG.error(detail, e);
            Alert alert = AlertBuilder.create().topic(RESIZE.name()).severity(AlertSeverity.ERROR).message(message)
                    .details(detail).build();
            this.eventBus.post(alert);
        }

        if (!terminatedIds.isEmpty()) {
            terminationAlert(terminatedIds);
        }
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
        Map<String, JsonElement> tags = new HashMap<>();
        List<String> startedMachineIds = startedMachines.stream().map(Machine::getId).collect(Collectors.toList());
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
        Map<String, JsonElement> tags = new HashMap<>();
        List<String> machineIdList = Arrays.asList(machineId);
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
    void terminationAlert(List<String> terminatedMachineIds) {
        String message = String.format("%d machine(s) were terminated in cloud pool: %s", terminatedMachineIds.size(),
                terminatedMachineIds);
        LOG.info(message);
        Map<String, JsonElement> tags = new HashMap<>();
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
        Map<String, JsonElement> tags = Maps.of("attachedMachines", JsonUtils.toJson(Arrays.asList(machineId)));
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
        Map<String, JsonElement> tags = Maps.of("detachedMachines", JsonUtils.toJson(Arrays.asList(machineId)));
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
        Map<String, JsonElement> tags = Maps.of();
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
        Map<String, JsonElement> tags = Maps.of();
        String message = String.format("Membership status set to %s for machine %s.", membershipStatus, machineId);
        this.eventBus.post(new Alert(AlertTopics.MEMBERSHIP_STATUS.name(), AlertSeverity.DEBUG, UtcTime.now(), message,
                null, tags));
    }

    private JsonElement poolMembersTag() {
        try {
            List<Machine> poolMembers = this.poolFetcher.get().getMachines();
            // exclude metadata field (noisy)
            List<Machine> shortFormatMembers = poolMembers.stream().map(Machine.toShortFormat())
                    .collect(Collectors.toList());
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
