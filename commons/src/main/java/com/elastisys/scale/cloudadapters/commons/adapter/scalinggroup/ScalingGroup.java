package com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup;

import java.util.List;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.NotFoundException;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapers.api.types.ServiceState;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapter;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScalingGroupConfig;

/**
 * Implements scaling group management primitives for a particular cloud
 * provider. A {@link ScalingGroup} is used by a {@link BaseCloudAdapter} as a
 * cloud provider-specific adapter capable of identifying group members, adding
 * machines to the group, removing machines from the group and recording
 * reported service state for members -- all according to the API offered by the
 * cloud provider.
 * <p/>
 * Note that the {@code configure} method must be invoked prior to executing any
 * other methods. Implementations should throw {@link IllegalStateException}s
 * whenever a method is accessed before a configuration has been set.
 * <p/>
 * Implementors should take care to ensure that implementations are thread-safe,
 * since they may be called by several concurrent threads.
 *
 * @see BaseCloudAdapter
 */
public interface ScalingGroup {

	/**
	 * Configures this {@link ScalingGroup} to start managing a group of
	 * servers. This method is called by the {@link BaseCloudAdapter} whenever a
	 * new configuration has been set, and includes the full configuration of
	 * the {@link BaseCloudAdapter}.
	 * <p/>
	 * The parts of the configuration that are of special interest to the
	 * {@link ScalingGroup}, such as cloud login details and scaling group name,
	 * are located under the {@link BaseCloudAdapterConfig#getScalingGroup()}
	 * and the {@link ScalingGroupConfig#getConfig()} and
	 * {@link ScalingGroupConfig#getName()} keys, respectively.
	 * <p/>
	 * The expected appearance of the {@link ScalingGroupConfig#getConfig()}
	 * configuration document depends on the {@link ScalingGroup} implementation
	 * but, at a minimum, should include details about how the
	 * {@link ScalingGroup} connects to its cloud provider. Before setting the
	 * configuration, the {@link ScalingGroup} should validate that all
	 * mandatory configuration keys have been provided. If this is not the case,
	 * a {@link ScalingGroupException} should be raised.
	 * <p/>
	 * The {@link ScalingGroup} must be configured before any other methods are
	 * invoked.
	 *
	 * @param configuration
	 *            The full configuration for the {@link BaseCloudAdapter}.
	 * @throws CloudAdapterException
	 */
	public void configure(BaseCloudAdapterConfig configuration)
			throws ScalingGroupException;

	/**
	 * Returns a list of the members of the scaling group.
	 * <p/>
	 * Note, that the response may include machines in any {@link MachineState},
	 * even machines that are in the process of terminating. The only machines
	 * that are to be considered <i>active</i> members of the scaling group are
	 * machines in machine state {@link MachineState#PENDING} or
	 * {@link MachineState#RUNNING} that are <b>not</b> in service state
	 * {@link ServiceState#OUT_OF_SERVICE}.
	 * <p/>
	 * The service state should be set to {@link ServiceState#UNKNOWN} for all
	 * machine instances for which no service state has been reported (see
	 * {@link #setServiceState(String, ServiceState)}).
	 * <p/>
	 * The <i>effective size</i> of the scaling group should be interpreted as
	 * the number of allocated machines (in any of the non-terminal states:
	 * {@link MachineState#REQUESTED}, {@link MachineState#PENDING}, or
	 * {@link MachineState#RUNNING}) that have not been marked
	 * {@link ServiceState#OUT_OF_SERVICE}. See
	 * {@link Machine#isEffectiveMember()}.
	 *
	 * @return The current members of the scaling group.
	 *
	 * @throws ScalingGroupException
	 */
	public List<Machine> listMachines() throws ScalingGroupException;

