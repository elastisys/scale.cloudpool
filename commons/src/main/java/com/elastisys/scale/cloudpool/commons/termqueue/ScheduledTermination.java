package com.elastisys.scale.cloudpool.commons.termqueue;

import static com.google.common.base.Preconditions.checkNotNull;

import org.joda.time.DateTime;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Represents a scheduled machine instance termination in a
 * {@link TerminationQueue}.
 *
 * @see TerminationQueue
 *
 */
public class ScheduledTermination implements Comparable<ScheduledTermination> {

	/** The machine instance that is scheduled for termination. */
	private final Machine instance;

	/** The time at which the machine instance termination is due. */
	private final DateTime terminationTime;

	/**
	 * Creates a new {@link ScheduledTermination}.
	 *
	 * @param instance
	 *            The machine instance that is scheduled for termination.
	 * @param terminationTime
	 *            The time at which the machine instance termination is due.
	 */
	public ScheduledTermination(Machine instance, DateTime terminationTime) {
		checkNotNull(instance, "null instance");
		checkNotNull(terminationTime, "null terminationTime");
		this.instance = instance;
		this.terminationTime = terminationTime;
	}

	/**
	 * Returns the machine instance that is scheduled for termination.
	 *
	 * @return
	 */
	public Machine getInstance() {
		return this.instance;
	}

	/**
	 * Returns the time at which the machine instance termination is due.
	 *
	 * @return
	 */
	public DateTime getTerminationTime() {
		return this.terminationTime;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.instance, this.terminationTime);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ScheduledTermination) {
			ScheduledTermination that = ScheduledTermination.class.cast(obj);
			return Objects.equal(this.instance, that.instance)
					&& Objects
							.equal(this.terminationTime, that.terminationTime);
		}
		return super.equals(obj);
	}

	@Override
	public int compareTo(ScheduledTermination other) {
		return getTerminationTime().compareTo(other.getTerminationTime());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("instance", this.instance)
				.add("terminationTime", this.terminationTime).toString();
	}
}
