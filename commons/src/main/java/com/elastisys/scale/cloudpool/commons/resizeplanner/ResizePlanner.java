package com.elastisys.scale.cloudpool.commons.resizeplanner;

import static com.elastisys.scale.cloudpool.api.types.Machine.isActiveMember;
import static com.elastisys.scale.cloudpool.api.types.Machine.isEvictable;
import static com.elastisys.scale.cloudpool.api.types.Machine.withState;
import static com.elastisys.scale.cloudpool.api.types.MachineState.REQUESTED;
import static com.elastisys.scale.commons.util.time.UtcTime.now;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Collections2.filter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.commons.scaledown.TerminationScheduler;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelector;
import com.elastisys.scale.cloudpool.commons.termqueue.ScheduledTermination;
import com.elastisys.scale.cloudpool.commons.termqueue.TerminationQueue;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

/**
 * A {@link ResizePlanner} determines necessary scaling actions needed to take a
 * machine pool to a certain desired pool size.
 * <p/>
 * Given a machine pool and a termination queue (holding pool members currently
 * scheduled for termination) and policies for scale-downs, a
 * {@link ResizePlanner} produces a {@link ResizePlan} that tells a cloud pool
 * how to modify the pool in order to reach a certain desired pool size.
 * <p/>
 * At any time, the <i>net size</i> of the machine pool is considered to be the
 * set of active machines in the pool (see {@link Machine#isActiveMember()})
 * that have not been scheduled for termination.
 * <p/>
 * When it comes to reducing the pool size, machines that are blessed by virtue
 * of not being evictable ({@link MembershipStatus#isEvictable()}) are never
 * considered for termination.
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
	 * Returns the <i>net size</i> of the machine pool, being the number of
	 * active pool members that aren't already scheduled for termination.
	 *
	 * @see Machine#isActiveMember()
	 *
	 * @return
	 */
	public int getNetSize() {
		List<Machine> activeMembers = this.machinePool.getActiveMachines();
		int currentPoolSize = activeMembers.size();
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

		List<Machine> activeMachines = this.machinePool.getActiveMachines();
		int active = activeMachines.size();
		int allocated = this.machinePool.getAllocatedMachines().size();
		int termQueueSize = this.terminationQueue.size();
		// the net size of the group only considers active pool members not
		// already marked for termination
		int netSize = getNetSize();

		LOG.debug("desired pool size: {} (allocated: {}, active: {}), "
				+ "net size (excluding termination-queued): {}, "
				+ "termination queue: {}", desiredSize, allocated, active,
				netSize, this.terminationQueue);

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
		// schedule any inactive, evictable machines for termination
		toTerminate.addAll(disposableMachines());

		ResizePlan resizePlan = new ResizePlan(toRequest, toSpare, toTerminate);
		LOG.debug("suggested resize plan: {}", resizePlan);
		return resizePlan;
	}

	/**
	 * Selects a number of victim machines and schedules them for termination in
	 * order to shrink the machine pool.
	 *
	 * @param excessMachines
	 *            The desired number of machines to terminate.
	 * @return
	 */
	private List<ScheduledTermination> scheduleForTermination(int excessMachines) {
		LOG.debug("need {} victim(s) to reach desired size", excessMachines);
		List<ScheduledTermination> toTerminate = Lists.newArrayList();
		Collection<Machine> candidates = getTerminationCandidates();
		LOG.debug("there are {} evictable candidate(s)", candidates.size());
		// the evictable candidate set can be smaller than excessMachines
		excessMachines = Math.min(excessMachines, candidates.size());
		LOG.debug("selecting {} victim(s) from {} candidate(s)",
				excessMachines, candidates.size());

		// Favor termination of REQUESTED machines (since these are likely
		// to not yet incur cost). Terminate them immediately.
		Iterable<Machine> requested = filter(candidates, withState(REQUESTED));
		Iterator<Machine> requestedMachines = requested.iterator();
		while ((excessMachines > 0) && requestedMachines.hasNext()) {
			toTerminate.add(new ScheduledTermination(requestedMachines.next(),
					now()));
			excessMachines--;
		}

		// use victim selection policy to pick victims from any remaining
		// candidates
		candidates.removeAll(Lists.newArrayList(requested));
		List<Machine> victims = victimSelector().selectVictims(candidates,
				excessMachines);
		for (Machine victim : victims) {
			toTerminate.add(scheduleTermination(victim));
		}
		return toTerminate;
	}

	/**
	 * Returns all machines that are candidates for being terminated. This
	 * includes all active machines, with a {@link MembershipStatus} that is
	 * evictable and that haven't already been added to the termination queue.
	 *
	 * @return
	 */
	private Collection<Machine> getTerminationCandidates() {
		// only consider active pool members
		Collection<Machine> candidates = this.machinePool.getActiveMachines();
		// filter out blessed pool members (marked as not being evictable)
		candidates = filter(candidates, Machine.isEvictable());
		// filter out already termination marked members
		candidates = filter(candidates, not(terminationMarked()));
		return candidates;
	}

	/**
	 * Schedules any disposable (inactive, evictable {@link MembershipStatus})
	 * machines for termination (if there are any).
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Collection<ScheduledTermination> disposableMachines() {
		// consider all allocated pool members ...
		Collection<Machine> disposables = this.machinePool
				.getAllocatedMachines();
		// ... that are inactive, evictable and not already termination-marked
		disposables = filter(
				disposables,
				and(not(isActiveMember()), isEvictable(),
						not(terminationMarked())));

		List<ScheduledTermination> terminations = Lists.newLinkedList();
		for (Machine disposable : disposables) {
			terminations.add(scheduleTermination(disposable));
		}
		return terminations;
	}

	private ScheduledTermination scheduleTermination(Machine victim) {
		return new TerminationScheduler(this.instanceHourMargin)
				.scheduleEviction(victim);
	}

	private VictimSelector victimSelector() {
		return new VictimSelector(
				this.victimSelectionPolicy.getVictimSelectionStrategy());
	}

	/**
	 * Returns a {@link Predicate} that will be <code>true</code> for any
	 * {@link Machine} that has been added to the termination queue.
	 *
	 * @return
	 */
	private Predicate<Machine> terminationMarked() {
		return new Predicate<Machine>() {
			@Override
			public boolean apply(Machine machine) {
				return ResizePlanner.this.terminationQueue.getQueuedInstances()
						.contains(machine);
			}
		};
	}

}