	/**
	 * Requests that a number of new {@link Machine}s be started in this scaling
	 * group. On success, the complete list of {@link Machine}s that were
	 * started is returned. On failure, a {@link StartMachinesException} is
	 * thrown with the cause of the failure and indicating which {@link Machine}
	 * s were started (if any) before the request failed.
	 * <p/>
	 * Depending on the functionality offered by the cloud provider, it may not
	 * be possible for the machine request to be immediately fulfilled (for
	 * example, this is typically the case when placing an AWS spot instance, or
	 * if the cloud API operates in an asynchronous manner such as the AWS Auto
	 * Scaling API). In such cases, the {@link ScalingGroup} does not need to
	 * wait for the machines to be booted but can return pseudo/placeholder
	 * {@link Machine}s in {@link MachineState#REQUESTED} state.
	 * <p/>
	 * The {@link ScalingGroup} implementation should take measures to ensure
	 * that, once launched, started machines are recognized as members of the
	 * {@link ScalingGroup}, and returned by subsequent calls to
	 * {@link #listMachines(String)}. The specific mechanism to mark group
	 * members, which may depend on the features of the particular cloud API, is
	 * left to the implementation but could, for example, set a tag on started
	 * machines.
	 *
	 * @param count
	 *            The number of {@link Machine}s to start.
	 * @param scaleUpConfig
	 *            The details of how to provision the new machine. Note: this is
	 *            the {@code scaleUpConfig} part of the {@link BaseCloudAdapter}
	 *            configuration document.
	 * @return The {@link List} of launched {@link Machine}s.
	 * @throws StartMachinesException
	 *             If the request failed to complete. The exception includes
	 *             details on machines that were started (if any).
	 */
	public List<Machine> startMachines(int count, ScaleUpConfig scaleUpConfig)
			throws StartMachinesException;

	/**
	 * Terminates a {@link Machine} in the scaling group.
	 *
	 * @param machineId
	 *            The identifier of the {@link Machine}.
	 * @throws NotFoundException
	 *             If the machine is not a member of the group.
	 * @throws ScalingGroupException
	 *             If anything went wrong.
	 */
	public void terminateMachine(String machineId) throws NotFoundException,
			ScalingGroupException;

	/**
	 * Attaches an already running machine instance to the scaling group.
	 *
	 * @param machineId
	 *            The identifier of the machine to attach to the scaling group.
	 * @throws NotFoundException
	 *             If the machine does not exist.
	 * @throws ScalingGroupException
	 *             If the operation could not be completed.
	 */
	void attachMachine(String machineId) throws NotFoundException,
			ScalingGroupException;

	/**
	 * Removes a member from the scaling group without terminating it. The
	 * machine keeps running but is no longer considered a scaling group member
	 * and, therefore, needs to be managed independently.
	 *
	 * @param machineId
	 *            The identifier of the machine to detach from the scaling
	 *            group.
	 * @throws NotFoundException
	 *             If the machine is not a member of the scaling group.
	 * @throws ScalingGroupException
	 *             If the operation could not be completed.
	 */
	void detachMachine(String machineId) throws NotFoundException,
			ScalingGroupException;

	/**
	 * Sets the service state of a given scaling group member. Setting the
	 * service state has no side-effects, unless the service state is set to
	 * {@link ServiceState#OUT_OF_SERVICE}, in which case a replacement machine
	 * will be launched (since {@link ServiceState#OUT_OF_SERVICE} machines are
	 * not considered effective members of the scaling group). An out-of-service
	 * machine can later be taken back into service by another call to this
	 * method to re-set its service state.
	 * <p/>
	 * The specific mechanism to mark group members state, which may depend on
	 * the features offered by the particular cloud API, is left to the
	 * implementation but could, for example, make use of tags.
	 *
	 *
	 * @param machineId
	 *            The id of the machine whose service state is to be updated.
	 * @param serviceState
	 *            The {@link ServiceState} to assign to the machine.
	 * @throws NotFoundException
	 *             If the machine is not a member of the scaling group.
	 * @throws ScalingGroupException
	 *             If the operation could not be completed.
	 */
	public void setServiceState(String machineId, ServiceState serviceState)
			throws NotFoundException, ScalingGroupException;

	/**
	 * Returns the name of this scaling group.
	 *
	 * @return The name of the scaling group.
	 */
	public String getScalingGroupName();

}
