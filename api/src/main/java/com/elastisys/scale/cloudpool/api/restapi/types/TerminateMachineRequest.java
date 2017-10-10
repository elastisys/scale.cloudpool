package com.elastisys.scale.cloudpool.api.restapi.types;

import com.elastisys.scale.cloudpool.api.restapi.CloudPoolRestApi;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Objects;

/**
 * REST API request type that requests that a machine be terminated.
 *
 * @see CloudPoolRestApi#terminateMachine(TerminateMachineRequest)
 */
public class TerminateMachineRequest {

    /** The identifier of the machine to be terminated. */
    private final String machineId;

    /**
     * If {@code true}, the desired size of the group should be decremented, if
     * {@code false} it should be left at its current value.
     */
    private final boolean decrementDesiredSize;

    public TerminateMachineRequest(String machineId, boolean decrementDesiredSize) {
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
        return Objects.hashCode(this.machineId, this.decrementDesiredSize);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TerminateMachineRequest) {
            TerminateMachineRequest that = (TerminateMachineRequest) obj;
            return Objects.equal(this.machineId, that.machineId)
                    && Objects.equal(this.decrementDesiredSize, that.decrementDesiredSize);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
