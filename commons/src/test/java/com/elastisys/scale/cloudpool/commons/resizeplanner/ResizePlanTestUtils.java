package com.elastisys.scale.cloudpool.commons.resizeplanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Test utilities for {@link ResizePlan} and {@link ResizePlanner} tests.
 */
public class ResizePlanTestUtils {

    /**
     * Creates a {@link Machine} with a given identifier index and launch time
     * in state {@link MachineState#RUNNING} (unless the launch time is
     * <code>null</code>, in which case the state is
     * {@link MachineState#REQUESTED}).
     *
     * @param idIndex
     *            The identifier sequence number of the machine.
     * @param launchTime
     *            A launch times for the machine. <code>null</code> is accepted
     *            and represent a machine that hasn't started (yet).
     * @return
     */
    public static Machine makeMachine(int idIndex, DateTime launchTime) {
        MachineState state = MachineState.RUNNING;
        if (launchTime == null) {
            state = MachineState.REQUESTED;
        }
        return makeMachine(idIndex, launchTime, state);
    }

    /**
     * Creates a {@link Machine} with a given identifier index, launch time and
     * machine state. The membership state is set to
     * {@link MembershipStatus#defaultStatus()}.
     *
     * @param idIndex
     *            The identifier sequence number of the machine.
     * @param launchTime
     *            A launch times for the machine. <code>null</code> is accepted
     *            and represent a machine that hasn't started (yet).
     * @param machineState
     *            The {@link MachineState}.
     * @return
     */
    public static Machine makeMachine(int idIndex, DateTime launchTime, MachineState machineState) {
        return makeMachine(idIndex, launchTime, machineState, MembershipStatus.defaultStatus());
    }

    /**
     * Creates a {@link Machine} with a given identifier index, request time,
     * launch time, machine state and service state.
     *
     * @param idIndex
     *            The identifier sequence number of the machine.
     * @param launchTime
     *            A launch times for the machine. <code>null</code> is accepted
     *            and represent a machine that hasn't started (yet).
     * @param machineState
     *            The {@link MachineState}.
     * @param membershipStatus
     *            The {@link MembershipStatus}.
     * @return
     */
    public static Machine makeMachine(int idIndex, DateTime requestTime, DateTime launchTime, MachineState machineState,
            MembershipStatus membershipStatus) {
        return Machine.builder().id("instance-" + idIndex).machineState(machineState).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").membershipStatus(membershipStatus).requestTime(requestTime)
                .launchTime(launchTime).build();
    }

    /**
     * Creates a {@link Machine} with a given identifier index, launch time,
     * machine state and service state. The request time equals the launch time.
     *
     * @param idIndex
     *            The identifier sequence number of the machine.
     * @param launchTime
     *            A launch times for the machine. <code>null</code> is accepted
     *            and represent a machine that hasn't started (yet).
     * @param machineState
     *            The {@link MachineState}.
     * @param membershipStatus
     *            The {@link MembershipStatus}.
     * @return
     */
    public static Machine makeMachine(int idIndex, DateTime launchTime, MachineState machineState,
            MembershipStatus membershipStatus) {
        return makeMachine(idIndex, launchTime, launchTime, machineState, membershipStatus);
    }

    /**
     * Creates a {@link MachinePool} with {@link Machine}s started according to
     * a list of launch times.
     *
     * @param launchTimes
     *            A list of launch times for the machines in the pool.
     *            <code>null</code> values are accepted and represent machines
     *            that haven't started (yet).
     * @return
     */
    public static MachinePool makePool(List<DateTime> launchTimes) {
        List<Machine> machines = new ArrayList<>();
        int id = 0;
        for (DateTime launchTime : launchTimes) {
            machines.add(makeMachine(id, launchTime));
        }
        return new MachinePool(machines, UtcTime.now());
    }

    /**
     * Creates an empty {@link MachinePool}.
     *
     * @return
     */
    public static MachinePool makePool() {
        List<Machine> machines = new ArrayList<>();
        return new MachinePool(machines, UtcTime.now());
    }

    /**
     * Creates an empty {@link MachinePool}.
     *
     * @return
     */
    public static MachinePool makePool(DateTime timestamp, List<Machine> machines) {
        return new MachinePool(machines, UtcTime.now());
    }

    public static DateTime nowOffset(int secondsOffset) {
        return UtcTime.now().plusSeconds(secondsOffset);
    }

    public static Machine victim(String instanceId) {
        Machine machine = Machine.builder().id(instanceId).machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").launchTime(UtcTime.now()).build();
        return machine;
    }

    public static List<Machine> toTerminate(Machine... victims) {
        return Arrays.asList(victims);
    }

}
