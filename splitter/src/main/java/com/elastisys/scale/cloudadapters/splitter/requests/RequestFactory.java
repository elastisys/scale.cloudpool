package com.elastisys.scale.cloudadapters.splitter.requests;

import java.util.concurrent.Callable;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapers.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudadapers.api.types.ServiceState;
import com.elastisys.scale.cloudadapters.splitter.Splitter;
import com.elastisys.scale.cloudadapters.splitter.config.PrioritizedCloudAdapter;

/**
 * A strategy for creating {@link Callable} tasks for carrying out operations
 * against remote {@link PrioritizedCloudAdapter}s.
 * 
 * @see Splitter
 */
public interface RequestFactory {
	/**
	 * Creates a {@link Callable} that calls
	 * {@link CloudAdapter#getMachinePool()} on the remote cloud adapter.
	 * 
	 * @param cloudAdapter
	 * @return
	 */
	Callable<MachinePool> newGetMachinePoolRequest(
			PrioritizedCloudAdapter cloudAdapter);

	/**
	 * Creates a {@link Callable} that calls {@link CloudAdapter#getPoolSize()}
	 * on the remote cloud adapter.
	 * 
	 * @param cloudAdapter
	 * @return
	 */
	Callable<PoolSizeSummary> newGetPoolSizeRequest(
			PrioritizedCloudAdapter cloudAdapter);

	/**
	 * Creates a {@link Callable} that calls
	 * {@link CloudAdapter#setDesiredSize(int)} on the remote cloud adapter.
	 * 
	 * @param cloudAdapter
	 * @param desiredSize
	 * @return
	 */
	Callable<Void> newSetDesiredSizeRequest(
			PrioritizedCloudAdapter cloudAdapter, int desiredSize);

	/**
	 * Creates a {@link Callable} that calls
	 * {@link CloudAdapter#terminateMachine(String, boolean)} on the remote
	 * cloud adapter.
	 * 
	 * @param cloudAdapter
	 * @param machineId
	 * @param decrementDesiredSize
	 * @return
	 */
	Callable<Void> newTerminateMachineRequest(
			PrioritizedCloudAdapter cloudAdapter, String machineId,
			boolean decrementDesiredSize);

	/**
	 * Creates a {@link Callable} that calls
	 * {@link CloudAdapter#setServiceState(String, ServiceState)} on the remote
	 * cloud adapter.
	 * 
	 * @param cloudAdapter
	 * @param machineId
	 * @param serviceState
	 * @return
	 */
	Callable<Void> newSetServiceStateRequest(
			PrioritizedCloudAdapter cloudAdapter, String machineId,
			ServiceState serviceState);

	/**
	 * Creates a {@link Callable} that calls
	 * {@link CloudAdapter#attachMachine(String)} on the remote cloud adapter.
	 * 
	 * @param cloudAdapter
	 * @param machineId
	 * @return
	 */
	Callable<Void> newAttachMachineRequest(
			PrioritizedCloudAdapter cloudAdapter, String machineId);

	/**
	 * Creates a {@link Callable} that calls
	 * {@link CloudAdapter#detachMachine(String, boolean)} on the remote cloud
	 * adapter.
	 * 
	 * @param cloudAdapter
	 * @param machineId
	 * @param decrementDesiredSize
	 * @return
	 */
	Callable<Void> newDetachMachineRequest(
			PrioritizedCloudAdapter cloudAdapter, String machineId,
			boolean decrementDesiredSize);

}
