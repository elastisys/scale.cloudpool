package com.elastisys.scale.cloudpool.api.restapi.types;

import com.elastisys.scale.cloudpool.api.restapi.CloudPoolHandler;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.google.common.base.Objects;

/**
 * REST API request type that requests a certain service state be set for a
 * machine in the pool.
 *
 * @see CloudPoolHandler#setServiceState(String, SetServiceStateRequest)
 */
public class SetServiceStateRequest {

    /**
     * Service state to set for instance.
     */
    private ServiceState serviceState;

    public SetServiceStateRequest(ServiceState serviceState) {
        this.serviceState = serviceState;
    }

    public void setServiceState(ServiceState serviceState) {
        this.serviceState = serviceState;
    }

    public ServiceState getServiceState() {
        return this.serviceState;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.serviceState);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SetServiceStateRequest) {
            SetServiceStateRequest that = (SetServiceStateRequest) obj;
            return Objects.equal(this.serviceState, that.serviceState);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("{\"serviceState\": %s}", this.serviceState.name());
    }
}
