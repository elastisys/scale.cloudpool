package com.elastisys.scale.cloudadapters.commons.scaledown.strategies;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.List;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.commons.scaledown.VictimSelectionStrategy;
import com.google.common.base.Function;

/**
 * A {@link VictimSelectionStrategy} that selects the instance that is closest
 * to the end of its current instance hour for termination.
 * 
 * 
 */
public enum ClosestToInstanceHourVictimSelectionStrategy implements
		VictimSelectionStrategy {
	/** The single instance of this class. */
	INSTANCE;

	@Override
	public Machine selectVictim(List<Machine> candidates)
			throws IllegalArgumentException {
		checkNotNull(candidates, "null candidate set");
		checkArgument(!candidates.isEmpty(), "empty candidate set");

		return Machine.sort(candidates, new ClosestToInstanceHourOrder())
				.get(0);
	}

	/**
	 * A {@link Comparator} that orders {@link Machine} instances in order of
	 * increasing time to the next instance hour (that is, the instance closest
	 * to the end of its current instance hour first).
	 * 
	 * 
	 * 
	 */
	public class ClosestToInstanceHourOrder implements Comparator<Machine> {

		@Override
		public int compare(Machine instance1, Machine instance2) {
			Function<Machine, Long> remainingInstanceHourTime = new Machine.RemainingInstanceHourTime();
			long i1TimeLeft = remainingInstanceHourTime.apply(instance1);
			long i2TimeLeft = remainingInstanceHourTime.apply(instance2);
			return (int) (i1TimeLeft - i2TimeLeft);
		}
	}
}