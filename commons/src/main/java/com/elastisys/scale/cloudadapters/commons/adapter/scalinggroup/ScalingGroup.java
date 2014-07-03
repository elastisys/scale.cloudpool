package com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup;

import java.util.List;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.LivenessState;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapter;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScalingGroupConfig;

/**
 * 
 * Represents a management interface for a scaling group on a particular cloud.
 * The {@link ScalingGroup}, which takes care of all protocol/API-specifics of
 * the targeted cloud, provides primitives for identifying scaling group member
 * {@link Machine} instances, provisioning new machine instances, and
 * decommissioning machine instances.
 * <p/>
 * Note that the {@code configure} method must be invoked prior to executing any
 * other methods. Implementations should throw {@link IllegalStateException}s
 * whenever a method is accessed before a configuration has been set.
 * <p/>
 * Implementors should take care to ensure that implementations are thread-safe,
 * since they may be called by several concurrent threads.
 * 
 * @see BaseCloudAdapter
 * 
 * 
 * 
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
	 * Returns all known members of the scaling group.
	 * <p/>
	 * The returned list of {@link Machine}s may include both active group
	 * members (in state {@link MachineState#PENDING} or
	 * {@link MachineState#RUNNING}) as well as machines that have just been
	 * requested or members of the group that are shut down or are in the
	 * process of being shut down. The execution state of each machine is
	 * indicated by its {@link MachineState}.
	 * <p/>
	 * The effective size of the machine pool should be interpreted as the
	 * number of machines in a non-terminal state (see
	 * {@link Machine#isAllocated()}).
	 * <p/>
	 * For {@link Machine}s in an active machine state (see
	 * {@link Machine#isActive()}), the {@link ScalingGroup} may choose to
	 * include {@link LivenessState} to describe their operational status
	 * further.
	 * 
	 * @return The current members of the scaling group.
	 * 
	 * @throws ScalingGroupException
	 */
	public List<Machine> listMachines() throws ScalingGroupException;

	/**
	 * Starts a number of new {@link Machine}s in this scaling group. On
	 * success, the complete list of started {@link Machine}s is returned. On
	 * failure, a {@link StartMachinesException} is thrown with the cause of the
	 * failure and indicating which {@link Machine}s were started (if any)
	 * before the request failed.
	 * <p/>
	 * The {@link ScalingGroup} implementation should take measures to ensure
	 * that, once launched, started machines are recognized as members of the
	 * {@link ScalingGroup}, and returned by subsequent calls to
	 * {@link #listMachines(String)}. The specific mechanism to mark group
	 * members, which may depend on the features of the particular cloud API, is
	 * left to the implementation but could, for example, set a tag on started
	 * machines.
	 * <p/>
	 * Should errors occur after one or more {@link Machine}s have been brought
	 * into existence, the recommended approach is to leave the {@link Machine}s
	 * running in the pool for purposes of troubleshooting. There can be lots of
	 * reasons for the start-up failing (a bug in one of the software packages
	 * of the machine, a bug in the provisioning script, faulty launch
	 * configuration, etc), most of which cannot be well addressed in an
	 * automated manner. A human administrator is responsible for fixing the
	 * error, for example, by repairing the broken machine in-place or by
	 * terminating it and having the {@link CloudAdapter} replace it with a new
	 * machine.
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
	 * @throws ScalingGroupException
	 *             If anything went wrong.
	 */
	public void terminateMachine(String machineId) throws ScalingGroupException;

	/**
	 * Returns the name of this scaling group.
	 * 
	 * @return The name of the scaling group.
	 */
	public String getScalingGroupName();
}
