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
public enum OldestMachineVictimSelectionStrategy implements VictimSelectionStrategy {
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
     * decreasing age (oldest instance first). A <code>null</code> requestTime
     * is considered youngest (rationale: a very recently requested machine may
     * not have had all metadata initialized). In case no request time is set or
     * they are equal, a comparison is made on the {@link Machine} id.
     */
    public static class OldestFirstOrder implements Comparator<Machine> {
        @Override
        public int compare(Machine m1, Machine m2) {
            Comparator<Machine> launchTimeComparator = Comparator.comparing(Machine::getLaunchTime,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            Comparator<Machine> idComparator = Comparator.comparing(Machine::getId, Comparator.naturalOrder());
            return launchTimeComparator.thenComparing(idComparator).compare(m1, m2);
        }
    }
}