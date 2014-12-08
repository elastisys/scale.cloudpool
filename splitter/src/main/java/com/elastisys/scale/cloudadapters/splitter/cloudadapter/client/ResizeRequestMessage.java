package com.elastisys.scale.cloudadapters.splitter.cloudadapter.client;

import java.util.Objects;

import com.google.common.base.Preconditions;

/**
 * A message used to request that the machine pool be resized to a desired
 * number of machine instances. See the <a href=
 * "http://cloudadapterapi.readthedocs.org/en/latest/api.html#resize-request-message"
 * >API</a>.
 *
 * <p>
 * This class is thread-safe by being immutable.
 * </p>
 */
public final class ResizeRequestMessage {
	private final long desiredCapacity;

	/**
	 * Creates a new instance with the given desired capacity.
	 *
	 * @param desiredCapacity
	 *            The desired capacity.
	 */
	public ResizeRequestMessage(long desiredCapacity) {
		Preconditions.checkArgument(desiredCapacity >= 0,
				"Desired capacity should be positive");
		this.desiredCapacity = desiredCapacity;
	}

	/**
	 * @return The desired capacity.
	 */
	public long getDesiredCapacity() {
		return this.desiredCapacity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.desiredCapacity);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ResizeRequestMessage that = (ResizeRequestMessage) obj;
		return Objects.equals(this.desiredCapacity, that.desiredCapacity);
	}
}
