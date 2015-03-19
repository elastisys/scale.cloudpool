package com.elastisys.scale.cloudpool.aws.spot.util;

import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;

public class SpotTestUtil {
	/**
	 * Create a {@link SpotInstanceRequest} that may be associated with an
	 * {@link Instance}.
	 *
	 * @param id
	 * @param state
	 * @param instanceId
	 * @return
	 */
	public static SpotInstanceRequest spotRequest(String id, String state,
			String instanceId, Tag... tags) {
		return new SpotInstanceRequest().withSpotInstanceRequestId(id)
				.withState(state).withInstanceId(instanceId).withTags(tags);
	}

	/**
	 * Create an {@link Instance} that may be associated with a
	 * {@link SpotInstanceRequest}.
	 *
	 * @param id
	 * @param state
	 * @param spotRequestId
	 * @return
	 */
	public static Instance instance(String id, InstanceStateName state,
			String spotRequestId) {
		return new Instance().withInstanceId(id)
				.withState(new InstanceState().withName(state.toString()))
				.withSpotInstanceRequestId(spotRequestId);
	}

	public static List<String> list(String... values) {
		return Arrays.asList(values);
	}

}
