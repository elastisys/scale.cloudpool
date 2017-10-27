package com.elastisys.scale.cloudpool.commons.resizeplanner;

import static com.elastisys.scale.cloudpool.api.types.Machine.inState;
import static com.elastisys.scale.cloudpool.api.types.Machine.isActiveMember;
import static com.elastisys.scale.cloudpool.api.types.Machine.isEvictable;
import static com.elastisys.scale.cloudpool.api.types.MachineState.REQUESTED;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Collections2.filter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelector;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * A {@link ResizePlanner} determines necessary scaling actions needed to take a
 * machine pool to a certain desired pool size.
 * <p/>
 * Given a machine pool and policies for scale-downs, a {@link ResizePlanner}
 * produces a {@link ResizePlan} that tells a cloud pool how to modify the pool
 * in order to reach a certain desired pool size.
 * <p/>
 * At any time, the <i>net size</i> of the machine pool is considered to be the
 * set of active machines in the pool (see {@link Machine#isActiveMember()}).
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
    /** The {@link VictimSelectionPolicy} to use when shrinking the pool. */
    private final VictimSelectionPolicy victimSelectionPolicy;

    /**
     * Creates a new {@link ResizePlanner} for a certain machine pool.
     *
     * @param machinePool
     *            The current pool members.
     * @param victimSelectionPolicy
     *            The {@link VictimSelectionPolicy} to use when shrinking the
     *            pool.
     */
    public ResizePlanner(MachinePool machinePool, VictimSelectionPolicy victimSelectionPolicy) {
        this.machinePool = machinePool;
        this.victimSelectionPolicy = victimSelectionPolicy;
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
        checkArgument(this.victimSelectionPolicy != null, "missing victim selection policy");
    }

    /**
     * Returns the <i>active size</i> of the machine pool, being the number of
     * active pool members.
     *
     * @see Machine#isActiveMember()
     *
     * @return
     */
    public int getActiveSize() {
        return this.machinePool.getActiveMachines().size();
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
        List<Machine> toTerminate = Lists.newArrayList();

        List<Machine> activeMachines = this.machinePool.getActiveMachines();
        int active = activeMachines.size();
        int allocated = this.machinePool.getAllocatedMachines().size();

        LOG.debug("desired pool size: {} (allocated: {}, active: {})", desiredSize, allocated, active);

        if (desiredSize > active) {
            // need to scale up
            toRequest = desiredSize - active;
        } else if (desiredSize < active) {
            // need to scale down
            int excessMachines = active - desiredSize;
            toTerminate = selectVictims(excessMachines);
        } else {
            LOG.debug("desired size {} equals net pool size, nothing to do", desiredSize);
        }
        // also remove machines that are disposable (inactive, evictable) so
        // that they can be replaced
        Collection<Machine> disposables = disposableMachines();
        if (!disposables.isEmpty()) {
            LOG.info(
                    "adding disposable (inactive, evictable) machines for termination so that they can be replaced: {}",
                    Iterables.transform(disposables, Machine.toShortString()));
            toTerminate.addAll(disposables);
        }

        ResizePlan resizePlan = new ResizePlan(toRequest, toTerminate);
        LOG.debug("suggested resize plan: {}", resizePlan);
        return resizePlan;
    }

    /**
     * Selects a number of victim machines to remove from the machine pool.
     *
     * @param excessMachines
     *            The desired number of machines to remove.
     * @return
     */
    private List<Machine> selectVictims(int excessMachines) {
        LOG.debug("need {} victim(s) to reach desired size", excessMachines);
        List<Machine> victims = Lists.newArrayList();
        Collection<Machine> candidates = getTerminationCandidates();
        LOG.debug("there are {} evictable candidate(s)", candidates.size());
        // the evictable candidate set can be smaller than excessMachines
        excessMachines = Math.min(excessMachines, candidates.size());
        LOG.debug("selecting {} victim(s) from {} candidate(s)", excessMachines, candidates.size());

        // favor termination of REQUESTED machines (since these are likely
        // to not yet be useful). Terminate them immediately.
        Iterable<Machine> requested = filter(candidates, inState(REQUESTED));
        Iterator<Machine> requestedMachines = requested.iterator();
        while (excessMachines > 0 && requestedMachines.hasNext()) {
            victims.add(requestedMachines.next());
            excessMachines--;
        }

        // use victim selection policy to pick victims from any remaining
        // candidates
        candidates.removeAll(Lists.newArrayList(requested));
        victims.addAll(victimSelector().selectVictims(candidates, excessMachines));

        return victims;
    }

    /**
     * Returns all machines that are candidates for being terminated. This
     * includes all active machines, with an evictable {@link MembershipStatus}.
     *
     * @return
     */
    private Collection<Machine> getTerminationCandidates() {
        // only consider active pool members
        Collection<Machine> candidates = this.machinePool.getActiveMachines();
        // filter out blessed pool members (marked as not being evictable)
        candidates = filter(candidates, Machine.isEvictable());
        return candidates;
    }

    /**
     * Schedules any disposable (inactive, evictable {@link MembershipStatus})
     * machines for termination (if there are any).
     *
     * @return
     */
    private Collection<Machine> disposableMachines() {
        // consider all allocated pool members ...
        List<Machine> disposables = this.machinePool.getAllocatedMachines();
        // ... that are inactive, evictable and not already termination-marked
        return filter(disposables, and(not(isActiveMember()), isEvictable()));
    }

    private VictimSelector victimSelector() {
        return new VictimSelector(this.victimSelectionPolicy.getVictimSelectionStrategy());
    }

}
