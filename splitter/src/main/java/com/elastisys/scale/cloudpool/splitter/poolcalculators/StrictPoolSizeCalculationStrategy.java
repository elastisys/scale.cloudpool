package com.elastisys.scale.cloudpool.splitter.poolcalculators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;

/**
 * A {@link PoolSizeCalculationStrategy} that ignores current machine pools and
 * the statuses of the machines therein, and merely strictly enforces the
 * fractions as given by the priorities and configuration order of the cloud
 * pools.
 * <p>
 * This class is a thread-safe singleton. The singleton instance only provides
 * only a function of its inputs.
 * </p>
 */
public enum StrictPoolSizeCalculationStrategy implements
		PoolSizeCalculationStrategy {
	/** The only instance of this class. */
	INSTANCE;

	@Override
	public Map<PrioritizedCloudPool, Integer> calculatePoolSizes(
			Collection<PrioritizedCloudPool> backendCloudPools,
			final int desiredTotalSize) throws CloudPoolException {
		Map<PrioritizedCloudPool, Integer> poolSizes = new HashMap<>();

		List<PrioritizedCloudPool> sortedPools = new ArrayList<>(
				backendCloudPools);
		// Stable sort: maintains order in which they were configured if the
		// priorities are equal
		Collections.sort(sortedPools);

		long remainder = desiredTotalSize;
		for (PrioritizedCloudPool pool : sortedPools) {
			double fraction = pool.getPriority() / 100.0d;
			int instances = (int) Math.min(
					Math.ceil(desiredTotalSize * fraction), remainder);
			poolSizes.put(pool, instances);
			remainder -= instances;
		}

		return poolSizes;
	}

}
