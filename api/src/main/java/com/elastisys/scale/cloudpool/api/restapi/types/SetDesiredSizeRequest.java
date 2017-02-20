package com.elastisys.scale.cloudpool.api.restapi.types;

import com.elastisys.scale.cloudpool.api.restapi.CloudPoolRestApi;
import com.google.common.base.Objects;

/**
 * REST API request type that requests a certain desired size of the machine
 * pool.
 *
 * @see CloudPoolRestApi#setDesiredSize(SetDesiredSizeRequest)
 */
public class SetDesiredSizeRequest {

    private int desiredSize;

    public SetDesiredSizeRequest(int desiredSize) {
        this.desiredSize = desiredSize;
    }

    public void setDesiredSize(int desiredSize) {
        this.desiredSize = desiredSize;
    }

    public int getDesiredSize() {
        return this.desiredSize;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.desiredSize);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SetDesiredSizeRequest) {
            SetDesiredSizeRequest that = (SetDesiredSizeRequest) obj;
            return Objects.equal(this.desiredSize, that.desiredSize);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("{\"desiredSize\": %d}", this.desiredSize);
    }
}
