package com.elastisys.scale.cloudpool.aws.spot.functions;

import static com.google.common.base.Preconditions.checkArgument;
import static org.joda.time.DateTimeZone.UTC;

import java.util.List;

import org.joda.time.DateTime;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.commons.ScalingTags;
import com.elastisys.scale.cloudpool.aws.spot.metadata.InstancePairedSpotRequest;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

/**
 * {@link Function} that converts an {@link InstancePairedSpotRequest} to its
 * {@link Machine} representation.
 */
public class InstancePairedSpotRequestToMachine implements
Function<InstancePairedSpotRequest, Machine> {

	/**
	 * Converts an {@link InstancePairedSpotRequest} to a {@link Machine}.
	 *
	 * @param instancePairedSpotRequest
	 * @return
	 */
	public static Machine convert(
			InstancePairedSpotRequest instancePairedSpotRequest) {
		return new InstancePairedSpotRequestToMachine()
		.apply(instancePairedSpotRequest);
	}

	@Override
	public Machine apply(InstancePairedSpotRequest spotInstanceRequest) {
		checkArgument(spotInstanceRequest != null,
				"received null spot instance request");

		SpotInstanceRequest request = spotInstanceRequest.getRequest();

		String id = request.getSpotInstanceRequestId();
		MachineState machineState = spotInstanceRequest.getMachineState();
		final DateTime requesttime = new DateTime(spotInstanceRequest
				.getRequest().getCreateTime(), UTC);
		DateTime launchtime = null;
		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();
		if (spotInstanceRequest.hasInstance()) {
			Instance instance = spotInstanceRequest.getInstance();
			launchtime = new DateTime(instance.getLaunchTime(), UTC);
			if (instance.getPublicIpAddress() != null) {
				publicIps.add(instance.getPublicIpAddress());
			}
			if (instance.getPrivateIpAddress() != null) {
				privateIps.add(instance.getPrivateIpAddress());
			}
		}

		// extract membership status if status tag is present on spot request
		MembershipStatus membershipStatus = MembershipStatus.defaultStatus();
		Optional<String> membershipStatusTag = getTagValue(request,
				ScalingTags.MEMBERSHIP_STATUS_TAG);
		if (membershipStatusTag.isPresent()) {
			membershipStatus = JsonUtils.toObject(
					JsonUtils.parseJsonString(membershipStatusTag.get()),
					MembershipStatus.class);
		}

		// set the service state if a service state tag is present on spot
		// request
		ServiceState serviceState = ServiceState.UNKNOWN;
		Optional<String> serviceStateTag = getTagValue(request,
				ScalingTags.SERVICE_STATE_TAG);
		if (serviceStateTag.isPresent()) {
			serviceState = ServiceState.valueOf(serviceStateTag.get());
		}

		JsonObject metadata = JsonUtils.toJson(spotInstanceRequest)
				.getAsJsonObject();
		return new Machine(id, machineState, membershipStatus, serviceState,
				requesttime, launchtime, publicIps, privateIps, metadata);
	}

	/**
	 * Retrieves a certain tag value from an {@link SpotInstanceRequest}.
	 *
	 * @param spotRequest
	 * @param tagKey
	 * @return
	 */
	private Optional<String> getTagValue(SpotInstanceRequest spotRequest,
			String tagKey) {
		List<Tag> tags = spotRequest.getTags();
		for (Tag tag : tags) {
			if (tag.getKey().equals(tagKey)) {
				return Optional.of(tag.getValue());
			}
		}
		return Optional.absent();
	}
}
