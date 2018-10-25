package com.elastisys.scale.cloudpool.commons.resizeplanner;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.commons.json.JsonUtils;

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
     * The {@link Machine}s that are to be terminated.
     */
    private final List<Machine> toTerminate;

    /**
     * Creates a new {@link ResizePlan}.
     *
     * @param toRequest
     *            Number of additional machines to request.
     * @param toTerminate
     *            The pool {@link Machine}s that are to be terminated, together
     *            with their scheduled termination time. Can be set to
     *            <code>null</code>, which has the same effect as setting an
     *            empty list.
     */
    public ResizePlan(int toRequest, List<Machine> toTerminate) {
        this.toRequest = toRequest;
        this.toTerminate = Optional.ofNullable(toTerminate).orElse(Collections.emptyList());
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
        checkArgument(this.toRequest >= 0, "negative number of additional machines to request");
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
     * Returns the {@link Machine}s that are to be terminated.
     *
     * @return
     */
    public List<Machine> getToTerminate() {
        return this.toTerminate;
    }

    /**
     * Indicate if this {@link ResizePlan} suggests scale-out actions.
     *
     * @return <code>true</code> if this {@link ResizePlan} suggests that
     *         scale-out actions are to be taken.
     */
    public boolean hasScaleOutActions() {
        return this.toRequest > 0;
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
        return Objects.hash(this.toRequest, this.toTerminate);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ResizePlan) {
            ResizePlan that = (ResizePlan) obj;
            return Objects.equals(this.toRequest, that.toRequest) //
                    && Objects.equals(this.toTerminate, that.toTerminate);
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
