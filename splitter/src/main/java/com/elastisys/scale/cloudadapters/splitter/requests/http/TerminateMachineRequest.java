package com.elastisys.scale.cloudadapters.splitter.requests.http;

import static java.lang.String.format;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import java.util.concurrent.Callable;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.elastisys.scale.cloudadapters.splitter.config.PrioritizedCloudAdapter;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;

/**
 * A {@link Callable} request that will execute
 * {@code POST /pool/<instance>/terminate} against a cloud adapter.
 */
public class TerminateMachineRequest extends CloudAdapterRequest<Void> {

	/** Machine to request cloud adapter to terminate. */
	private final String machineId;
	/** {@code true} if cloud adapter should decrement its desired pool size. */
	private final boolean decrementDesiredSize;

	public TerminateMachineRequest(PrioritizedCloudAdapter cloudAdapter,
			String machineId, boolean decrementDesiredSize) {
		super(cloudAdapter);
		this.machineId = machineId;
		this.decrementDesiredSize = decrementDesiredSize;
	}

	@Override
	public Void execute(AuthenticatedHttpClient client) throws Exception {
		String path = String.format("/pool/%s/terminate", this.machineId);
		HttpPost request = new HttpPost(url(path));
		String message = format("{ \"decrementDesiredSize\": %s }",
				this.decrementDesiredSize);
		request.setEntity(new StringEntity(message, APPLICATION_JSON));
		client.execute(request);

		return null;
	}
}
