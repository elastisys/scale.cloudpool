package com.elastisys.scale.cloudadapers.api;

import com.elastisys.scale.cloudadapers.api.restapi.PoolHandler;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapers.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudadapers.api.types.ServiceState;
import com.elastisys.scale.commons.json.schema.JsonValidator;
import com.google.common.base.Optional;
import com.google.gson.JsonObject;

/**
 * A {@link CloudAdapter} manages a <i>scaling group</i> (an elastic
 * {@link MachinePool} of {@link Machine} instances) for a particular cloud
 * provider, handling communication with the cloud provider according to the
 * protocol/API that the cloud provider supports.
 * <p/>
 * The primary tasks of a {@link CloudAdapter} are:
 * <ul>
 * <li>tracking the scaling group members and their states.</li>
 * <li>resizing the scaling group, by commissioning/decommissioning machine
 * instances so that the number of active machines in the pool matches the
 * desired pool size.</li>
 * </ul>
 * <p/>
 * A {@link CloudAdapter} instance is intended to be made network-accessible by
 * a {@link PoolHandler} REST endpoint, which exposes the {@link CloudAdapter}
 * through the <a
 * href="http://cloudadapterapi.readthedocs.org/en/latest/">elastisys:scale
 * cloud adapter REST API</a>).
 * <p/>
 * Implementors should take measures to ensure that implementations are
 * thread-safe, since they may be called by several concurrent threads.
 *
 * @see PoolHandler
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
	 * Returns a list of the members of the scaling group.
	 * <p/>
	 * Note, that the response may include machines in any {@link MachineState},
	 * even machines that are in the process of terminating.
	 * <p/>
	 * The only machines that are to be considered <i>active</i> members of the
	 * pool are machines in machine state {@link MachineState#PENDING} or
	 * {@link MachineState#RUNNING} that are <b>not</b> in service state
	 * {@link ServiceState#OUT_OF_SERVICE}.
	 * <p/>
	 * The service state should be set to UNKNOWN for all machine instances for
	 * which no service state has been reported (see
	 * {@link #setServiceState(String, ServiceState)}).
	 * <p/>
	 * The <i>effective size</i> of the machine pool should be interpreted as
	 * the number of allocated machines (in any of the non-terminal states:
	 * {@link MachineState#REQUESTED}, {@link MachineState#PENDING}, or
	 * {@link MachineState#RUNNING}) that have not been marked
	 * {@link ServiceState#OUT_OF_SERVICE}. See
	 * {@link Machine#isEffectiveMember()}.
	 *
	 * @return A list of scaling group members.
	 *
	 * @throws CloudAdapterException
	 *             If the operation could not be completed.
	 */
	MachinePool getMachinePool() throws CloudAdapterException;

	/**
	 * Returns the current size of the {@link MachinePool} -- both in terms of
	 * the desired size and the actual size (as these may differ at any time).
	 *
	 * @return The current {@link PoolSizeSummary}.
	 * @throws CloudAdapterException
	 *             If the operation could not be completed.
	 */
	PoolSizeSummary getPoolSize() throws CloudAdapterException;

	/**
	 * Sets the desired number of machines in the machine pool. This method is
	 * asynchronous and returns immediately after updating the desired size.
	 * There may be a delay before the changes take effect and are reflected in
	 * the machine pool.
	 * <p/>
	 * Note: the {@link CloudAdapter} implementation should take measures to
	 * ensure that requested machines are recognized as pool members. The
	 * specific mechanism to mark group members, which may depend on the
	 * features offered by the particular cloud API, is left to the
	 * implementation but could, for example, make use of tags.
	 *
	 * @param desiredSize
	 *            The desired number of machines in the pool.
	 *
	 * @throws IllegalArgumentException
	 *             If the desired size is illegal.
	 * @throws CloudAdapterException
	 *             If the operation could not be completed.
	 */
	void setDesiredSize(int desiredSize) throws IllegalArgumentException,
			CloudAdapterException;

	/**
	 * Terminates a particular machine pool member. The caller can control if a
	 * replacement machine is to be provisioned via the
	 * {@code decrementDesiredSize} parameter.
	 *
	 * @param machineId
	 *            The machine to terminate.
	 * @param decrementDesiredSize
	 *            If the desired pool size should be decremented ({@code true})
	 *            or left at its current size ({@code false}).
	 *
	 * @throws NotFoundException
	 *             If the specified machine is not a member of the pool.
	 * @throws CloudAdapterException
	 *             If the operation could not be completed.
	 */
	void terminateMachine(String machineId, boolean decrementDesiredSize)
			throws NotFoundException, CloudAdapterException;

	/**
	 * Sets the service state of a given machine pool member. Setting the
	 * service state has no side-effects, unless the service state is set to
	 * {@link ServiceState#OUT_OF_SERVICE}, in which case a replacement machine
	 * will be launched (since {@link ServiceState#OUT_OF_SERVICE} machines are
	 * not considered effective members of the pool). An out-of-service machine
	 * can later be taken back into service by another call to this method to
	 * re-set its service state.
	 *
	 * @param machineId
	 *            The id of the machine whose service state is to be updated.
	 * @param serviceState
	 *            The {@link ServiceState} to assign to the machine.
	 * @throws NotFoundException
	 *             If the specified machine is not a member of the pool.
	 * @throws CloudAdapterException
	 *             If the operation could not be completed.
	 */
	void setServiceState(String machineId, ServiceState serviceState)
			throws NotFoundException, CloudAdapterException;

	/**
	 * Attaches an already running machine instance to the pool, growing the
	 * pool with a new member. This operation implies that the desired size of
	 * the group is incremented by one.
	 *
	 * @param machineId
	 *            The identifier of the machine to attach to the pool.
	 * @throws NotFoundException
	 *             If the specified machine does not exist.
	 * @throws CloudAdapterException
	 *             If the operation could not be completed.
	 */
	void attachMachine(String machineId) throws NotFoundException,
			CloudAdapterException;

	/**
	 * Removes a member from the pool without terminating it. The machine keeps
	 * running but is no longer considered a pool member and, therefore, needs
	 * to be managed independently. The caller can control if a replacement
	 * machine is to be provisioned via the {@code decrementDesiredSize}
	 * parameter.
	 *
	 * @param machineId
	 *            The identifier of the machine to detach from the pool.
	 * @param decrementDesiredSize
	 *            If the desired pool size should be decremented ({@code true})
	 *            or left at its current size ({@code false}).
	 * @throws NotFoundException
	 *             If the specified machine is not a member of the pool.
	 * @throws CloudAdapterException
	 *             If the operation could not be completed.
	 */
	void detachMachine(String machineId, boolean decrementDesiredSize)
			throws NotFoundException, CloudAdapterException;

}
