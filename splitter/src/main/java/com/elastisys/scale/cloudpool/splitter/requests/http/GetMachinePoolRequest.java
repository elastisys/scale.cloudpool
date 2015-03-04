package com.elastisys.scale.cloudpool.splitter.requests.http;

import java.util.concurrent.Callable;

import org.apache.http.client.methods.HttpGet;

import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.http.HttpRequestResponse;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;

/**
 * A {@link Callable} request that will execute {@code GET /pool} against a
 * cloud pool.
 */
public class GetMachinePoolRequest extends CloudPoolRequest<MachinePool> {

	public GetMachinePoolRequest(PrioritizedCloudPool cloudPool) {
		super(cloudPool);
	}

	@Override
	public MachinePool execute(AuthenticatedHttpClient client) throws Exception {
		HttpRequestResponse response = client
				.execute(new HttpGet(url("/pool")));
		return JsonUtils.toObject(
				JsonUtils.parseJsonString(response.getResponseBody()),
				MachinePool.class);
	}
}