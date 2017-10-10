package com.elastisys.scale.cloudpool.api.restapi.types;

import com.elastisys.scale.cloudpool.api.restapi.CloudPoolRestApi;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Objects;

/**
 * REST API request type that requests a certain service state be set for a
 * machine in the pool.
 *
 * @see CloudPoolRestApi#setServiceState(SetServiceStateRequest)
 */
public class SetServiceStateRequest {

    /** The identifier of the machine whose service state is to be set. */
    private final String machineId;

    /** Service state to set for machine. */
    private final ServiceState serviceState;

    public SetServiceStateRequest(String machineId, ServiceState serviceState) {
        this.machineId = machineId;
        this.serviceState = serviceState;
    }

    public String getMachineId() {
        return this.machineId;
    }

    public ServiceState getServiceState() {
        return this.serviceState;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.machineId, this.serviceState);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SetServiceStateRequest) {
            SetServiceStateRequest that = (SetServiceStateRequest) obj;
            return Objects.equal(this.machineId, that.machineId) && Objects.equal(this.serviceState, that.serviceState);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
