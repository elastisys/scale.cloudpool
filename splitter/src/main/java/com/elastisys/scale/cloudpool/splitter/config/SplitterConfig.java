package com.elastisys.scale.cloudpool.splitter.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import jersey.repackaged.com.google.common.collect.Sets;

import com.elastisys.scale.cloudpool.splitter.Splitter;
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

	/** The list of back-end cloud pools and their individual priorities. */
	private final List<PrioritizedCloudPool> backendPools;

	/**
	 * The time interval (in seconds) between pushing out the desired size to
	 * each child pool.
	 */
	private final Long poolUpdatePeriod;

	/**
	 * Creates a {@link SplitterConfig}.
	 *
	 * @param poolSizeCalculator
	 *            The pool size calculation strategy to use.
	 * @param pools
	 *            The list of back-end cloud pools and their individual
	 *            priorities.
	 * @param poolUpdatePeriod
	 *            The time interval (in seconds) between pushing out the desired
	 *            size to each child pool.
	 */
	public SplitterConfig(PoolSizeCalculator poolSizeCalculator,
			List<PrioritizedCloudPool> pools, long poolUpdatePeriod) {
		this.poolSizeCalculator = poolSizeCalculator;
		this.backendPools = pools;
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
	 * Returns the list of back-end cloud pools and their individual priorities.
	 *
	 * @return
	 */
	public List<PrioritizedCloudPool> getBackendPools() {
		return this.backendPools;
	}

	/**
	 * Returns the time interval (in seconds) between pushing out the desired
	 * size to each child pool.
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
		checkArgument(
				this.backendPools != null && !this.backendPools.isEmpty(),
				"no backendPools set");
		checkArgument(getPoolUpdatePeriod() > 0,
				"poolUpdatePeriod must be a positive number");

		// duplicate cloud pools are not allowed
		checkArgument(
				Sets.newHashSet(this.backendPools).size() == this.backendPools
						.size(),
				"cannot contain duplicate backend pools (with same host and port)");

		// validate each pool and make sure the priorities add up to 100
		int prioritySum = 0;
		for (PrioritizedCloudPool pool : this.backendPools) {
			pool.validate();
			prioritySum += pool.getPriority();
		}
		checkArgument(prioritySum == 100, "pool priorities must add up to 100");
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SplitterConfig) {
			SplitterConfig that = (SplitterConfig) obj;
			return Objects.equal(this.poolSizeCalculator,
					that.poolSizeCalculator)
					&& Objects.equal(this.backendPools, that.backendPools)
					&& Objects.equal(this.getPoolUpdatePeriod(),
							that.getPoolUpdatePeriod());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.poolSizeCalculator, this.backendPools,
				this.getPoolUpdatePeriod());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("poolSizeCalculator", this.poolSizeCalculator)
				.add("backendPools", this.backendPools)
				.add("poolUpdatePeriod", this.poolUpdatePeriod).toString();
	}
}
