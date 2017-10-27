package com.elastisys.scale.cloudpool.api.types;

import static com.elastisys.scale.cloudpool.api.types.Machine.toShortString;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.joda.time.DateTime;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

/**
 * Represents a snapshot of the machine pool managed by a {@link CloudPool}.
 *
 * @see CloudPool
 */
public class MachinePool {

    /**
     * The machine instances that were part of the machine pool at the time of
     * the snapshot.
     */
    private final List<Machine> machines;
    /**
     * The time at which the pool observation was made. Note that in case the
     * cloud pool serves locally cached data, this field may be used by the
     * client to determine if the data is fresh enough to be acted upon.
     */
    private final DateTime timestamp;

    /**
     * Constructs a new {@link MachinePool} snapshot.
     *
     * @param machines
     *            The machine instances that were part of the machine pool at
     *            the time of the snapshot.
     * @param timestamp
     *            The time when this snapshot of the resource pool was taken.
     */
    public MachinePool(List<? extends Machine> machines, DateTime timestamp) {
        checkNotNull(machines, "machines cannot be null");
        checkNotNull(timestamp, "timestamp cannot be null");
        this.machines = Lists.newArrayList(machines);
        this.timestamp = timestamp;
    }

    /**
     * Returns all {@link Machine}s in the pool.
     * <p/>
     * Note: the returned {@link Machine}s may be in <i>any</i>
     * {@link MachineState} and may include both machines in non-terminal states
     * ({@link MachineState#REQUESTED}, {@link MachineState#PENDING},
     * {@link MachineState#RUNNING}) as well as machines in terminal states (
     * {@link MachineState#REJECTED}, {@link MachineState#TERMINATING},
     * {@link MachineState#TERMINATED}).
     *
     * @return
     */
    public List<Machine> getMachines() {
        return ImmutableList.copyOf(this.machines);
    }

    /**
     * Returns all active {@link Machine}s in the pool. See
     * {@link Machine#isActiveMember()}.
     *
     * @return
     */
    public List<Machine> getActiveMachines() {
        return getMachines().stream().filter(Machine.isActiveMember()).collect(Collectors.toList());
    }

    /**
     * Returns all <i>allocated</i> {@link Machine}s in the pool. See
     * {@link Machine#isAllocated()}.
     *
     * @return
     */
    public List<Machine> getAllocatedMachines() {
        return getMachines().stream().filter(Machine.isAllocated()).collect(Collectors.toList());
    }

    /**
     * Returns all <i>started</i> {@link Machine}s in the pool. See
     * {@link Machine#isStarted()}.
     *
     * @return
     */
    public List<Machine> getStartedMachines() {
        return getMachines().stream().filter(Machine.isStarted()).collect(Collectors.toList());
    }

    /**
     * Returns the time at which the pool observation was made. Note that in
     * case the cloud pool serves locally cached data, this field may be used by
     * the client to determine if the data is fresh enough to be acted upon.
     *
     * @return
     */
    public DateTime getTimestamp() {
        return this.timestamp;
    }

    /**
     * Factory method for creating an empty machine pool.
     *
     * @param timestamp
     *            The timestamp of the machine pool.
     * @return
     */
    public static MachinePool emptyPool(DateTime timestamp) {
        return new MachinePool(new ArrayList<Machine>(), timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.timestamp, this.machines);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MachinePool) {
            MachinePool that = (MachinePool) obj;
            final boolean timestampsEqual;
            if (this.timestamp != null && that.timestamp != null) {
                timestampsEqual = this.timestamp.isEqual(that.timestamp);
            } else if (this.timestamp == null && that.timestamp == null) {
                timestampsEqual = true;
            } else {
                timestampsEqual = false;
            }
            return timestampsEqual && Objects.equal(this.machines, that.machines);
        }
        return false;
    }

    @Override
    public String toString() {
        List<String> shortPool = this.machines.stream().map(toShortString()).collect(Collectors.toList());
        return MoreObjects.toStringHelper(this).add("timestamp", this.timestamp).add("machines", shortPool).toString();
    }

    /**
     * Parses a JSON representation of a {@link MachinePool} to its Java
     * counterpart. Any failure to parse the JSON representation into a valid
     * {@link MachinePool} instance results in an exception being thrown.
     *
     * @param machinePoolAsJson
     * @return
     * @throws IOException
     */
    public static MachinePool fromJson(String machinePoolAsJson) throws IOException {
        MachinePool machinePool = JsonUtils.toObject(JsonUtils.parseJsonString(machinePoolAsJson), MachinePool.class);
        checkNotNull(machinePool.timestamp, "machine pool missing timestamp");
        checkNotNull(machinePool.machines, "machine pool missing instances");
        for (Machine machine : machinePool.machines) {
            checkNotNull(machine.getId(), "machine missing id");
            checkNotNull(machine.getMachineState(), "machine missing state");
        }
        return machinePool;
    }

    /**
     * Returns the JSON representation for this {@link MachinePool}.
     *
     * @return A {@link JsonObject} representation of this {@link MachinePool}.
     */
    public JsonObject toJson() {
        return JsonUtils.toJson(this).getAsJsonObject();
    }
}