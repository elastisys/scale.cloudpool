package com.elastisys.scale.cloudpool.multipool.impl;

import static com.elastisys.scale.commons.json.JsonUtils.parseJsonFile;
import static com.elastisys.scale.commons.json.JsonUtils.toObject;
import static com.elastisys.scale.commons.json.JsonUtils.toPrettyString;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.Future;

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
import com.elastisys.scale.cloudpool.multipool.api.CloudPoolInstance;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * A {@link CloudPoolInstance} whose state is saved to disk whenever the
 * configuration/start state changes.
 */
class DiskBackedCloudPoolInstance implements CloudPoolInstance {
    private static final Logger LOG = LoggerFactory.getLogger(DiskBackedCloudPoolInstance.class);

    /**
     * File name under {@link #stateDir} where {@link CloudPool}'s config (if
     * any) is saved.
     */
    public static final String CONFIG_FILE = "config.json";
    /**
     * File name under {@link #stateDir} where {@link CloudPool}'s status is
     * saved.
     */
    public static final String STATUS_FILE = "status.json";

    /** The {@link CloudPool} instance. */
    private final CloudPool cloudPool;
    /** Directory where the {@link CloudPool} state will be saved. */
    private final File stateDir;

    /**
     * Creates a {@link DiskBackedCloudPoolInstance} from a given
     * {@link CloudPool} and state storage directory. If {@code stateDir} does
     * not exist, it will be created.
     *
     * @param cloudPool
     *            The {@link CloudPool} instance.
     * @param stateDir
     *            Directory where the {@link CloudPool} state will be saved.
     *            Will be created if it does not exist.
     * @throws IOException
     *             if the {@code stateDir} could not be created.
     */
    public DiskBackedCloudPoolInstance(CloudPool cloudPool, File stateDir) throws IOException {
        checkArgument(cloudPool != null, "cloudPool cannot be null");
        checkArgument(stateDir != null, "stateDir cannot be null");
        this.cloudPool = cloudPool;
        this.stateDir = stateDir;
        Files.createDirectories(stateDir.toPath());
    }

    @Override
    public String name() {
        return this.stateDir.getName();
    }

    private void save() throws CloudPoolSaveException {
        try {
            Optional<JsonObject> config = this.cloudPool.getConfiguration();
            if (config != null && config.isPresent()) {
                String configAsJson = toPrettyString(config.get());
                Files.write(configFile().toPath(), configAsJson.getBytes());
            }

            CloudPoolStatus status = this.cloudPool.getStatus();
            if (status != null) {
                String statusAsJson = JsonUtils.toPrettyString(JsonUtils.toJson(status));
                Files.write(statusFile().toPath(), statusAsJson.getBytes());
            }
        } catch (Exception e) {
            throw new CloudPoolSaveException(String.format("failed to save cloudpool %s: %s", name(), e.getMessage()),
                    e);
        }
    }

    /**
     * Restores this instance by re-setting the configuration and status found
     * on disk. If the instance has never been saved this is a no-op.
     *
     * @throws CloudPoolRestoreException
     */
    public void restore() throws CloudPoolRestoreException {
        try {
            LOG.debug("restoring cloudpool {} ...", name());

            if (configFile().isFile()) {
                LOG.debug("restoring config for cloudpool {} ...", name());
                JsonObject config = parseJsonFile(configFile()).getAsJsonObject();
                this.cloudPool.configure(config);
            }

            if (statusFile().isFile()) {
                CloudPoolStatus status = toObject(parseJsonFile(statusFile()), CloudPoolStatus.class);
                if (status.isStarted()) {
                    LOG.debug("starting cloudpool {} ...", name());
                    this.cloudPool.start();
                } else {
                    this.cloudPool.stop();
                }
            }
        } catch (Exception e) {
            throw new CloudPoolRestoreException(
                    String.format("failed to restore cloudpool %s: %s", name(), e.getMessage()), e);
        }
    }

    @Override
    public void configure(JsonObject configuration) throws IllegalArgumentException, CloudPoolException {
        this.cloudPool.configure(configuration);
        save();
    }

    @Override
    public Optional<JsonObject> getConfiguration() {
        return this.cloudPool.getConfiguration();
    }

    @Override
    public void start() throws NotConfiguredException {
        this.cloudPool.start();
        save();
    }

    @Override
    public void stop() {
        this.cloudPool.stop();
        save();
    }

    @Override
    public CloudPoolStatus getStatus() {
        return this.cloudPool.getStatus();
    }

    @Override
    public MachinePool getMachinePool() throws CloudPoolException, NotStartedException {
        return this.cloudPool.getMachinePool();
    }

    @Override
    public PoolSizeSummary getPoolSize() throws CloudPoolException, NotStartedException {
        return this.cloudPool.getPoolSize();
    }

    @Override
    public Future<?> setDesiredSize(int desiredSize)
            throws IllegalArgumentException, CloudPoolException, NotStartedException {
        return this.cloudPool.setDesiredSize(desiredSize);
    }

    @Override
    public void terminateMachine(String machineId, boolean decrementDesiredSize)
            throws NotFoundException, CloudPoolException, NotStartedException {
        this.cloudPool.terminateMachine(machineId, decrementDesiredSize);
    }

    @Override
    public void setServiceState(String machineId, ServiceState serviceState)
            throws NotFoundException, CloudPoolException, NotStartedException {
        this.cloudPool.setServiceState(machineId, serviceState);
    }

    @Override
    public void setMembershipStatus(String machineId, MembershipStatus membershipStatus)
            throws NotFoundException, CloudPoolException, NotStartedException {
        this.cloudPool.setMembershipStatus(machineId, membershipStatus);
    }

    @Override
    public void attachMachine(String machineId) throws NotFoundException, CloudPoolException, NotStartedException {
        this.cloudPool.attachMachine(machineId);
    }

    @Override
    public void detachMachine(String machineId, boolean decrementDesiredSize)
            throws NotFoundException, CloudPoolException, NotStartedException {
        this.cloudPool.detachMachine(machineId, decrementDesiredSize);
    }

    private File configFile() {
        return new File(this.stateDir, CONFIG_FILE);
    }

    private File statusFile() {
        return new File(this.stateDir, STATUS_FILE);
    }
}