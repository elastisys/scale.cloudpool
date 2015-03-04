package com.elastisys.scale.cloudpool.splitter.requests.http;

import java.util.concurrent.Callable;

import org.apache.http.client.methods.HttpGet;

import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.http.HttpRequestResponse;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;

/**
 * A {@link Callable} request that will execute {@code GET /pool/size} against a
 * cloud pool.
 */
public class GetPoolSizeRequest extends CloudPoolRequest<PoolSizeSummary> {

	public GetPoolSizeRequest(PrioritizedCloudPool cloudPool) {
		super(cloudPool);
	}

	@Override
	public PoolSizeSummary execute(AuthenticatedHttpClient client)
			throws Exception {
		HttpRequestResponse response = client.execute(new HttpGet(
				url("/pool/size")));
		return JsonUtils.toObject(
				JsonUtils.parseJsonString(response.getResponseBody()),
				PoolSizeSummary.class);
	}

}