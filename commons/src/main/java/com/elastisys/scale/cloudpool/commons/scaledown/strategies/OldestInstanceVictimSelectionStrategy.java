package com.elastisys.scale.cloudpool.commons.scaledown.strategies;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Comparator;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionStrategy;

/**
 * A {@link VictimSelectionStrategy} that selects the instance created the
 * longest time ago for termination.
 *
 *
 */
public enum OldestInstanceVictimSelectionStrategy implements VictimSelectionStrategy {
    /** The single instance of this class. */
    INSTANCE;

    @Override
    public Machine selectVictim(Collection<Machine> candidates) throws IllegalArgumentException {
        checkNotNull(candidates, "null candidate set");
        checkArgument(!candidates.isEmpty(), "empty candidate set");

        return Machine.sort(candidates, new OldestFirstOrder()).get(0);
    }

    /**
     * {@link Comparator} that orders {@link Machine} instances in order of
     * decreasing age (oldest instance first).
     *
     *
     *
     */
    public static class OldestFirstOrder implements Comparator<Machine> {
        @Override
        public int compare(Machine instance1, Machine instance2) {
            return instance1.getLaunchTime().compareTo(instance2.getLaunchTime());
        }
    }
}