package com.elastisys.scale.cloudadapters.commons.resizeplanner;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.commons.termqueue.ScheduledTermination;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * Captures actions required to resize a machine pool to a suitable size.
 * <p/>
 * A {@link ResizePlan} is the outcome of calculations performed by a
 * {@link ResizePlanner}.
 * 
 * @see ResizePlanner
 * 
 * 
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
	public ResizePlan(int toRequest, int toSpare,
			List<ScheduledTermination> toTerminate) {
		this.toSpare = toSpare;
		this.toRequest = toRequest;
		this.toTerminate = Optional.fromNullable(toTerminate).or(
				new ArrayList<ScheduledTermination>());
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
		checkArgument(this.toRequest >= 0,
				"negative number of additional machines to request");

		// check for contradictory values
		int decrease = this.toTerminate.size();
		int increase = this.toSpare + this.toRequest;
		if ((decrease > 0) && (increase > 0)) {
			throw new IllegalArgumentException("resize plan is ambigous: "
					+ "suggests both increasing and decreasing pool");
		}
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
	 * Indicate if this {@link ResizePlan} represents a scale-up (adding of
	 * capacity) of the machine pool.
	 * 
	 * @return <code>true</code> if this {@link ResizePlan} reperesents a
	 *         scale-up. <code>false</code> otherwise.
	 */
	public boolean isScaleUp() {
		return (this.toRequest > 0) || (this.toSpare > 0);
	}

	/**
	 * Indicate if this {@link ResizePlan} represents a scale-down (down-size of
	 * capacity) of the machine pool.
	 * 
	 * @return <code>true</code> if this {@link ResizePlan} reperesents a
	 *         scale-down. <code>false</code> otherwise.
	 */
	public boolean isScaleDown() {
		return !this.toTerminate.isEmpty();
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.toRequest, this.toSpare, this.toTerminate);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ResizePlan) {
			ResizePlan that = (ResizePlan) obj;
			return Objects.equal(this.toRequest, that.toRequest)
					&& Objects.equal(this.toSpare, that.toSpare)
					&& Objects.equal(this.toTerminate, that.toTerminate);
		}
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("toRequest", this.toRequest)
				.add("toSpare", this.toSpare)
				.add("toTerminate", this.toTerminate).toString();
	}
}
