package com.elastisys.scale.cloudadapters.commons.scaledown;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;

import java.util.List;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.commons.termqueue.TerminationQueue;
import com.google.common.collect.Lists;

/**
 * Selects victim instances to terminate from a set of candidate machines. The
 * selection is performed as follows:
 * <ol>
 * <li>Filter out already termination marked instances (i.e., instances in the
 * {@link TerminationQueue}).</li>
 * <li>Use the {@link VictimSelectionStrategy} to select an instance from the
 * candidate set.</li>
 * </ol>
 * 
 * @see TerminationQueue
 * @see VictimSelectionStrategy
 * 
 * 
 */
public class VictimSelector {

	/**
	 * The queue of already termination-marked instances (these will be used to
	 * filter out instances already scheduled for termination from the candidate
	 * set).
	 */
	private final TerminationQueue terminationQueue;
	/**
	 * The {@link VictimSelectionStrategy} used to select a victim from the
	 * candidate set.
	 */
	private final VictimSelectionStrategy victimSelectionStrategy;

	/**
	 * Constructs a new {@link VictimSelector}.
	 * 
	 * @param terminationQueue
	 *            The queue of already termination-marked instances (these will
	 *            be used to filter out instances already scheduled for
	 *            termination from the candidate set).
	 * @param victimSelectionStrategy
	 *            The {@link VictimSelectionStrategy} used to select a victim
	 *            from the candidate set.
	 */
	public VictimSelector(TerminationQueue terminationQueue,
			VictimSelectionStrategy victimSelectionStrategy) {
		checkNotNull(terminationQueue, "null terminationQueue");
		checkNotNull(victimSelectionStrategy, "null victimSelectionStrategy");
		this.terminationQueue = terminationQueue;
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
	public List<Machine> selectVictims(List<Machine> candidates, long numVictims)
			throws IllegalArgumentException {
		checkNotNull(candidates, "null candidates");
		checkArgument(numVictims >= 0, "negative number of victims");
		checkArgument(candidates.size() >= numVictims,
				"more victims than candidates");

		List<Machine> victims = Lists.newArrayList();
		// filter out already termination marked instances
		List<Machine> terminationCandidates = getTerminationCandidates(candidates);
		checkArgument(terminationCandidates.size() >= numVictims,
				"more victims (%s) than eligble termination candidates (%s)",
				numVictims, terminationCandidates.size());
		for (int i = 0; i < numVictims; i++) {
			// use victim selection strategy to pick a victim
			Machine victim = this.victimSelectionStrategy
					.selectVictim(terminationCandidates);
			victims.add(victim);
			terminationCandidates.remove(victim);
		}

		return victims;
	}

	/**
	 * From a collection of {@link Machine}s, extracts a list of machines that
	 * are <i>not</i> already in the termination queue, and hence are candidates
	 * for being selected for termination.
	 * 
	 * @param machines
	 * @return
	 */
	private List<Machine> getTerminationCandidates(List<Machine> machines) {
		List<Machine> terminationMarked = this.terminationQueue
				.getQueuedInstances();
		return Lists.newArrayList(filter(machines, not(in(terminationMarked))));
	}

}
