package com.elastisys.scale.cloudpool.api.types;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;

import org.joda.time.DateTime;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Response message of the {@link CloudPool#getPoolSize()} operation that
 * returns information about the pool size -- both desired and actual size.
 */
public class PoolSizeSummary {

    /**
     * The time at which the pool size observation was made. Note that in case
     * the cloud pool serves locally cached data, this field may be used by the
     * client to determine if the data is fresh enough to be acted upon.
     */
    private final DateTime timestamp;

    /** The desired size of the machine pool. */
    private final int desiredSize;
    /**
     * The number of allocated machines in the pool (see
     * {@link Machine#isAllocated()}.
     */
    private final int allocated;
    /**
     * The number of active machines in the pool
     * {@link Machine#isActiveMember()}. That is, the number of allocated
     * machines that have also been marked with an active membership status.
     */
    private final int active;

    /**
     * @param desiredSize
     *            The desired size of the machine pool.
     * @param allocated
     *            The number of allocated machines in the pool (see
     *            {@link Machine#isAllocated()}.
     * @param active
     *            The number of active machines in the pool
     *            {@link Machine#isActiveMember()}. That is, the number of
     *            allocated machines that have also been marked with an active
     *            membership status.
     */
    public PoolSizeSummary(int desiredSize, int allocated, int active) {
        this(UtcTime.now(), desiredSize, allocated, active);
    }

    public PoolSizeSummary(DateTime timestamp, int desiredSize, int allocated, int active) {
        checkArgument(timestamp != null, "no timestamp given");
        checkArgument(desiredSize >= 0, "desiredSize must be >= 0");
        checkArgument(allocated >= 0, "allocated must be >= 0");
        checkArgument(active >= 0, "active must be >= 0");

        checkArgument(allocated >= active, "active cannot be greater than allocated");

        this.timestamp = timestamp;
        this.desiredSize = desiredSize;
        this.allocated = allocated;
        this.active = active;
    }

    /**
     * Returns the time at which the pool size observation was made. Note that
     * in case the cloud pool serves locally cached data, this field may be used
     * by the client to determine if the data is fresh enough to be acted upon.
     *
     * @return
     */
    public DateTime getTimestamp() {
        return this.timestamp;
    }

    /**
     * Returns the desired size of the machine pool.
     *
     * @return
     */
    public int getDesiredSize() {
        return this.desiredSize;
    }

    /**
     * Returns the number of allocated machines in the pool (see
     * {@link Machine#isAllocated()}.
     *
     * @return
     */
    public int getAllocated() {
        return this.allocated;
    }

    /**
     * The number of active machines in the pool
     * {@link Machine#isActiveMember()}. That is, the number of allocated
     * machines that have also been marked with an active membership status.
     *
     * @return
     */
    public int getActive() {
        return this.active;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.timestamp, this.desiredSize, this.allocated, this.active);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PoolSizeSummary) {
            PoolSizeSummary that = (PoolSizeSummary) obj;
            return Objects.equals(this.timestamp, that.timestamp) //
                    && Objects.equals(this.desiredSize, that.desiredSize) //
                    && Objects.equals(this.allocated, that.allocated) //
                    && Objects.equals(this.active, that.active);
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
