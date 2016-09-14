package com.elastisys.scale.cloudpool.api;

import com.elastisys.scale.cloudpool.api.restapi.CloudPoolHandler;
import com.elastisys.scale.cloudpool.api.types.CloudPoolMetadata;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.google.common.base.Optional;
import com.google.gson.JsonObject;

/**
 * A {@link CloudPool} is a management interface towards an elastic pool of
 * machines for a particular cloud provider, handling communication with the
 * cloud provider according to its API. The {@link CloudPool} provides a
 * cloud-neutral API to clients, such as the autoscaler, with a number of
 * management primitives for the machine pool. In general terms, these
 * primitives allow clients to:
 * <ul>
 * <li>track the machine pool members and their states</li>
 * <li>modify the size of the machine pool (the cloud pool continuously
 * starts/stops machine instances so that the number of machines in the pool
 * matches the desired size set for the pool)</li>
 * </ul>
 * <p/>
 * A {@link CloudPool} instance is intended to be made network-accessible by a
 * {@link CloudPoolHandler} REST endpoint, which exposes the {@link CloudPool}
 * through the
 * <a href="http://cloudpoolrestapi.readthedocs.org/en/latest/">elastisys:scale
 * cloud pool REST API</a>).
 * <p/>
 * Implementors should take measures to ensure that implementations are
 * thread-safe, since they may be called by several concurrent threads.
 *
 * @see CloudPoolHandler
 */
public interface CloudPool {

    /**
     * Updates the configuration for this {@link CloudPool}.
     * <p/>
     * This operation does not change the {@link CloudPool}'s started state --
     * if the {@link CloudPool} is started it should remain started, and if it
     * is in a stopped state it should remain stopped.
     * <p/>
     * The configuration is passed as a JSON object. It is up to the
     * {@link CloudPool} to validate and apply the contents of the configuration
     * as well as to perform whatever changes are needed for the new
     * configuration to take effect.
     *
     * @param configuration
     *            The JSON configuration to be set.
     * @throws IllegalArgumentException
     *             If the received configuration was invalid.
     * @throws CloudPoolException
     *             If the configuration could not be applied.
     */
    void configure(JsonObject configuration) throws IllegalArgumentException, CloudPoolException;

    /**
     * Returns the configuration currently set for this {@link CloudPool}, if
     * one has been set.
     *
     * @return A JSON configuration if set, {@link Optional#absent()} otherwise.
     */
    Optional<JsonObject> getConfiguration();

    /**
     * Starts the {@link CloudPool}.
     * <p/>
     * This will set the {@link CloudPool} in an activated state where it will
     * start to accept requests to query or modify the machine pool.
     * <p/>
     * If the {@link CloudPool} has not been configured the method will fail. If
     * the {@link CloudPool} is already started this is a no-op.
     *
     * @throws NotConfiguredException
     *             If the {@link CloudPool} has not been configured.
     */
    void start() throws NotConfiguredException;

    /**
     * Stops the {@link CloudPool}.
     * <p/>
     * A stopped {@link CloudPool} is in a passivated state and will not accept
     * any requests to query or modify the machine pool.
     * <p/>
     * If the {@link CloudPool} is already in a stopped state this is a no-op.
     */
    void stop();

    /**
     * Returns the execution status for the {@link CloudPool}.
     *
     * @return
     */
    CloudPoolStatus getStatus();

    /**
     * Returns a list of the members of the cloud pool.
     * <p/>
     * Note, that the response may include machines in any {@link MachineState},
     * even machines that are in the process of terminating.
     * <p/>
     * The {@link MembershipStatus} of a machine in an allocated/started state
     * determines if it is to be considered an active member of the pool.The
     * <i>active size</i> of the machine pool should be interpreted as the
     * number of allocated machines (in any of the non-terminal machine states
     * {@code REQUESTED}, {@code PENDING} or {@code RUNNING} that have not been
     * marked with an inactive {@link MembershipStatus}. See
     * {@link Machine#isActiveMember()}.
     * <p/>
     * The service state should be set to UNKNOWN for all machine instances for
     * which no service state has been reported (see
     * {@link #setServiceState(String, ServiceState)}).
     * <p/>
     * Similarly, the {@link MembershipStatus} should be set to
     * {@link MembershipStatus#defaultStatus()} for all machine instances for
     * which no membership status has been reported (see
     * {@link #setMembershipStatus(String, MembershipStatus)}).
     *
     * @return A list of cloud pool members.
     *
     * @throws CloudPoolException
     *             If the operation could not be completed.
     * @throws NotStartedException
     *             If the {@link CloudPool} is not started.
     */
    MachinePool getMachinePool() throws CloudPoolException, NotStartedException;

