package com.elastisys.scale.cloudpool.splitter.requests;

import java.util.concurrent.Callable;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.splitter.Splitter;
import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;

/**
 * A strategy for creating {@link Callable} tasks for carrying out operations
 * against remote {@link PrioritizedCloudPool}s.
 *
 * @see Splitter
 */
public interface RequestFactory {
	/**
	 * Creates a {@link Callable} that calls {@link CloudPool#getMachinePool()}
	 * on the remote cloud pool.
	 *
	 * @param cloudPool
	 * @return
	 */
	Callable<MachinePool> newGetMachinePoolRequest(
			PrioritizedCloudPool cloudPool);

	/**
	 * Creates a {@link Callable} that calls {@link CloudPool#getPoolSize()} on
	 * the remote cloud pool.
	 *
	 * @param cloudPool
	 * @return
	 */
	Callable<PoolSizeSummary> newGetPoolSizeRequest(
			PrioritizedCloudPool cloudPool);

	/**
	 * Creates a {@link Callable} that calls
	 * {@link CloudPool#setDesiredSize(int)} on the remote cloud pool.
	 *
	 * @param cloudPool
	 * @param desiredSize
	 * @return
	 */
	Callable<Void> newSetDesiredSizeRequest(PrioritizedCloudPool cloudPool,
			int desiredSize);

	/**
	 * Creates a {@link Callable} that calls
	 * {@link CloudPool#terminateMachine(String, boolean)} on the remote cloud
	 * pool.
	 *
	 * @param cloudPool
	 * @param machineId
	 * @param decrementDesiredSize
	 * @return
	 */
	Callable<Void> newTerminateMachineRequest(PrioritizedCloudPool cloudPool,
			String machineId, boolean decrementDesiredSize);

	/**
	 * Creates a {@link Callable} that calls
	 * {@link CloudPool#setServiceState(String, ServiceState)} on the remote
	 * cloud pool.
	 *
	 * @param cloudPool
	 * @param machineId
	 * @param serviceState
	 * @return
	 */
	Callable<Void> newSetServiceStateRequest(PrioritizedCloudPool cloudPool,
			String machineId, ServiceState serviceState);

	/**
	 * Creates a {@link Callable} that calls
	 * {@link CloudPool#attachMachine(String)} on the remote cloud pool.
	 *
	 * @param cloudPool
	 * @param machineId
	 * @return
	 */
	Callable<Void> newAttachMachineRequest(PrioritizedCloudPool cloudPool,
			String machineId);

	/**
	 * Creates a {@link Callable} that calls
	 * {@link CloudPool#detachMachine(String, boolean)} on the remote cloud
	 * pool.
	 *
	 * @param cloudPool
	 * @param machineId
	 * @param decrementDesiredSize
	 * @return
	 */
	Callable<Void> newDetachMachineRequest(PrioritizedCloudPool cloudPool,
			String machineId, boolean decrementDesiredSize);

}
