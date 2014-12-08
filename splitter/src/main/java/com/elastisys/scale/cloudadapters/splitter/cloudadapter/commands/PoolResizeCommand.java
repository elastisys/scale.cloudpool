package com.elastisys.scale.cloudadapters.splitter.cloudadapter.commands;

import java.util.concurrent.Callable;

import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.PrioritizedRemoteCloudAdapter;
import com.google.common.base.Preconditions;

/**
 * Issues a command to a remote cloud adapter to resize its machine pool.
 */
public class PoolResizeCommand implements Callable<Void> {

	private final long desiredSize;
	private final PrioritizedRemoteCloudAdapter cloudAdapter;

	/**
	 * Creates a new instance, which when called, will issue the
	 * {@link PrioritizedRemoteCloudAdapter#resizeMachinePool(int)} command with
	 * the given desired size.
	 *
	 * @param cloudAdapter
	 *            The cloud adapter that shall carry out the operation.
	 * @param desiredSize
	 *            The desired size of the machine pool.
	 */
	public PoolResizeCommand(PrioritizedRemoteCloudAdapter cloudAdapter,
			final long desiredSize) {
		Preconditions
				.checkNotNull(cloudAdapter, "Cloud adapter cannot be null");
		Preconditions.checkArgument(desiredSize >= 0,
				"Desired size must be positive");
		this.desiredSize = desiredSize;
		this.cloudAdapter = cloudAdapter;
	}

	@Override
	public Void call() throws Exception {
		this.cloudAdapter.resizeMachinePool(this.desiredSize);
		return null;
	}
}
