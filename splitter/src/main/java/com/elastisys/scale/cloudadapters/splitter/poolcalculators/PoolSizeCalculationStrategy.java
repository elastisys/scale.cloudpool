package com.elastisys.scale.cloudadapters.splitter.poolcalculators;

import java.util.Collection;
import java.util.Map;

import com.elastisys.scale.cloudadapters.splitter.Splitter;
import com.elastisys.scale.cloudadapters.splitter.config.PrioritizedCloudAdapter;

/**
 * Used by the {@link Splitter} to calculate how to divide a total desired pool
 * size across all back-end cloud adapters.
 */
public interface PoolSizeCalculationStrategy {

	/**
	 * Calculates the desired sizes to set for a collection of back-end cloud
	 * adapters given their individual priorities and a total desired pool size.
	 *
	 * @param backendCloudAdapters
	 *            The cloud adapters, in the order in which they were
	 *            configured, should that matter to the calculation.
	 * @param desiredTotalSize
	 *            The desired total pool size.
	 * @return A mapping between back-end cloud adapters and the size they
	 *         should resize their pool to.
	 */
	Map<PrioritizedCloudAdapter, Integer> calculatePoolSizes(
			Collection<PrioritizedCloudAdapter> backendCloudAdapters,
			final int desiredTotalSize);

}
