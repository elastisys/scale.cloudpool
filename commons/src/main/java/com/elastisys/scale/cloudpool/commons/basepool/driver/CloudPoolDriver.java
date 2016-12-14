package com.elastisys.scale.cloudpool.commons.basepool.driver;

import java.util.List;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.CloudPoolMetadata;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPool;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.CloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;

/**
 * A cloud pool management interface for a particular cloud provider.
 * <p/>
 * A {@link CloudPoolDriver} is used by a {@link BaseCloudPool} to provide a set
 * of cloud pool management primitives. This allows the {@link BaseCloudPool} to
 * be cloud-neutral, with all cloud-specific interactions handled by the
 * {@link CloudPoolDriver}.
 * <p/>
 * Implementing classes should use whatever API capabilities are offered by the
 * targeted cloud provider to implement primitives for identifying pool members,
 * adding machines to the pool, removing machines from the pool and recording
 * reported service state for members.
 * <p/>
 * Note that the {@code configure} method must be invoked prior to executing any
 * other methods. Implementations should throw {@link IllegalStateException}s
 * whenever a method is accessed before a configuration has been set.
 * <p/>
 * Implementors should take care to ensure that implementations are thread-safe,
 * since they may be called by several concurrent threads.
 *
 * @see BaseCloudPool
 */
public interface CloudPoolDriver {

    /**
     * Configures this {@link CloudPoolDriver} to start managing a pool of
     * servers. This method is called by the {@link BaseCloudPool} whenever a
     * new configuration has been set, and includes the full configuration of
     * the {@link BaseCloudPool}.
     * <p/>
     * The parts of the configuration that are of special interest to the
     * {@link CloudPoolDriver}, such as cloud login details and cloud pool name,
     * are located under the {@link BaseCloudPoolConfig#getCloudPool()} and the
     * {@link CloudPoolConfig#getDriverConfig()} and
     * {@link CloudPoolConfig#getName()} keys, respectively.
     * <p/>
     * The expected appearance of the {@link CloudPoolConfig#getDriverConfig()}
     * configuration document depends on the {@link CloudPoolDriver}
     * implementation but, at a minimum, should include details about how the
     * {@link CloudPoolDriver} connects to its cloud provider. Before setting
     * the configuration, the {@link CloudPoolDriver} should validate that all
     * mandatory configuration keys have been provided. If this is not the case,
     * a {@link CloudPoolDriverException} should be raised.
     * <p/>
     * The {@link CloudPoolDriver} must be configured before any other methods
     * are invoked.
     *
     * @param configuration
     *            The full configuration for the {@link BaseCloudPool}.
     * @throws IllegalArgumentException
     *             If the received configuration was invalid.
     * @throws CloudPoolDriverException
     *             If the configuration could not be applied.
     */
    public void configure(BaseCloudPoolConfig configuration) throws IllegalArgumentException, CloudPoolDriverException;

    /**
     * Returns a list of the members of the cloud pool.
     * <p/>
     * Note, that the response may include machines in any {@link MachineState},
     * even machines that are in the process of terminating.
     * <p/>
     * The {@link MembershipStatus} of a machine in an allocated/started machine
     * state determines if it is to be considered an active member of the
     * pool.The <i>active size</i> of the machine pool should be interpreted as
     * the number of allocated machines (in any of the non-terminal machine
     * states {@code REQUESTED}, {@code PENDING} or {@code RUNNING} that have
     * not been marked with an inactive {@link MembershipStatus}. See
     * {@link Machine#isActiveMember()}.
     * <p/>
     * The service state should be set to UNKNOWN for all machine instances for
     * which no service state has been reported (see
     * {@link #setServiceState(String, ServiceState)}).
     *
     * @return The current members of the cloud pool.
     *
     * @throws CloudPoolDriverException
     *             If the operation could not be completed.
     */
    public List<Machine> listMachines() throws CloudPoolDriverException;

