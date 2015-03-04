package com.elastisys.scale.cloudpool.commons.resizeplanner;

import static com.elastisys.scale.cloudpool.api.types.Machine.withState;
import static com.elastisys.scale.cloudpool.api.types.MachineState.REQUESTED;
import static com.elastisys.scale.commons.util.time.UtcTime.now;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Collections2.filter;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.scaledown.TerminationScheduler;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelector;
import com.elastisys.scale.cloudpool.commons.termqueue.ScheduledTermination;
import com.elastisys.scale.cloudpool.commons.termqueue.TerminationQueue;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

/**
 * A {@link ResizePlanner} determines, for a certain machine pool, necessary
 * scaling actions needed to meet a certain desired pool size.
 * <p/>
 * Given a machine pool and a termination queue (holding pool members currently
 * scheduled for termination) and policies for scale-downs, a
 * {@link ResizePlanner} produces a {@link ResizePlan} that tells a cloud pool
 * how to modify the pool in order to reach a certain desired pool size.
 * <p/>
 * The effective size of the machine pool is considered to be the set of
 * effective machines in the pool (see {@link Machine#isEffectiveMember()}) that
 * have not been scheduled for termination. That is, all allocated machines that
 * have not been marked as out-of-service (awaiting troubleshooting/repair) and
 * aren't already scheduled for termination.
 *
 * @see ResizePlan
 */
public class ResizePlanner {
	static final Logger LOG = LoggerFactory.getLogger(ResizePlanner.class);

	/** The current pool members. */
	private final MachinePool machinePool;
	/**
	 * Termination queue holding the pool members currently scheduled for
	 * termination.
	 */
	private final TerminationQueue terminationQueue;
	/** The {@link VictimSelectionPolicy} to use when shrinking the pool. */
	private final VictimSelectionPolicy victimSelectionPolicy;
	/**
	 * How many seconds prior to the next instance hour machines should be
	 * scheduled for termination. This should be set to a conservative and safe
	 * value to prevent machines from being billed for an additional hour. If
	 * set to {@code 0}, machines are scheduled for immediate termination. This
	 * may be useful in clouds that allow per-minute billing, or in private
	 * clouds where machine termination should be carried out as quickly as
	 * possible.
	 */
	private final long instanceHourMargin;

	/**
	 * Creates a new {@link ResizePlanner} for a certain machine pool.
	 *
	 * @param machinePool
	 *            The current pool members.
	 * @param terminationQueue
	 *            Termination queue holding the pool members currently scheduled
	 *            for termination.
	 * @param victimSelectionPolicy
	 *            The {@link VictimSelectionPolicy} to use when shrinking the
	 *            pool.
	 * @param instanceHourMargin
	 *            How many seconds prior to the next instance hour machines
	 *            should be scheduled for termination. This should be set to a
	 *            conservative and safe value to prevent machines from being
	 *            billed for an additional hour. If set to {@code 0}, machines
	 *            are scheduled for immediate termination. This may be useful in
	 *            clouds that allow per-minute billing, or in private clouds
	 *            where machine termination should be carried out as quickly as
	 *            possible.
	 */
	public ResizePlanner(MachinePool machinePool,
			TerminationQueue terminationQueue,
			VictimSelectionPolicy victimSelectionPolicy, long instanceHourMargin) {
		this.machinePool = machinePool;
		this.terminationQueue = terminationQueue;
		this.victimSelectionPolicy = victimSelectionPolicy;
		this.instanceHourMargin = instanceHourMargin;
		validate();
	}

	/**
	 * Performs a basic sanity check of this {@link ResizePlanner}. If values
	 * are sane, the method simply returns. Should the {@link ResizePlanner}
	 * contain an illegal mix of values, an {@link IllegalArgumentException} is
	 * thrown.
	 *
	 * @throws IllegalArgumentException
	 */
	public void validate() throws IllegalArgumentException {
		checkArgument(this.machinePool != null, "missing machinePool");
		checkArgument(this.terminationQueue != null,
				"missing termination queue");
		checkArgument(this.victimSelectionPolicy != null,
				"missing victim selection policy");

		long hourSeconds = TimeUnit.SECONDS.convert(1, TimeUnit.HOURS);
		checkArgument(
				Range.closedOpen(0L, hourSeconds).contains(
						this.instanceHourMargin),
				"instanceHourMargin must be within interval [0, 3600) seconds.");
	}

