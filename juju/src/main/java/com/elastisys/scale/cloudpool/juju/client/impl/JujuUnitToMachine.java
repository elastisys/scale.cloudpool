package com.elastisys.scale.cloudpool.juju.client.impl;

import java.util.function.Function;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.google.gson.JsonObject;

/**
 * Converts a Juju unit specification to a {@link Machine}.
 *
 * @author Elastisys AB <techteam@elastisys.com>
 *
 * @see JujuUnitStateToMachineState
 */
public class JujuUnitToMachine implements Function<JsonObject, Machine> {

	final public static String CLOUD_PROVIDER = "Juju";
	final public static String MACHINE_SIZE = "1";
	final public static String REGION = "juju";

	@Override
	public Machine apply(JsonObject unit) {
		// The id stored directly in the "unit" specifier
		final String id = unit.entrySet().iterator().next().getKey();

		final JsonObject workloadStatus = unit.getAsJsonObject("workload-status");
		final MachineState machineState = new JujuUnitStateToMachineState()
				.apply(workloadStatus.get("current").getAsString());

		final String publicIp = getWithDefault(unit, "public-address", null);
		final String privateIp = getWithDefault(unit, "private-address", null);

		return Machine.builder().id(id).machineState(machineState).cloudProvider(CLOUD_PROVIDER).region(REGION)
				.machineSize(MACHINE_SIZE).publicIp(publicIp).privateIp(privateIp).metadata(unit).build();
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
	private String getWithDefault(JsonObject object, String property, String defaultValue) {
		if (object.has(property)) {
			return object.get(property).getAsString();
		}
		return defaultValue;
	}

}
