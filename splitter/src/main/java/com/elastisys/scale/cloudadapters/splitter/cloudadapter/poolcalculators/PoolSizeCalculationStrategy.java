package com.elastisys.scale.cloudadapters.splitter.cloudadapter.poolcalculators;

import java.util.Map;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.PrioritizedRemoteCloudAdapter;
import com.google.common.collect.ImmutableList;

/**
 * For a given set of cloud adapters, implementing classes should calculate and
 * emit a mapping between the adapters and the new pool size they should have to
 * reach the given desired total size.
 */
public interface PoolSizeCalculationStrategy {

	/**
	 * Calculates the correct pool sizes for the given cloud adapters.
	 *
	 * @param currentMachinePools
	 *            The current machine pools, for each adapter.
	 * @param cloudAdapters
	 *            The cloud adapters, in the order in which they were
	 *            configured, should that matter to the calculation.
	 * @param desiredTotalSize
	 *            The desired total pool size.
	 * @return A mapping between cloud adapters and the size they should resize
	 *         their pool to.
	 * @throws CloudAdapterException
	 *             Thrown if there is an error, e.g., while querying current
	 *             machine pools from the cloud adapters.
	 */
	Map<PrioritizedRemoteCloudAdapter, Long> calculatePoolSizes(
			Map<PrioritizedRemoteCloudAdapter, MachinePool> currentMachinePools,
			ImmutableList<PrioritizedRemoteCloudAdapter> cloudAdapters,
			final long desiredTotalSize) throws CloudAdapterException;

}
