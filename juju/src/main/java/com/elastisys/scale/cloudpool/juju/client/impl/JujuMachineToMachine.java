package com.elastisys.scale.cloudpool.juju.client.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Function;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.google.gson.JsonObject;

public class JujuMachineToMachine implements Function<JsonObject, Machine> {
	final public static String CLOUD_PROVIDER = "Juju";
	final public static String MACHINE_SIZE = "1";
	final public static String REGION = "juju";

	@Override
	public Machine apply(JsonObject machine) {
		// The id stored directly in the "unit" specifier
		final String id = machine.entrySet().iterator().next().getKey();

		final MachineState machineState = new JujuMachineStateToMachineState()
				.apply(machine.get("instance-state").getAsString());

		/*
		 * We try a DNS lookup, but it will likely fail if the hostname is set
		 * to something cluster-specific such as "juju-machine-0.novalocal" or
		 * similar. If the dnsName is already an IP address, the getByName
		 * function still succeeds.
		 */
		final String dnsName = machine.get("dns-name").getAsString();
		String publicIp;
		try {
			final String ipAddress = InetAddress.getByName(dnsName).getHostAddress();
			publicIp = ipAddress;
		} catch (UnknownHostException e) {
			publicIp = dnsName;
		}

		return Machine.builder().id(id).machineState(machineState).cloudProvider(CLOUD_PROVIDER).region(REGION)
				.machineSize(MACHINE_SIZE).publicIp(publicIp).metadata(machine).build();
	}

}
