package com.elastisys.scale.cloudpool.api.restapi.types;

import java.util.Objects;

import com.elastisys.scale.cloudpool.api.restapi.CloudPoolRestApi;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * REST API request type that requests that a machine be detached from the
 * machine pool.
 *
 * @see CloudPoolRestApi#detachMachine(String, DetachMachineRequest)
 */
public class DetachMachineRequest {

    /** The identifier of the machine to be detached from the cloudpool. */
    private final String machineId;
    /**
     * If {@code true}, the desired size of the group should be decremented, if
     * {@code false} it should be left at its current value.
     */
    private final boolean decrementDesiredSize;

    public DetachMachineRequest(String machineId, boolean decrementDesiredSize) {
        this.machineId = machineId;
        this.decrementDesiredSize = decrementDesiredSize;
    }

    public String getMachineId() {
        return this.machineId;
    }

    /**
     * Indicates if the desired size of the group should be decremented after
     * terminating the machine.
     *
     * @return
     */
    public boolean isDecrementDesiredSize() {
        return this.decrementDesiredSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.machineId, this.decrementDesiredSize);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DetachMachineRequest) {
            DetachMachineRequest that = (DetachMachineRequest) obj;
            return Objects.equals(this.machineId, that.machineId) //
                    && Objects.equals(this.decrementDesiredSize, that.decrementDesiredSize);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
