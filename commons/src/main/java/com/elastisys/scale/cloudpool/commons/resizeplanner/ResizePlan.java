package com.elastisys.scale.cloudpool.commons.resizeplanner;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.commons.termqueue.ScheduledTermination;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * Captures actions required to resize a machine pool to a suitable size.
 * <p/>
 * A {@link ResizePlan} is the outcome of calculations performed by a
 * {@link ResizePlanner}.
 *
 * @see ResizePlanner
 */
public class ResizePlan {

    /** Number of additional machines to request. */
    private final int toRequest;
    /**
     * Number of machines currently in termination queue that shall be spared,
     * rather than terminated
     */
    private final int toSpare;
    /**
     * The {@link Machine}s that are to be terminated, together with their
     * scheduled termination time.
     */
    private final List<ScheduledTermination> toTerminate;

    /**
     * Creates a new {@link ResizePlan}.
     *
     * @param toRequest
     *            Number of additional machines to request.
     * @param toSpare
     *            Number of machines currently in termination queue that shall
     *            be spared, rather than terminated
     * @param toTerminate
     *            The pool {@link Machine}s that are to be terminated, together
     *            with their scheduled termination time. Can be set to
     *            <code>null</code>, which has the same effect as setting an
     *            empty list.
     */
    public ResizePlan(int toRequest, int toSpare, List<ScheduledTermination> toTerminate) {
        this.toSpare = toSpare;
        this.toRequest = toRequest;
        this.toTerminate = Optional.fromNullable(toTerminate).or(new ArrayList<ScheduledTermination>());
        validate();
    }

    /**
     * Performs a basic sanity check of this {@link ResizePlan}. If values are
     * sane, the method simply returns. Should the {@link ResizePlan} contain an
     * illegal mix of values, an {@link IllegalArgumentException} is thrown.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.toSpare >= 0, "negative number of machines to spare");
        checkArgument(this.toRequest >= 0, "negative number of additional machines to request");
    }

    /**
     * Returns the number of machines currently in termination queue that shall
     * be spared, rather than terminated.
     *
     * @return
     */
    public int getToSpare() {
        return this.toSpare;
    }

    /**
     * Returns the number of additional machines to request.
     *
     * @return
     */
    public int getToRequest() {
        return this.toRequest;
    }

    /**
     * Returns the {@link Machine}s that are to be terminated, together with
     * their scheduled termination time.
     *
     * @return
     */
    public List<ScheduledTermination> getToTerminate() {
        return this.toTerminate;
    }

    /**
     * Indicate if this {@link ResizePlan} suggests scale-out actions.
     *
     * @return <code>true</code> if this {@link ResizePlan} suggests that
     *         scale-out actions are to be taken.
     */
    public boolean hasScaleOutActions() {
        return (this.toRequest > 0) || (this.toSpare > 0);
    }

    /**
     * Indicate if this {@link ResizePlan} suggests scale-in actions.
     *
     * @return <code>true</code> if this {@link ResizePlan} suggests that
     *         scale-in actions are to be taken.
     */
    public boolean hasScaleInActions() {
        return !this.toTerminate.isEmpty();
    }

    /**
     * <code>true</code> if this {@link ResizePlan} neither suggests any
     * scale-in nor any scale-out actions.
     *
     * @return
     */
    public boolean noChanges() {
        return !hasScaleInActions() && !hasScaleOutActions();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.toRequest, this.toSpare, this.toTerminate);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ResizePlan) {
            ResizePlan that = (ResizePlan) obj;
            return Objects.equal(this.toRequest, that.toRequest) && Objects.equal(this.toSpare, that.toSpare)
                    && Objects.equal(this.toTerminate, that.toTerminate);
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("toRequest", this.toRequest).add("toSpare", this.toSpare)
                .add("toTerminate", this.toTerminate).toString();
    }
}
