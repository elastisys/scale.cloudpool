package com.elastisys.scale.cloudadapters.splitter.requests.http;

import java.util.concurrent.Callable;

import org.apache.http.client.methods.HttpPost;

import com.elastisys.scale.cloudadapters.splitter.config.PrioritizedCloudAdapter;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;

/**
 * A {@link Callable} request that will execute
 * {@code POST /pool/<instance>/attach} against a cloud adapter.
 */
public class AttachMachineRequest extends CloudAdapterRequest<Void> {
	/** Machine to request cloud adapter to attach. */
	private final String machineId;

	public AttachMachineRequest(PrioritizedCloudAdapter cloudAdapter,
			String machineId) {
		super(cloudAdapter);
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
