package com.elastisys.scale.cloudpool.api.types;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Objects;

/**
 * Represents an execution status for a {@link CloudPool}.
 *
 * @see CloudPool
 */
public class CloudPoolStatus {

    /** Indicates if the {@link CloudPool} is in a started state. */
    private final boolean started;
    /** Indicates if the {@link CloudPool} is configured. */
    private final boolean configured;

    /**
     * Creates a {@link CloudPoolStatus}.
     *
     * @param started
     *            Indicates if the {@link CloudPool} is in a started state.
     * @param configured
     *            Indicates if the {@link CloudPool} is configured.
     */
    public CloudPoolStatus(boolean started, boolean configured) {
        this.started = started;
        this.configured = configured;
    }

    /**
     * Indicates if the {@link CloudPool} is in a started state.
     *
     * @return
     */
    public boolean isStarted() {
        return this.started;
    }

    /**
     * Indicates if the {@link CloudPool} is configured.
     *
     * @return
     */
    public boolean isConfigured() {
        return this.configured;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.started, this.configured);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CloudPoolStatus) {
            CloudPoolStatus that = (CloudPoolStatus) obj;
            return Objects.equal(this.started, that.started) && Objects.equal(this.configured, that.configured);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
