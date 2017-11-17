package com.elastisys.scale.cloudpool.commons.basepool.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Control's the {@link CloudPool}'s behavior with respect to how often to
 * attempt fetching of {@link MachinePool} and for how long to mask fetch
 * errors.
 */
public class PoolFetchConfig {

    /** Retry handling when fetching pool members from the cloud API fails. */
    private final RetriesConfig retries;
    /**
     * How often to refresh the cloud pool's view of the {@link MachinePool}
     * members.
     */
    private final TimeInterval refreshInterval;
    /**
     * How long to respond with cached {@link MachinePool} observations before
     * responding with a cloud reachability error. In other words, for how long
     * should failures to fetch the machine pool be masked.
     */
    private final TimeInterval reachabilityTimeout;

    /**
     * Creates a {@link PoolFetchConfig}.
     *
     * @param retries
     *            Retry handling when fetching pool members from the cloud API
     *            fails.
     * @param refreshInterval
     *            How often to refresh the cloud pool's view of the
     *            {@link MachinePool} members.
     * @param reachabilityTimeout
     *            How long to respond with cached {@link MachinePool}
     *            observations before responding with a cloud reachability
     *            error. In other words, for how long should failures to fetch
     *            the machine pool be masked.
     */
    public PoolFetchConfig(RetriesConfig retries, TimeInterval refreshInterval, TimeInterval reachabilityTimeout) {
        this.retries = retries;
        this.refreshInterval = refreshInterval;
        this.reachabilityTimeout = reachabilityTimeout;
    }

    /**
     * Retry handling when fetching pool members from the cloud API fails.
     *
     * @return
     */
    public RetriesConfig getRetries() {
        return this.retries;
    }

    /**
     * How often to refresh the cloud pool's view of the {@link MachinePool}
     * members.
     *
     * @return
     */
    public TimeInterval getRefreshInterval() {
        return this.refreshInterval;
    }

    /**
     * How long to respond with cached {@link MachinePool} observations before
     * responding with a cloud reachability error. In other words, for how long
     * should failures to fetch the machine pool be masked.
     *
     * @return
     */
    public TimeInterval getReachabilityTimeout() {
        return this.reachabilityTimeout;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.retries, this.refreshInterval, this.reachabilityTimeout);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PoolFetchConfig) {
            PoolFetchConfig that = (PoolFetchConfig) obj;
            return Objects.equals(this.retries, that.retries) //
                    && Objects.equals(this.refreshInterval, that.refreshInterval) //
                    && Objects.equals(this.reachabilityTimeout, that.reachabilityTimeout);

        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.retries != null, "poolFetch: missing retries");
        checkArgument(this.refreshInterval != null, "poolFetch: missing refreshInterval");
        checkArgument(this.reachabilityTimeout != null, "poolFetch: missing reachabilityTimeout");

        this.retries.validate();
        this.refreshInterval.validate();
        this.reachabilityTimeout.validate();

        long refreshMillis = TimeUnit.MILLISECONDS.convert(this.refreshInterval.getTime(),
                this.refreshInterval.getUnit());
        long timeoutMillis = TimeUnit.MILLISECONDS.convert(this.reachabilityTimeout.getTime(),
                this.reachabilityTimeout.getUnit());
        checkArgument(refreshMillis < timeoutMillis,
                "poolFetch: reachabilityTimeout cannot be shorter than refreshInterval");
    }

}
