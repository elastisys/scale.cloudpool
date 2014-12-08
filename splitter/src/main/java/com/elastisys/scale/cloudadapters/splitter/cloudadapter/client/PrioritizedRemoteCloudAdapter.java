package com.elastisys.scale.cloudadapters.splitter.cloudadapter.client;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config.ConfigurationException;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config.PrioritizedRemoteCloudAdapterConfig;

/**
 * A cloud adapter that does not support
 */
public interface PrioritizedRemoteCloudAdapter extends
Comparable<PrioritizedRemoteCloudAdapter> {

	/**
	 * Configures this remote cloud adapter proxy with the given values.
	 *
	 * @param configuration
	 *            The configuration required to connect to the remote cloud
	 *            adapter.
	 * @throws ConfigurationException
	 *             Thrown if the configuration cannot be validated, i.e., there
	 *             is something wrong with it (missing or unacceptable value).
	 */
	void configure(final PrioritizedRemoteCloudAdapterConfig configuration)
			throws ConfigurationException;

	/**
	 * @return The priority of this prioritized cloud adapter.
	 */
	int getPriority();

	/**
	 * Returns the remote cloud adapter's machine pool.
	 */
	MachinePool getMachinePool() throws CloudAdapterException;

	/**
	 * Tells the remote cloud adapter to resize the machine pool it manages.
	 */
	void resizeMachinePool(final long desiredCapacity)
			throws IllegalArgumentException, CloudAdapterException;

	/**
	 * Sort order MUST be descending with regard to priority.
	 */
	@Override
	int compareTo(PrioritizedRemoteCloudAdapter other);

}