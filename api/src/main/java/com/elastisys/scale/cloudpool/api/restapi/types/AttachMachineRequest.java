package com.elastisys.scale.cloudpool.api.restapi.types;

import java.util.Objects;

import com.elastisys.scale.cloudpool.api.restapi.CloudPoolRestApi;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * REST API request type that requests a certain desired size of the machine
 * pool.
 *
 * @see CloudPoolRestApi#attachMachine(AttachMachineRequest)
 */
public class AttachMachineRequest {

    /** Identifier of machine to be attach to cloudpool. */
    private final String machineId;

    public AttachMachineRequest(String machineId) {
        this.machineId = machineId;
    }

    public String getMachineId() {
        return this.machineId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.machineId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AttachMachineRequest) {
            AttachMachineRequest that = (AttachMachineRequest) obj;
            return Objects.equals(this.machineId, that.machineId);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
