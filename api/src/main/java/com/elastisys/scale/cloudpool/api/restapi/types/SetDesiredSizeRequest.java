package com.elastisys.scale.cloudpool.api.restapi.types;

import java.util.Objects;

import com.elastisys.scale.cloudpool.api.restapi.CloudPoolRestApi;

/**
 * REST API request type that requests a certain desired size of the machine
 * pool.
 *
 * @see CloudPoolRestApi#setDesiredSize(SetDesiredSizeRequest)
 */
public class SetDesiredSizeRequest {

    /** The desired size to set. */
    private final int desiredSize;

    public SetDesiredSizeRequest(int desiredSize) {
        this.desiredSize = desiredSize;
    }

    public int getDesiredSize() {
        return this.desiredSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.desiredSize);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SetDesiredSizeRequest) {
            SetDesiredSizeRequest that = (SetDesiredSizeRequest) obj;
            return Objects.equals(this.desiredSize, that.desiredSize);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("{\"desiredSize\": %d}", this.desiredSize);
    }
}
