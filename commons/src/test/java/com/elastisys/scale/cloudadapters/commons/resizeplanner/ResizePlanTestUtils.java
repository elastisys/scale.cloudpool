package com.elastisys.scale.cloudadapters.commons.resizeplanner;

import java.util.List;

import org.joda.time.DateTime;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapters.commons.termqueue.ScheduledTermination;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

/**
 * Test utilities for {@link ResizePlan} and {@link ResizePlanner} tests.
 * 
 * 
 * 
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
	 * state.
	 * 
	 * @param idIndex
	 *            The identifier sequence number of the machine.
	 * @param launchTime
	 *            A launch times for the machine. <code>null</code> is accepted
	 *            and represent a machine that hasn't started (yet).
	 * @param state
	 *            The {@link MachineState}.
	 * @return
	 */
	public static Machine makeMachine(int idIndex, DateTime launchTime,
			MachineState state) {
		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();
		return new Machine("instance-" + idIndex, state, launchTime, publicIps,
				privateIps, new JsonObject());
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
		List<Machine> machines = Lists.newArrayList();
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
		List<Machine> machines = Lists.newArrayList();
		return new MachinePool(machines, UtcTime.now());
	}

	/**
	 * Creates an empty {@link MachinePool}.
	 * 
	 * @return
	 */
	public static MachinePool makePool(DateTime timestamp,
			List<Machine> machines) {
		return new MachinePool(machines, UtcTime.now());
	}

	public static DateTime nowOffset(int secondsOffset) {
		return UtcTime.now().plusSeconds(secondsOffset);
	}

	public static ScheduledTermination termination(String instanceId,
			DateTime terminationTime) {
		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();
		return new ScheduledTermination(new Machine(instanceId,
				MachineState.RUNNING, UtcTime.now(), publicIps, privateIps,
				new JsonObject()), terminationTime);
	}

	public static List<ScheduledTermination> toTerminate(
			ScheduledTermination... scheduledTerminations) {
		List<ScheduledTermination> list = Lists.newArrayList();
		for (ScheduledTermination scheduledTermination : scheduledTerminations) {
			list.add(scheduledTermination);
		}
		return list;
	}

	/**
	 * Returns all {@link Machine} instances that are to be scheduled for
	 * termination in a certain {@link ResizePlan}.
	 * 
	 * @param plan
	 * @return
	 */
	public static List<Machine> terminationMarked(ResizePlan plan) {
		List<Machine> terminationMarkedMachines = Lists.newArrayList();
		List<ScheduledTermination> scheduledTerminations = plan
				.getToTerminate();
		for (ScheduledTermination scheduledTermination : scheduledTerminations) {
			terminationMarkedMachines.add(scheduledTermination.getInstance());
		}
		return terminationMarkedMachines;
	}
}
