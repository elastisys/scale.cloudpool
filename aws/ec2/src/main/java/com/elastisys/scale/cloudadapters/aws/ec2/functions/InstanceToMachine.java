package com.elastisys.scale.cloudadapters.aws.ec2.functions;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.amazonaws.services.ec2.model.Instance;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

/**
 * {@link Function} that translates an EC2 API instance representation to its
 * corresponding {@link Machine} representation.
 *
 * 
 *
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
	 * Convert an {@link Instance} to a {@link Machine}.
	 *
	 * @see com.google.common.base.Function#apply(java.lang.Object)
	 */
	@Override
	public Machine apply(Instance instance) {
		Preconditions.checkArgument(instance != null, "received null instance");

		String id = instance.getInstanceId();
		MachineState state = new InstanceStateToMachineState().apply(instance
				.getState());
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

		JsonObject metadata = JsonUtils.toJson(instance).getAsJsonObject();
		return new Machine(id, state, launchtime, publicIps, privateIps,
				metadata);
	}
}
