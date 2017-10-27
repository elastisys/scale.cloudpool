package com.elastisys.scale.cloudpool.commons.scaledown;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.commons.scaledown.strategies.NewestMachineVictimSelectionStrategy;
import com.elastisys.scale.cloudpool.commons.scaledown.strategies.OldestMachineVictimSelectionStrategy;

/**
 * The collection of valid victim selection policies that governs how to select
 * a {@link Machine} to be terminated when a scale-down has been ordered.
 * <p/>
 * Each policy is paired with a {@link VictimSelectionStrategy} that implements
 * the selection algorithm for the policy.
 *
 * @see VictimSelectionStrategy
 */
public enum VictimSelectionPolicy {
    /**
     * Specify this if you want the oldest instance in your Auto Scaling group
     * to be terminated.
     */
    OLDEST(OldestMachineVictimSelectionStrategy.INSTANCE),
    /** Specify this if you want the last launched instance to be terminated. */
    NEWEST(NewestMachineVictimSelectionStrategy.INSTANCE);

    /**
     * The {@link VictimSelectionStrategy} associated with this
     * {@link VictimSelectionPolicy}.
     */
    private VictimSelectionStrategy victimSelectionStrategy;

    /**
     * Constructs a new {@link VictimSelectionPolicy}.
     *
     * @param victimSelectionStrategy
     *            the {@link VictimSelectionStrategy} associated with this
     *            {@link VictimSelectionPolicy}.
     */
    private VictimSelectionPolicy(VictimSelectionStrategy victimSelectionStrategy) {
        this.victimSelectionStrategy = victimSelectionStrategy;
    }

    /**
     * Returns the {@link VictimSelectionStrategy} associated with this
     * {@link VictimSelectionPolicy}.
     *
     * @return
     */
    public VictimSelectionStrategy getVictimSelectionStrategy() {
        return this.victimSelectionStrategy;
    }
}
