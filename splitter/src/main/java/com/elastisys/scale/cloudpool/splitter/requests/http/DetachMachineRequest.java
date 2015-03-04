package com.elastisys.scale.cloudpool.splitter.requests.http;

import static java.lang.String.format;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import java.util.concurrent.Callable;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;

/**
 * A {@link Callable} request that will execute
 * {@code POST /pool/<instance>/detach} against a cloud pool.
 */
public class DetachMachineRequest extends CloudPoolRequest<Void> {
	/** Machine to request cloud pool to detach. */
	private final String machineId;
	/** {@code true} if cloud pool should decrement its desired pool size. */
	private final boolean decrementDesiredSize;

	public DetachMachineRequest(PrioritizedCloudPool cloudPool,
			String machineId, boolean decrementDesiredSize) {
		super(cloudPool);
		this.machineId = machineId;
		this.decrementDesiredSize = decrementDesiredSize;
	}

	@Override
	public Void execute(AuthenticatedHttpClient client) throws Exception {
		String path = String.format("/pool/%s/detach", this.machineId);
		HttpPost request = new HttpPost(url(path));
		String message = format("{ \"decrementDesiredSize\": %s }",
				this.decrementDesiredSize);
		request.setEntity(new StringEntity(message, APPLICATION_JSON));
		client.execute(request);

		return null;
	}

}
