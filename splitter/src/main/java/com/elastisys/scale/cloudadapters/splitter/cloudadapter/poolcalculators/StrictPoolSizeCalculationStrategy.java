package com.elastisys.scale.cloudadapters.splitter.cloudadapter.poolcalculators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.PrioritizedRemoteCloudAdapter;
import com.google.common.collect.ImmutableList;

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
	public Map<PrioritizedRemoteCloudAdapter, Long> calculatePoolSizes(
			Map<PrioritizedRemoteCloudAdapter, MachinePool> currentMachinePools,
			ImmutableList<PrioritizedRemoteCloudAdapter> cloudAdapters,
			final long desiredTotalSize) throws CloudAdapterException {
		Map<PrioritizedRemoteCloudAdapter, Long> poolSizes = new HashMap<PrioritizedRemoteCloudAdapter, Long>();

		List<PrioritizedRemoteCloudAdapter> sortedAdapters = new ArrayList<PrioritizedRemoteCloudAdapter>(
				cloudAdapters);
		/*
		 * Stable sort: maintains order in which they were configured if the
		 * priorities are equal
		 */
		Collections.sort(sortedAdapters);

		long remainder = desiredTotalSize;

		for (PrioritizedRemoteCloudAdapter adapter : sortedAdapters) {
			final double fraction = adapter.getPriority() / 100.0d;
			final long instances = (long) Math.min(
					Math.ceil(desiredTotalSize * fraction), remainder);

			poolSizes.put(adapter, instances);

			remainder -= instances;
		}

		return poolSizes;
	}

}
