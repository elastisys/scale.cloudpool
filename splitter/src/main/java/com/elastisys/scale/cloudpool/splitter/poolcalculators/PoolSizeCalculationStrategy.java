package com.elastisys.scale.cloudpool.splitter.poolcalculators;

import java.util.Collection;
import java.util.Map;

import com.elastisys.scale.cloudpool.splitter.Splitter;
import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;

/**
 * Used by the {@link Splitter} to calculate how to divide a total desired pool
 * size across all back-end cloud pools.
 */
public interface PoolSizeCalculationStrategy {

	/**
	 * Calculates the desired sizes to set for a collection of back-end cloud
	 * pools given their individual priorities and a total desired pool size.
	 *
	 * @param backendCloudPools
	 *            The cloud pools, in the order in which they were configured,
	 *            should that matter to the calculation.
	 * @param desiredTotalSize
	 *            The desired total pool size.
	 * @return A mapping between back-end cloud pools and the size they should
	 *         resize their pool to.
	 */
	Map<PrioritizedCloudPool, Integer> calculatePoolSizes(
			Collection<PrioritizedCloudPool> backendCloudPools,
			final int desiredTotalSize);

}
