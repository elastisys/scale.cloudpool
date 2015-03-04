package com.elastisys.scale.cloudpool.splitter.requests.http;

import static java.lang.String.format;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import java.util.concurrent.Callable;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;

/**
 * A {@link Callable} request that will execute {@code POST /pool/size} against
 * a cloud pool.
 */
public class SetDesiredSizeRequest extends CloudPoolRequest<Void> {

	/** Desired size of cloud pool. */
	private final int desiredSize;

	public SetDesiredSizeRequest(PrioritizedCloudPool cloudPool, int desiredSize) {
		super(cloudPool);
		this.desiredSize = desiredSize;
	}

	@Override
	public Void execute(AuthenticatedHttpClient client) throws Exception {
		HttpPost request = new HttpPost(url("/pool/size"));
		String message = format("{ \"desiredSize\": %d }", this.desiredSize);
		request.setEntity(new StringEntity(message, APPLICATION_JSON));
		client.execute(request);

		return null;
	}

}
