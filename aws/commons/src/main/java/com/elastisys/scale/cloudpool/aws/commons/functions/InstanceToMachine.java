package com.elastisys.scale.cloudpool.aws.commons.functions;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.commons.ScalingTags;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

/**
 * {@link Function} that translates an EC2 API {@link Instance} representation
 * to its corresponding {@link Machine} representation.
 */
public class InstanceToMachine implements Function<Instance, Machine> {

	/**
	 * Converts a {@link Instance} to its {@link Machine} representation.
	 *
	 * @param instance
	 * @return
	 */
	public static Machine convert(Instance instance) {
		return new InstanceToMachine().apply(instance);
	}

	/**
	 * Converts an {@link Instance} to a {@link Machine}.
	 *
	 * @see com.google.common.base.Function#apply(java.lang.Object)
	 */
	@Override
	public Machine apply(Instance instance) {
		Preconditions.checkArgument(instance != null, "received null instance");

		String id = instance.getInstanceId();
		MachineState machineState = new InstanceStateToMachineState()
				.apply(instance.getState());
		DateTime launchtime = new DateTime(instance.getLaunchTime(),
				DateTimeZone.UTC);
		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();
		String publicIp = instance.getPublicIpAddress();
		if (publicIp != null) {
			publicIps.add(publicIp);
		}
		String privateIp = instance.getPrivateIpAddress();
		if (privateIp != null) {
			privateIps.add(privateIp);
		}

		// extract membership status if status tag is present on instance
		MembershipStatus membershipStatus = MembershipStatus.defaultStatus();
		Optional<String> membershipStatusTag = getTagValue(instance,
				ScalingTags.MEMBERSHIP_STATUS_TAG);
		if (membershipStatusTag.isPresent()) {
			membershipStatus = JsonUtils.toObject(
					JsonUtils.parseJsonString(membershipStatusTag.get()),
					MembershipStatus.class);
		}

		// set the service state if a service state tag is present on instance
		ServiceState serviceState = ServiceState.UNKNOWN;
		Optional<String> serviceStateTag = getTagValue(instance,
				ScalingTags.SERVICE_STATE_TAG);
		if (serviceStateTag.isPresent()) {
			serviceState = ServiceState.valueOf(serviceStateTag.get());
		}

		JsonObject metadata = JsonUtils.toJson(instance).getAsJsonObject();
		return new Machine(id, machineState, membershipStatus, serviceState,
				launchtime, publicIps, privateIps, metadata);
	}

	/**
	 * Retrieves a certain tag value from an {@link Instance}.
	 *
	 * @param instance
	 * @param tagKey
	 * @return
	 */
	private Optional<String> getTagValue(Instance instance, String tagKey) {
		List<Tag> tags = instance.getTags();
		for (Tag tag : tags) {
			if (tag.getKey().equals(tagKey)) {
				return Optional.of(tag.getValue());
			}
		}
		return Optional.absent();
	}
}
