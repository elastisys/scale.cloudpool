package com.elastisys.scale.cloudpool.commons.scaledown.strategies;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Comparator;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionStrategy;

/**
 * A {@link VictimSelectionStrategy} that selects the most recently created
 * instance for termination.
 *
 *
 */
public enum NewestMachineVictimSelectionStrategy implements VictimSelectionStrategy {

    /** The single instance of this class. */
    INSTANCE;

    @Override
    public Machine selectVictim(Collection<Machine> candidates) throws IllegalArgumentException {
        requireNonNull(candidates, "null candidate set");
        checkArgument(!candidates.isEmpty(), "empty candidate set");

        return Machine.sort(candidates, new NewestFirstOrder()).get(0);
    }

    /**
     * {@link Comparator} that orders {@link Machine} instances in order of
     * increasing age (youngest instance first). A <code>null</code> requestTime
     * is considered youngest (rationale: a very recently requested machine may
     * not have had all metadata initialized). In case no request time is set or
     * they are equal, a comparison is made on the {@link Machine} id.
     */
    public class NewestFirstOrder implements Comparator<Machine> {
        @Override
        public int compare(Machine m1, Machine m2) {

            Comparator<Machine> launchTimeComparator = Comparator.comparing(Machine::getLaunchTime,
                    Comparator.nullsFirst(Comparator.reverseOrder()));

            Comparator<Machine> idComparator = Comparator.comparing(Machine::getId, Comparator.reverseOrder());
            return launchTimeComparator.thenComparing(idComparator).compare(m1, m2);
        }
    }

}