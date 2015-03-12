package com.elastisys.scale.cloudpool.commons.scaledown;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;

import com.elastisys.scale.cloudpool.api.types.Machine;
import com.google.common.collect.Lists;

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
	 * Selects victim instances to terminate from a set of candidate machines.
	 * The selection is performed as follows:
	 * <ol>
	 * <li>Filter out already termination marked instances (i.e., instances in
	 * the termination queue).</li>
	 * <li>Use the {@link VictimSelectionStrategy} to select an instance from
	 * the candidate set.</li>
	 * </ol>
	 *
	 * @param candidates
	 *            The collection of candidate machine instances.
	 * @param numVictims
	 *            The number of victims to select.
	 * @return
	 */
	public List<Machine> selectVictims(Collection<Machine> candidates,
			long numVictims) throws IllegalArgumentException {
		checkNotNull(candidates, "null candidates");
		checkArgument(numVictims >= 0, "negative number of victims");
		checkArgument(candidates.size() >= numVictims,
				"more victims than termination candidates");

		List<Machine> terminationCandidates = Lists.newArrayList(candidates);
		List<Machine> victims = Lists.newArrayList();
		for (int i = 0; i < numVictims; i++) {
			// use victim selection strategy to pick a victim
			Machine victim = this.victimSelectionStrategy
					.selectVictim(terminationCandidates);
			victims.add(victim);
			terminationCandidates.remove(victim);
		}

		return victims;
	}

}
