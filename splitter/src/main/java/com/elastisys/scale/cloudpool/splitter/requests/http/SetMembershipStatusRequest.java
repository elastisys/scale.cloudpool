package com.elastisys.scale.cloudpool.splitter.requests.http;

import static java.lang.String.format;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import java.util.concurrent.Callable;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;

/**
 * A {@link Callable} request that will execute
 * {@code POST /pool/<instance>/membershipStatus} against a cloud pool.
 */
public class SetMembershipStatusRequest extends CloudPoolRequest<Void> {

	/** Machine whose service state is to be set. */
	private final String machineId;
	/** Membership status to set. */
	private final MembershipStatus membershipStatus;

	public SetMembershipStatusRequest(PrioritizedCloudPool cloudPool,
			String machineId, MembershipStatus membershipStatus) {
		super(cloudPool);
		this.machineId = machineId;
		this.membershipStatus = membershipStatus;
	}

	@Override
	public Void execute(AuthenticatedHttpClient client) throws Exception {
		String path = String
				.format("/pool/%s/membershipStatus", this.machineId);
		HttpPost request = new HttpPost(url(path));
		String message = format("{ \"membershipStatus\": %s }",
				JsonUtils.toString(JsonUtils.toJson(this.membershipStatus)));
		request.setEntity(new StringEntity(message, APPLICATION_JSON));
		client.execute(request);

		return null;
	}

}
