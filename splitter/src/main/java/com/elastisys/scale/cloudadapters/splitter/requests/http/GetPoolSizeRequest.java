package com.elastisys.scale.cloudadapters.splitter.requests.http;

import java.util.concurrent.Callable;

import org.apache.http.client.methods.HttpGet;

import com.elastisys.scale.cloudadapers.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudadapters.splitter.config.PrioritizedCloudAdapter;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.http.HttpRequestResponse;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;

/**
 * A {@link Callable} request that will execute {@code GET /pool/size} against a
 * cloud adapter.
 */
public class GetPoolSizeRequest extends CloudAdapterRequest<PoolSizeSummary> {

	public GetPoolSizeRequest(PrioritizedCloudAdapter cloudAdapter) {
		super(cloudAdapter);
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