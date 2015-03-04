package com.elastisys.scale.cloudpool.splitter.requests.http;

import static java.lang.String.format;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import java.util.concurrent.Callable;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;

/**
 * A {@link Callable} request that will execute
 * {@code POST /pool/<instance>/serviceState} against a cloud pool.
 */
public class SetServiceStateRequest extends CloudPoolRequest<Void> {

	/** Machine whose service state is to be set. */
	private final String machineId;
	/** Service state to set. */
	private final ServiceState serviceState;

	public SetServiceStateRequest(PrioritizedCloudPool cloudPool,
			String machineId, ServiceState serviceState) {
		super(cloudPool);
		this.machineId = machineId;
		this.serviceState = serviceState;
	}

	@Override
	public Void execute(AuthenticatedHttpClient client) throws Exception {
		String path = String.format("/pool/%s/serviceState", this.machineId);
		HttpPost request = new HttpPost(url(path));
		String message = format("{ \"serviceState\": \"%s\" }",
				this.serviceState.name());
		request.setEntity(new StringEntity(message, APPLICATION_JSON));
		client.execute(request);

		return null;
	}

}
