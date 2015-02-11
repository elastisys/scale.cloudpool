package com.elastisys.scale.cloudadapters.splitter.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import jersey.repackaged.com.google.common.collect.Sets;

import com.elastisys.scale.cloudadapters.splitter.Splitter;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * Represents a configuration for a {@link Splitter}.
 */
public class SplitterConfig {
	/** Default value for {@link #poolUpdatePeriod}. */
	public static final long DEFAULT_POOL_UPDATE_PERIOD = 60;

	/** The pool size calculation strategy to use. */
	private final PoolSizeCalculator poolSizeCalculator;

	/** The list of back-end cloud adapters and their individual priorities. */
	private final List<PrioritizedCloudAdapter> adapters;

	/**
	 * The time interval (in seconds) between pushing out the desired size to
	 * each child adapter.
	 */
	private final Long poolUpdatePeriod;

	/**
	 * Creates a {@link SplitterConfig}.
	 *
	 * @param poolSizeCalculator
	 *            The pool size calculation strategy to use.
	 * @param adapters
	 *            The list of back-end cloud adapters and their individual
	 *            priorities.
	 * @param poolUpdatePeriod
	 *            The time interval (in seconds) between pushing out the desired
	 *            size to each child adapter.
	 */
	public SplitterConfig(PoolSizeCalculator poolSizeCalculator,
			List<PrioritizedCloudAdapter> adapters, long poolUpdatePeriod) {
		this.poolSizeCalculator = poolSizeCalculator;
		this.adapters = adapters;
		this.poolUpdatePeriod = poolUpdatePeriod;
		this.validate();
	}

	/**
	 * Returns the pool size calculation strategy to use.
	 *
	 * @return
	 */
	public PoolSizeCalculator getPoolSizeCalculator() {
		return this.poolSizeCalculator;
	}

	/**
	 * Returns the list of back-end cloud adapters and their individual
	 * priorities.
	 *
	 * @return
	 */
	public List<PrioritizedCloudAdapter> getAdapters() {
		return this.adapters;
	}

	/**
	 * Returns the time interval (in seconds) between pushing out the desired
	 * size to each child adapter.
	 *
	 * @return
	 */
	public long getPoolUpdatePeriod() {
		return Optional.fromNullable(this.poolUpdatePeriod).or(
				DEFAULT_POOL_UPDATE_PERIOD);
	}

	public void validate() throws IllegalArgumentException {
		checkArgument(this.poolSizeCalculator != null,
				"no poolSizeCalculator set");
		checkArgument(this.adapters != null && !this.adapters.isEmpty(),
				"no adapters set");
		checkArgument(getPoolUpdatePeriod() > 0,
				"poolUpdatePeriod must be a positive number");

		// duplicate cloud adapters are not allowed
		checkArgument(
				Sets.newHashSet(this.adapters).size() == this.adapters.size(),
				"cannot contain duplicate adapters (with same host and port)");

		// validate each adapter and make sure the priorities add up to 100
		int prioritySum = 0;
		for (PrioritizedCloudAdapter adapter : this.adapters) {
			adapter.validate();
			prioritySum += adapter.getPriority();
		}
		checkArgument(prioritySum == 100,
				"adapter priorities must add up to 100");
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SplitterConfig) {
			SplitterConfig that = (SplitterConfig) obj;
			return Objects.equal(this.poolSizeCalculator,
					that.poolSizeCalculator)
					&& Objects.equal(this.adapters, that.adapters)
					&& Objects.equal(this.getPoolUpdatePeriod(),
							that.getPoolUpdatePeriod());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.poolSizeCalculator, this.adapters,
				this.getPoolUpdatePeriod());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("poolSizeCalculator", this.poolSizeCalculator)
				.add("adapters", this.adapters)
				.add("poolUpdatePeriod", this.poolUpdatePeriod).toString();
	}
}
