package com.elastisys.scale.cloudpool.splitter.requests.http;

import java.util.concurrent.Callable;

import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.splitter.Splitter;
import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.elastisys.scale.cloudpool.splitter.requests.RequestFactory;

/**
 * A {@link RequestFactory} that produces request tasks that invoke cloud pool
 * over HTTP.
 *
 * @see Splitter
 */
public class HttpRequestFactory implements RequestFactory {

	@Override
	public Callable<MachinePool> newGetMachinePoolRequest(
			PrioritizedCloudPool cloudPool) {
		return new GetMachinePoolRequest(cloudPool);
	}

	@Override
	public Callable<PoolSizeSummary> newGetPoolSizeRequest(
			PrioritizedCloudPool cloudPool) {
		return new GetPoolSizeRequest(cloudPool);
	}

	@Override
	public Callable<Void> newSetDesiredSizeRequest(
			PrioritizedCloudPool cloudPool, int desiredSize) {
		return new SetDesiredSizeRequest(cloudPool, desiredSize);
	}

	@Override
	public Callable<Void> newTerminateMachineRequest(
			PrioritizedCloudPool cloudPool, String machineId,
			boolean decrementDesiredSize) {
		return new TerminateMachineRequest(cloudPool, machineId,
				decrementDesiredSize);
	}

	@Override
	public Callable<Void> newSetServiceStateRequest(
			PrioritizedCloudPool cloudPool, String machineId,
			ServiceState serviceState) {
		return new SetServiceStateRequest(cloudPool, machineId, serviceState);
	}

	@Override
	public Callable<Void> newSetMembershipStatusRequest(
			PrioritizedCloudPool cloudPool, String machineId,
			MembershipStatus membershipStatus) {
		return new SetMembershipStatusRequest(cloudPool, machineId,
				membershipStatus);
	}

	@Override
	public Callable<Void> newAttachMachineRequest(
			PrioritizedCloudPool cloudPool, String machineId) {
		return new AttachMachineRequest(cloudPool, machineId);
	}

	@Override
	public Callable<Void> newDetachMachineRequest(
			PrioritizedCloudPool cloudPool, String machineId,
			boolean decrementDesiredSize) {
		return new DetachMachineRequest(cloudPool, machineId,
				decrementDesiredSize);
	}

}
