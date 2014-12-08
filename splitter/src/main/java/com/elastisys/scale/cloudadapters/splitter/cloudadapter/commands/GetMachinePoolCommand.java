package com.elastisys.scale.cloudadapters.splitter.cloudadapter.commands;

import java.util.concurrent.Callable;

import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.PrioritizedRemoteCloudAdapter;
import com.google.common.base.Preconditions;

/**
 * Issues a command to a remote cloud adapter to return its machine pool.
 */
public class GetMachinePoolCommand implements Callable<MachinePool> {
	private final PrioritizedRemoteCloudAdapter cloudAdapter;

	/**
	 * Creates a new instance that, when called, will issue the
	 * {@link PrioritizedRemoteCloudAdapter#getMachinePool()} command to the
	 * remote cloud adapter.
	 *
	 * @param cloudAdapter
	 *            The remote cloud adapter.
	 */
	public GetMachinePoolCommand(PrioritizedRemoteCloudAdapter cloudAdapter) {
		Preconditions
				.checkNotNull(cloudAdapter, "Cloud adapter cannot be null");
		this.cloudAdapter = cloudAdapter;
	}

	@Override
	public MachinePool call() throws Exception {
		return this.cloudAdapter.getMachinePool();
	}

}
