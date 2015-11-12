package com.elastisys.scale.cloudpool.commons.basepool.poolupdater;

import java.io.Closeable;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;

/**
 * A {@link PoolUpdater} takes care of managing a {@link CloudPool} in order to
 * keep it at its set desired size. It also allows the pool and its members to
 * be updated in various ways (update individual pool members' state and
 * terminating/attaching/detaching specific members).
 * <p/>
 * When its services are no longer needed, the {@link #close()} method of a
 * {@link PoolUpdater} should be called to allow the {@link PoolUpdater} to
 * release any held system resources.
 */
public interface PoolUpdater extends Closeable {
	/**
	 * Sets the desired number of machines in the machine pool.
	 * <p/>
	 * Note that this method does not attempt to apply the desired size
	 * immediately, but only registers the new desired size. The next call to
	 * {@link #resize()}, will make an attempt to satisfy the new desired size.
	 *
	 * @param desiredSize
	 *            The desired number of machines in the pool.
	 *
	 * @throws IllegalArgumentException
	 *             If the desired size is illegal.
	 * @throws CloudPoolException
	 *             If the operation could not be completed.
	 */
	void setDesiredSize(int desiredSize)
			throws IllegalArgumentException, CloudPoolException;

	/**
	 * Returns the currently set desired size if one has been set. If no desired
	 * size has been explicitly set (via {@link #setDesiredSize(int)}), the
	 * {@link PoolUpdater} must try to determine the desired size from the
	 * pool's current membership.
	 *
	 * @return
	 * @throws CloudPoolException
	 *             If the desired size could not be determined.
	 */
	int getDesiredSize() throws CloudPoolException;

	/**
	 * Updates the size of the machine pool to match the currently set desired
	 * size using the provided configuration to govern how machines are to be
	 * added (in case of scale-out) and removed (in case of scale-in).
	 *
	 * @param config
	 *            Configuration that governs how to perform scaling actions.
	 * @throws CloudPoolException
	 */
	void resize(BaseCloudPoolConfig config) throws CloudPoolException;

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
	 */
	void terminateMachine(String machineId, boolean decrementDesiredSize)
			throws NotFoundException, CloudPoolException;

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
	 */
	void setServiceState(String machineId, ServiceState serviceState)
			throws NotFoundException, CloudPoolException;

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
	 *             If the specified machine is not a member of the pool.
	 * @throws CloudPoolException
	 *             If the operation could not be completed.
	 */
	void setMembershipStatus(String machineId,
			MembershipStatus membershipStatus)
					throws NotFoundException, CloudPoolException;

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
	 */
	void attachMachine(String machineId)
			throws NotFoundException, CloudPoolException;

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
	 */
	void detachMachine(String machineId, boolean decrementDesiredSize)
			throws NotFoundException, CloudPoolException;

	/**
	 * Closes this {@link PoolUpdater}, allowing it to release any held system
	 * resources. A {@link PoolUpdater} can not be used after it has been
	 * closed.
	 */
	@Override
	void close();
}