    /**
     * Returns the current size of the {@link MachinePool} -- both in terms of
     * the desired size and the actual size (as these may differ at any time).
     *
     * @return The current {@link PoolSizeSummary}.
     * @throws CloudPoolException
     *             If the operation could not be completed.
     * @throws NotStartedException
     *             If the {@link CloudPool} is not started.
     */
    PoolSizeSummary getPoolSize() throws CloudPoolException, NotStartedException;

    /**
     * Sets the desired number of machines in the machine pool. This method is
     * asynchronous and returns immediately after updating the desired size.
     * There may be a delay before the changes take effect and are reflected in
     * the machine pool.
     * <p/>
     * Note: the {@link CloudPool} implementation should take measures to ensure
     * that requested machines are recognized as pool members. The specific
     * mechanism to mark pool members, which may depend on the features offered
     * by the particular cloud API, is left to the implementation but could, for
     * example, make use of tags.
     *
     * @param desiredSize
     *            The desired number of machines in the pool.
     *
     * @throws IllegalArgumentException
     *             If the desired size is illegal.
     * @throws CloudPoolException
     *             If the operation could not be completed.
     * @throws NotStartedException
     *             If the {@link CloudPool} is not started.
     */
    void setDesiredSize(int desiredSize) throws IllegalArgumentException, CloudPoolException, NotStartedException;

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
     * @throws CloudPoolException
     *             If the operation could not be completed.
     * @throws NotStartedException
     *             If the {@link CloudPool} is not started.
     */
    void terminateMachine(String machineId, boolean decrementDesiredSize)
            throws NotFoundException, CloudPoolException, NotStartedException;

    /**
     * Sets the service state of a given machine pool member. Setting the
     * service state does not have any functional implications on the pool
     * member, but should be seen as way to supply operational information about
     * the service running on the machine to third-party services (such as load
     * balancers).
     *
     * @param machineId
     *            The id of the machine whose service state is to be updated.
     * @param serviceState
     *            The {@link ServiceState} to assign to the machine.
     * @throws NotFoundException
     *             If the specified machine is not a member of the pool.
     * @throws CloudPoolException
     *             If the operation could not be completed.
     * @throws NotStartedException
     *             If the {@link CloudPool} is not started.
     */
    void setServiceState(String machineId, ServiceState serviceState)
            throws NotFoundException, CloudPoolException, NotStartedException;

    /**
     * Sets the membership status of a given pool member.
     * <p/>
     * The membership status for a machine can be set to protect the machine
     * from being terminated (by setting its evictability status) and/or to mark
     * a machine as being in need of replacement by flagging it as an inactive
     * pool member.
     *
     * @param machineId
     *            The id of the machine whose status is to be updated.
     * @param membershipStatus
     *            The {@link MembershipStatus} to set.
     * @throws NotFoundException
     * @throws CloudPoolException
     * @throws NotStartedException
     *             If the {@link CloudPool} is not started.
     */
    void setMembershipStatus(String machineId, MembershipStatus membershipStatus)
            throws NotFoundException, CloudPoolException, NotStartedException;

    /**
     * Attaches an already running machine instance to the pool, growing the
     * pool with a new member. This operation implies that the desired size of
     * the pool is incremented by one.
     *
     * @param machineId
     *            The identifier of the machine to attach to the pool.
     * @throws NotFoundException
     *             If the specified machine does not exist.
     * @throws CloudPoolException
     *             If the operation could not be completed.
     * @throws NotStartedException
     *             If the {@link CloudPool} is not started.
     */
    void attachMachine(String machineId) throws NotFoundException, CloudPoolException, NotStartedException;

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
     * @throws CloudPoolException
     *             If the operation could not be completed.
     * @throws NotStartedException
     *             If the {@link CloudPool} is not started.
     */
    void detachMachine(String machineId, boolean decrementDesiredSize)
            throws NotFoundException, CloudPoolException, NotStartedException;

    /**
     * @return A {@link CloudPoolMetadata} object that describes certain static
     *         metadata about the cloud pool implementation and the cloud it
     *         manages.
     */
    CloudPoolMetadata getMetadata();
}
