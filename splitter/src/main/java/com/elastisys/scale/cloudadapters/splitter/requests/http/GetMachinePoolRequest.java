package com.elastisys.scale.cloudadapters.splitter.requests.http;

import java.util.concurrent.Callable;

import org.apache.http.client.methods.HttpGet;

import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapters.splitter.config.PrioritizedCloudAdapter;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.http.HttpRequestResponse;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;

/**
 * A {@link Callable} request that will execute {@code GET /pool} against a
 * cloud adapter.
 */
public class GetMachinePoolRequest extends CloudAdapterRequest<MachinePool> {

	public GetMachinePoolRequest(PrioritizedCloudAdapter cloudAdapter) {
		super(cloudAdapter);
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