package com.elastisys.scale.cloudadapters.splitter.requests.http;

import java.util.concurrent.Callable;

import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapers.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudadapers.api.types.ServiceState;
import com.elastisys.scale.cloudadapters.splitter.Splitter;
import com.elastisys.scale.cloudadapters.splitter.config.PrioritizedCloudAdapter;
import com.elastisys.scale.cloudadapters.splitter.requests.RequestFactory;

/**
 * A {@link RequestFactory} that produces request tasks that invoke cloud
 * adapters over HTTP.
 *
 * @see Splitter
 */
public class HttpRequestFactory implements RequestFactory {

	@Override
	public Callable<MachinePool> newGetMachinePoolRequest(
			PrioritizedCloudAdapter cloudAdapter) {
		return new GetMachinePoolRequest(cloudAdapter);
	}

	@Override
	public Callable<PoolSizeSummary> newGetPoolSizeRequest(
			PrioritizedCloudAdapter cloudAdapter) {
		return new GetPoolSizeRequest(cloudAdapter);
	}

	@Override
	public Callable<Void> newSetDesiredSizeRequest(
			PrioritizedCloudAdapter cloudAdapter, int desiredSize) {
		return new SetDesiredSizeRequest(cloudAdapter, desiredSize);
	}

	@Override
	public Callable<Void> newTerminateMachineRequest(
			PrioritizedCloudAdapter cloudAdapter, String machineId,
			boolean decrementDesiredSize) {
		return new TerminateMachineRequest(cloudAdapter, machineId,
				decrementDesiredSize);
	}

	@Override
	public Callable<Void> newSetServiceStateRequest(
			PrioritizedCloudAdapter cloudAdapter, String machineId,
			ServiceState serviceState) {
		return new SetServiceStateRequest(cloudAdapter, machineId, serviceState);
	}

	@Override
	public Callable<Void> newAttachMachineRequest(
			PrioritizedCloudAdapter cloudAdapter, String machineId) {
		return new AttachMachineRequest(cloudAdapter, machineId);
	}

	@Override
	public Callable<Void> newDetachMachineRequest(
			PrioritizedCloudAdapter cloudAdapter, String machineId,
			boolean decrementDesiredSize) {
		return new DetachMachineRequest(cloudAdapter, machineId,
				decrementDesiredSize);
	}

}
