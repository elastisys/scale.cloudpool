package com.elastisys.scale.cloudpool.commons.scaledown;

import java.util.Collection;

import com.elastisys.scale.cloudpool.api.types.Machine;

/**
 * Represents a policy for which machine instance, from a collection of machine
 * instances, to select for (eventual) termination when a scale-down of a
 * scaling group is ordered.
 */
public interface VictimSelectionStrategy {

    /**
     * Selects which instance, from a collection of machine instances, to
     * schedule for (eventual) termination.
     *
     * @param candidates
     *            The collection of machines eligible for termination.
     * @return The selected victim machine instance.
     */
    Machine selectVictim(Collection<Machine> candidates);
}