	/**
	 * Returns the <i>effective size</i> of the machine pool, being the number
	 * allocated machines that have not been marked as out-of-service (awaiting
	 * troubleshooting/repair) and aren't already scheduled for termination.
	 *
	 * @see Machine#isEffectiveMember()
	 *
	 * @return
	 */
	public int getEffectiveSize() {
		List<Machine> effectiveMembers = effectiveMembers(this.machinePool);
		int currentPoolSize = effectiveMembers.size();
		// number of pool machines currently scheduled for termination
		int termQueueSize = this.terminationQueue.size();
		// number of pool members that are not marked for termination
		int netSize = currentPoolSize - termQueueSize;
		return netSize;
	}

	/**
	 * Calculates how the pool should be resized to reach a certain desired
	 * size.
	 *
	 * @param desiredSize
	 *            The desired number of active machines in the machine pool.
	 * @return A {@link ResizePlan} to reach the desired pool size.
	 */
	public ResizePlan calculateResizePlan(int desiredSize) {
		checkArgument(desiredSize >= 0, "desired pool size must be >= 0");

		int toRequest = 0;
		int toSpare = 0;
		List<ScheduledTermination> toTerminate = Lists.newArrayList();

		// Only count effective machines. Machines marked OUT_OF_SERVICE should
		// remain as passive members of the pool for troubleshooting.
		List<Machine> effectiveMachines = this.machinePool
				.getEffectiveMachines();
		int effectivePoolSize = effectiveMachines.size();
		int allocated = this.machinePool.getAllocatedMachines().size();
		int outOfService = this.machinePool.getOutOfServiceMachines().size();
		int termQueueSize = this.terminationQueue.size();
		int netSize = getEffectiveSize();

		LOG.debug("desired pool size: {}, " + "effective pool size: {} "
				+ "(allocated: {}, out-of-service: {}), "
				+ "net size (excluding termination-queued): {}, "
				+ "termination queue: {}", desiredSize, effectivePoolSize,
				allocated, outOfService, netSize, this.terminationQueue);

		if (desiredSize > netSize) {
			// need to scale up
			int missingMachines = desiredSize - netSize;
			toSpare = Math.min(termQueueSize, missingMachines);
			toRequest = missingMachines - toSpare;
		} else if (desiredSize < netSize) {
			// need to scale down
			int excessMachines = netSize - desiredSize;
			toTerminate = scheduleForTermination(excessMachines);
		} else {
			LOG.debug("desired size {} equals net pool size, nothing to do",
					desiredSize);
		}

		ResizePlan resizePlan = new ResizePlan(toRequest, toSpare, toTerminate);
		LOG.debug("suggested resize plan: {}", resizePlan);
		return resizePlan;
	}

	/**
	 * Selects a number of victim machines and schedules them for termination in
	 * order to shrink the machine pool.
	 *
	 * @param excessMachines
	 *            The number of machines to terminate.
	 * @return
	 */
	private List<ScheduledTermination> scheduleForTermination(int excessMachines) {
		List<ScheduledTermination> toTerminate = Lists.newArrayList();
		List<Machine> candidates = effectiveMembers(this.machinePool);

		LOG.debug("need to select {} victim(s) for termination from "
				+ "{} effective machine(s)", excessMachines, candidates.size());

		// Favor termination of REQUESTED machines (since these are likely
		// to not yet incur cost). Terminate them immediately.
		Iterable<Machine> inRequestedState = filter(candidates,
				withState(REQUESTED));
		Iterator<Machine> requestedStateMachines = inRequestedState.iterator();
		while ((excessMachines > 0) && requestedStateMachines.hasNext()) {
			toTerminate.add(new ScheduledTermination(requestedStateMachines
					.next(), now()));
			excessMachines--;
		}

		// use victim selection policy to pick victims from any remaining
		// candidates
		candidates.removeAll(Lists.newArrayList(inRequestedState));
		List<Machine> victims = victimSelector().selectVictims(candidates,
				excessMachines);
		for (Machine victim : victims) {
			toTerminate.add(scheduleTermination(victim));
		}
		return toTerminate;
	}

	private ScheduledTermination scheduleTermination(Machine victim) {
		return new TerminationScheduler(this.instanceHourMargin)
				.scheduleEviction(victim);
	}

	private VictimSelector victimSelector() {
		return new VictimSelector(this.terminationQueue,
				this.victimSelectionPolicy.getVictimSelectionStrategy());
	}

	/**
	 * Collects all effective machines of a machine pool (see
	 * {@link Machine#isEffectiveMember()}). This excludes {@link Machine}s that
	 * have been marked {@link ServiceState#OUT_OF_SERVICE} (these should remain
	 * as passive members of the pool for troubleshooting).
	 *
	 * @param machinePool
	 *
	 * @return
	 */
	private List<Machine> effectiveMembers(MachinePool machinePool) {
		return machinePool.getEffectiveMachines();
	}
}
