package com.elastisys.scale.cloudpool.commons.basepool.driver;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

/**
 * A {@link CloudPoolDriver} with no real backend: it only pretends to have one.
 */
public class DummyCloudPoolDriver implements CloudPoolDriver{

    private List<Machine> machines = new ArrayList<>();
    private Hashtable<String, ServiceState> serviceStates = new Hashtable<>();
    private Hashtable<String, MembershipStatus> membershipStatuses = new Hashtable<>();

    public DummyCloudPoolDriver() {

    }
    /**
     * Does nothing for now
     */
    @Override public void configure(DriverConfig configuration)
            throws IllegalArgumentException, CloudPoolDriverException {
        return;
    }

    /**
     *
     */
    @Override public List<Machine> listMachines() throws IllegalStateException, CloudPoolDriverException {
        return machines;
    }

    /**
     *
     */
    @Override public List<Machine> startMachines(int count) throws IllegalStateException, StartMachinesException {
        ArrayList<Machine> started = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Machine newMachine = createMachine();
            machines.add(newMachine);
            started.add(newMachine);
        }
        return started;
    }

    /**
     * Terminates a {@link Machine} in the cloud pool.
     *
     * @param machineId The identifier of the {@link Machine}.
     * @throws IllegalStateException    If the {@link CloudPoolDriver} has not been configured.
     * @throws NotFoundException        If the machine is not a member of the pool.
     * @throws CloudPoolDriverException If anything went wrong.
     */
    @Override public void terminateMachine(String machineId)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        for (Machine m : machines) {
            if (m.getId().equals(machineId)) {
                m.setMachineState(MachineState.TERMINATED);
                return;
            }
        }
        throw new NotFoundException("Machine with ID " + machineId + " is not managed by DummyPool");
    }

    /**
     * Attaches an already running machine instance to the cloud pool.
     *
     * @param machineId The identifier of the machine to attach to the cloud pool.
     * @throws IllegalStateException    If the {@link CloudPoolDriver} has not been configured.
     * @throws NotFoundException        If the machine does not exist.
     * @throws CloudPoolDriverException If the operation could not be completed.
     */
    @Override public void attachMachine(String machineId)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        machines.add(createMachine(machineId));
    }

    /**
     * Removes a member from the cloud pool without terminating it. The machine
     * keeps running but is no longer considered a cloud pool member and,
     * therefore, needs to be managed independently.
     *
     * @param machineId The identifier of the machine to detach from the cloud pool.
     * @throws IllegalStateException    If the {@link CloudPoolDriver} has not been configured.
     * @throws NotFoundException        If the machine is not a member of the cloud pool.
     * @throws CloudPoolDriverException If the operation could not be completed.
     */
    @Override public void detachMachine(String machineId)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        for (Machine m : machines) {
            if (m.getId().equals(machineId)) {
                machines.remove(m);
                return;
            }
        }
        throw new NotFoundException("Machine with ID " + machineId + " is not managed by DummyPool");
    }

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
     * @param machineId    The id of the machine whose service state is to be updated.
     * @param serviceState The {@link ServiceState} to assign to the machine.
     * @throws IllegalStateException    If the {@link CloudPoolDriver} has not been configured.
     * @throws NotFoundException        If the machine is not a member of the cloud pool.
     * @throws CloudPoolDriverException If the operation could not be completed.
     */
    @Override public void setServiceState(String machineId, ServiceState serviceState)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        for (Machine m : machines) {
            if (m.getId().equals(machineId)) {
                serviceStates.put(machineId,serviceState);
                return;
            }
        }
        throw new NotFoundException("Machine with ID " + machineId + " is not managed by DummyPool");
    }

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
     * @param machineId        The id of the machine whose status is to be updated.
     * @param membershipStatus The {@link MembershipStatus} to set.
     * @throws IllegalStateException    If the {@link CloudPoolDriver} has not been configured.
     * @throws NotFoundException        If the machine is not a member of the cloud pool.
     * @throws CloudPoolDriverException If the operation could not be completed.
     */
    @Override public void setMembershipStatus(String machineId, MembershipStatus membershipStatus)
            throws IllegalStateException, NotFoundException, CloudPoolDriverException {
        for (Machine m : machines) {
            if (m.getId().equals(machineId)) {
                membershipStatuses.put(machineId,membershipStatus);
                return;
            }
        }

    }

    @Override public String getPoolName() throws IllegalStateException {
        return "DummyPool";
    }

    private Machine createMachine() {
        return createMachine(UUID.randomUUID().toString());
    }

    private Machine createMachine(String uuid) {
        return Machine.builder().id(uuid).cloudProvider("Dummy").launchTime(DateTime.now())
                .machineSize("Lagom").machineState(MachineState.RUNNING)
                .membershipStatus(MembershipStatus.defaultStatus()).region("/dev/null")
                .requestTime(DateTime.now()).build();
    }
}
