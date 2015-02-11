package com.elastisys.scale.cloudadapters.splitter.poolcalculators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapters.splitter.config.PrioritizedCloudAdapter;

/**
 * A {@link PoolSizeCalculationStrategy} that ignores current machine pools and
 * the statuses of the machines therein, and merely strictly enforces the
 * fractions as given by the priorities and configuration order of the cloud
 * adapters.
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
	public Map<PrioritizedCloudAdapter, Integer> calculatePoolSizes(
			Collection<PrioritizedCloudAdapter> backendCloudAdapters,
			final int desiredTotalSize) throws CloudAdapterException {
		Map<PrioritizedCloudAdapter, Integer> poolSizes = new HashMap<>();

		List<PrioritizedCloudAdapter> sortedAdapters = new ArrayList<>(
				backendCloudAdapters);
		// Stable sort: maintains order in which they were configured if the
		// priorities are equal
		Collections.sort(sortedAdapters);

		long remainder = desiredTotalSize;
		for (PrioritizedCloudAdapter adapter : sortedAdapters) {
			double fraction = adapter.getPriority() / 100.0d;
			int instances = (int) Math.min(
					Math.ceil(desiredTotalSize * fraction), remainder);
			poolSizes.put(adapter, instances);
			remainder -= instances;
		}

		return poolSizes;
	}

}
