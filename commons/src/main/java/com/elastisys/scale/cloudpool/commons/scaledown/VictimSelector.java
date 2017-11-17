package com.elastisys.scale.cloudpool.commons.scaledown;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.elastisys.scale.cloudpool.api.types.Machine;

/**
 * Selects a number of victim machines to terminate from a set of candidates by
 * using a {@link VictimSelectionStrategy} to select each machine.
 *
 * @see VictimSelectionStrategy
 */
public class VictimSelector {

    /**
     * The {@link VictimSelectionStrategy} used to select a victim from the
     * candidate set.
     */
    private final VictimSelectionStrategy victimSelectionStrategy;

    /**
     * Constructs a new {@link VictimSelector}.
     *
     * @param victimSelectionStrategy
     *            The {@link VictimSelectionStrategy} used to select a victim
     *            from the candidate set.
     */
    public VictimSelector(VictimSelectionStrategy victimSelectionStrategy) {
        checkNotNull(victimSelectionStrategy, "null victimSelectionStrategy");
        this.victimSelectionStrategy = victimSelectionStrategy;
    }

    /**
     * Selects victim machines to terminate from a set of candidate machines
     * using the configured {@link VictimSelectionStrategy}.
     *
     * @param candidates
     *            The collection of candidate machine instances.
     * @param numVictims
     *            The number of victims to select.
     * @return
     */
    public List<Machine> selectVictims(Collection<Machine> candidates, long numVictims)
            throws IllegalArgumentException {
        checkNotNull(candidates, "null candidates");
        checkArgument(numVictims >= 0, "negative number of victims");
        checkArgument(candidates.size() >= numVictims, "more victims than termination candidates");

        // defensive copy
        List<Machine> terminationCandidates = new ArrayList<>(candidates);

        List<Machine> victims = new ArrayList<>();
        for (int i = 0; i < numVictims; i++) {
            // use victim selection strategy to pick a victim
            Machine victim = this.victimSelectionStrategy.selectVictim(terminationCandidates);
            victims.add(victim);
            terminationCandidates.remove(victim);
        }

        return victims;
    }

}
