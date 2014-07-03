package com.elastisys.scale.cloudadapers.api;

import com.elastisys.scale.cloudadapers.api.restapi.PoolHandler;
import com.elastisys.scale.cloudadapers.api.types.LivenessState;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.commons.json.schema.JsonValidator;
import com.google.common.base.Optional;
import com.google.gson.JsonObject;

/**
 * A {@link CloudAdapter} manages a <i>scaling group</i> (an elastic
 * {@link MachinePool} of {@link Machine} instances) for a particular cloud
 * provider, handling communication with the cloud provider according to the
 * protocol/API that the cloud provider supports.
 * <p/>
 * A {@link CloudAdapter} performs the following tasks:
 * <ul>
 * <li>It provides an up-to-date view of the scaling group members.</li>
 * <li>It resizes the scaling group, by commissioning/decommissioning machine
 * instances so that the number of active machines in the pool matches the
 * desired pool size.</li>
 * </ul>
 * <p/>
 * A {@link CloudAdapter} instance is supposed to be made network-accessible by
 * a {@link PoolHandler} REST endpoint, which exposes the {@link CloudAdapter}
 * through the <a
 * href="http://cloudadapterapi.readthedocs.org/en/latest/">elastisys:scale
 * cloud adapter REST API</a>).
 * <p/>
 * Implementors should take measures to ensure that implementations are
 * thread-safe, since they may be called by several concurrent threads.
 * 
 * @see PoolHandler
 * 
 * 
 * 
 */
public interface CloudAdapter {

	/**
	 * Returns a JSON Schema that describes the structure of valid JSON
	 * configuration documents for this {@link CloudAdapter}.
	 * <p/>
	 * In case this {@link CloudAdapter} doesn't publish a JSON Schema,
	 * {@link Optional#absent()} is returned.
	 * 
	 * @return A JSON Schema if one is supplied by this {@link CloudAdapter},
	 *         {@link Optional#absent()} otherwise.
	 */
	Optional<JsonObject> getConfigurationSchema();

	/**
	 * Updates the configuration for this {@link CloudAdapter}.
	 * <p/>
	 * The configuration is passed as a JSON object. It is up to the
	 * {@link CloudAdapter} to validate and apply the contents of the
	 * configuration as well as to reload internal data structures, restart
	 * periodical tasks or perform whatever changes are needed for the new
	 * configuration to take effect.
	 * <p/>
	 * In case the {@link CloudAdapter} publishes a JSON Schema (see
	 * {@link #getConfigurationSchema()}), it should make sure that the received
	 * configuration is a valid instance of the JSON Schema.
	 * {@link JsonValidator} can be used for this purpose.
	 * 
	 * @see JsonValidator
	 * 
	 * @param configuration
	 *            The JSON configuration to be set.
	 * @throws IllegalArgumentException
	 *             If the received configuration was invalid.
	 * @throws CloudAdapterException
	 *             If the configuration could not be applied.
	 */
	void configure(JsonObject configuration) throws IllegalArgumentException,
			CloudAdapterException;

	/**
	 * Returns the configuration currently set for this {@link CloudAdapter}, if
	 * one has been set.
	 * 
	 * @return A JSON configuration if set, {@link Optional#absent()} otherwise.
	 */
	Optional<JsonObject> getConfiguration();

	/**
	 * Returns all known members of the scaling group.
	 * <p/>
	 * The returned {@link MachinePool} may include both active group members
	 * (in state {@link MachineState#PENDING} or {@link MachineState#RUNNING})
	 * as well as machines that have just been requested or members of the group
	 * that are shut down or are in the process of being shut down. The
	 * execution state of each machine is indicated by its {@link MachineState}.
	 * <p/>
	 * The effective size of the machine pool should be interpreted as the
	 * number of <i>allocated</i> machines, being the machines in any of the
	 * non-terminal states: {@link MachineState#REQUESTED},
	 * {@link MachineState#PENDING}, or {@link MachineState#RUNNING} (see
	 * {@link Machine#isAllocated()}).
	 * <p/>
	 * For {@link Machine}s in an active machine state (see
	 * {@link Machine#isActive()}), the {@link CloudAdapter} may choose to
	 * include {@link LivenessState} to describe their operational status
	 * further.
	 * 
	 * @return The current members of the scaling group.
	 * 
	 * @throws CloudAdapterException
	 *             If there was a problem retrieving the {@link MachinePool}
	 *             from the cloud provider.
	 */
	MachinePool getMachinePool() throws CloudAdapterException;

	/**
	 * Sets the desired number of machines in the machine pool.
	 * <p/>
	 * If the resize operation increases the size of the scaling group, the
	 * {@link CloudAdapter} should take measures to ensure that requested
	 * machines are recognized as members of the scaling group and returned by
	 * subsequent calls to {@link #getMachinePool()}. The specific mechanism to
	 * mark group members, which may depend on the features offered by the
	 * particular cloud API, is left to the implementation but could, for
	 * example, make use of tags.
	 * <p/>
	 * Should the boot-up of a machine fail (after it has been launched), the
	 * method should raise an exception, but the {@link Machine} should be kept
	 * in the group for purposes of troubleshooting. There are lots of reasons
	 * for the start-up failing (a bug in one of the software packages of the
	 * machine, a bug in a provisioning script, faulty launch configuration,
	 * etc), most of which are difficult to address in an automated manner. A
	 * sensible approach is to notify a human administrator, who can fix the
	 * error, for example, by repairing the broken machine in-place or by
	 * terminating it and having the {@link CloudAdapter} replace it with a new
	 * machine.
	 * 
	 * @param desiredSize
	 *            The desired number of machines in the machine pool.
	 * @throws IllegalArgumentException
	 *             If the requested capacity is invalid.
	 * @throws CloudAdapterException
	 *             If there was a problem in requesting the desired capacity
	 *             from the cloud provider.
	 */
	void resizeMachinePool(int desiredSize) throws IllegalArgumentException,
			CloudAdapterException;
}