    /**
     * Requests that a number of new {@link Machine}s be started in this cloud
     * pool. On success, the complete list of {@link Machine}s that were started
     * is returned. On failure, a {@link StartMachinesException} is thrown with
     * the cause of the failure and indicating which {@link Machine} s were
     * started (if any) before the request failed.
     * <p/>
     * Depending on the functionality offered by the cloud provider, it may not
     * be possible for the machine request to be immediately fulfilled (for
     * example, this is typically the case when placing an AWS spot instance, or
     * if the cloud API operates in an asynchronous manner such as the AWS Auto
     * Scaling API). In such cases, the {@link CloudPoolDriver} does not need to
     * wait for the machines to be booted but can return pseudo/placeholder
     * {@link Machine}s in {@link MachineState#REQUESTED} state.
     * <p/>
     * The {@link CloudPoolDriver} implementation should take measures to ensure
     * that, once launched, started machines are recognized as members of the
     * {@link CloudPoolDriver}, and returned by subsequent calls to
     * {@link #listMachines(String)}. The specific mechanism to mark pool
     * members, which may depend on the features of the particular cloud API, is
     * left to the implementation but could, for example, set a tag on started
     * machines.
     *
     * @param count
     *            The number of {@link Machine}s to start.
     * @param scaleOutConfig
     *            The details of how to provision the new machine. Note: this is
     *            the {@code scaleUpConfig} part of the {@link BaseCloudPool}
     *            configuration document.
     * @return The {@link List} of launched {@link Machine}s.
     * @throws StartMachinesException
     *             If the request failed to complete. The exception includes
     *             details on machines that were started (if any).
     */
    public List<Machine> startMachines(int count, ScaleOutConfig scaleOutConfig) throws StartMachinesException;

    /**
     * Terminates a {@link Machine} in the cloud pool.
     *
     * @param machineId
     *            The identifier of the {@link Machine}.
     * @throws NotFoundException
     *             If the machine is not a member of the pool.
     * @throws CloudPoolDriverException
     *             If anything went wrong.
     */
    public void terminateMachine(String machineId) throws NotFoundException, CloudPoolDriverException;

    /**
     * Attaches an already running machine instance to the cloud pool.
     *
     * @param machineId
     *            The identifier of the machine to attach to the cloud pool.
     * @throws NotFoundException
     *             If the machine does not exist.
     * @throws CloudPoolDriverException
     *             If the operation could not be completed.
     */
    void attachMachine(String machineId) throws NotFoundException, CloudPoolDriverException;

    /**
     * Removes a member from the cloud pool without terminating it. The machine
     * keeps running but is no longer considered a cloud pool member and,
     * therefore, needs to be managed independently.
     *
     * @param machineId
     *            The identifier of the machine to detach from the cloud pool.
     * @throws NotFoundException
     *             If the machine is not a member of the cloud pool.
     * @throws CloudPoolDriverException
     *             If the operation could not be completed.
     */
    void detachMachine(String machineId) throws NotFoundException, CloudPoolDriverException;

    /**
     * Sets the service state of a given machine pool member. Setting the
     * service state does not have any functional implications on the pool
     * member, but should be seen as way to supply operational information about
     * the service running on the machine to third-party services (such as load
     * balancers).
     * <p/>
     * The specific mechanism to mark pool members state, which may depend on
     * the features offered by the particular cloud API, is left to the
     * implementation but could, for example, make use of tags.
     *
     * @param machineId
     *            The id of the machine whose service state is to be updated.
     * @param serviceState
     *            The {@link ServiceState} to assign to the machine.
     * @throws NotFoundException
     *             If the machine is not a member of the cloud pool.
     * @throws CloudPoolDriverException
     *             If the operation could not be completed.
     */
    public void setServiceState(String machineId, ServiceState serviceState)
            throws NotFoundException, CloudPoolDriverException;

    /**
     * Sets the membership status of a given pool member.
     * <p/>
     * The membership status for a machine can be set to protect the machine
     * from being terminated (by setting its evictability status) and/or to mark
     * a machine as being in need of replacement by flagging it as an inactive
     * pool member.
     * <p/>
     * The specific mechanism to mark pool members' status, which may depend on
     * the features offered by the particular cloud API, is left to the
     * implementation but could, for example, make use of tags.
     *
     * @param machineId
     *            The id of the machine whose status is to be updated.
     * @param membershipStatus
     *            The {@link MembershipStatus} to set.
     * @throws NotFoundException
     *             If the machine is not a member of the cloud pool.
     * @throws CloudPoolDriverException
     *             If the operation could not be completed.
     */
    public void setMembershipStatus(String machineId, MembershipStatus membershipStatus)
            throws NotFoundException, CloudPoolDriverException;

    /**
     * Returns the logical name of the managed machine pool.
     *
     * @return The logical name of the managed machine pool.
     */
    public String getPoolName();

    /**
     * Returns description of static properties about the cloud pool itself and
     * the cloud it manages.
     *
     * @return Description of static properties about the cloud pool itself and
     *         the cloud it manages.
     */
    public CloudPoolMetadata getMetadata();

}
