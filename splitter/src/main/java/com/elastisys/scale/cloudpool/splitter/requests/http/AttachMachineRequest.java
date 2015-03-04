package com.elastisys.scale.cloudpool.splitter.requests.http;

import java.util.concurrent.Callable;

import org.apache.http.client.methods.HttpPost;

import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;

/**
 * A {@link Callable} request that will execute
 * {@code POST /pool/<instance>/attach} against a cloud pool.
 */
public class AttachMachineRequest extends CloudPoolRequest<Void> {
	/** Machine to request cloud pool to attach. */
	private final String machineId;

	public AttachMachineRequest(PrioritizedCloudPool cloudPool, String machineId) {
		super(cloudPool);
		this.machineId = machineId;
	}

	@Override
	public Void execute(AuthenticatedHttpClient client) throws Exception {
		String path = String.format("/pool/%s/attach", this.machineId);
		HttpPost request = new HttpPost(url(path));
		client.execute(request);

		return null;
	}
}
