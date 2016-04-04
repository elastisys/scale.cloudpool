package com.elastisys.scale.cloudpool.kubernetes.client.impl;

import java.util.function.Function;

import org.joda.time.DateTime;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

/**
 * A {@link Function} that takes a pod status JSON document (one
 * <code>item</code> of the output from
 * <code>kubectl get pods --selector="app=nginx" --output=json</code>) and
 * converts it to a {@link Machine} instance.
 *
 */
public class PodToMachine implements Function<JsonObject, Machine> {

	/**
	 * Takes a pod status JSON document (one <code>item</code> of the output
	 * from <code>kubectl get pods --selector="app=nginx" --output=json</code>)
	 * and converts it to a {@link Machine} instance.
	 */
	@Override
	public Machine apply(JsonObject pod) {
		JsonObject status = pod.get("status").getAsJsonObject();
		JsonObject metadata = pod.get("metadata").getAsJsonObject();

		String id = metadata.get("name").getAsString();
		MachineState machineState = new PodStateToMachineState()
				.apply(status.get("phase").getAsString());
		String cloudProvider = "Kubernetes";
		String region = "N/A";
		String machineSize = "N/A";
		DateTime requestTime = UtcTime
				.parse(metadata.get("creationTimestamp").getAsString());

		// if pod isn't running, no startTime will be available
		String launchTimeValue = getWithDefault(status, "startTime", null);
		DateTime launchTime = launchTimeValue != null
				? UtcTime.parse(launchTimeValue) : null;
		// if pod isn't running, no IPs will be available
		String publicIp = getWithDefault(status, "hostIP", null);
		String privateIp = getWithDefault(status, "podIP", null);

		return Machine.builder().id(id).machineState(machineState)
				.cloudProvider(cloudProvider).region(region)
				.machineSize(machineSize).launchTime(launchTime)
				.requestTime(requestTime).publicIp(publicIp)
				.privateIp(privateIp).metadata(pod).build();
	}

	/**
	 * Returns a given property field from a JSON object or returns a default
	 * value in case the property is missing.
	 *
	 * @param object
	 * @param property
	 * @param defaultValue
	 * @return
	 */
	private String getWithDefault(JsonObject object, String property,
			String defaultValue) {
		if (object.has(property)) {
			return object.get(property).getAsString();
		}
		return defaultValue;
	}

}
