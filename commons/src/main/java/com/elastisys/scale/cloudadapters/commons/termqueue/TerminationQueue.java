package com.elastisys.scale.cloudadapters.commons.termqueue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import org.joda.time.DateTime;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * Tracks a collection of termination-marked machine instances and their
 * scheduled termination times.
 * <p/>
 * The queue of scheduled machine instance terminations is kept in increasing
 * order of termination time, such that instances closer to their termination
 * are kept at the head of the queue.
 * 
 * @see ScheduledTermination
 * 
 */
public class TerminationQueue {

	/**
	 * The queue of scheduled machine terminations ordered in increasing order
	 * of termination time (closest to termination is kept first) .
	 */
	private final PriorityQueue<ScheduledTermination> scheduledTerminations;

	/**
	 * Constructs a {@link TerminationQueue} with a given {@link TimeSource}.
	 * 
	 * @param timeSource
	 *            The {@link TimeSource} that provides the
	 *            {@link TerminationQueue} with the notion of current time.
	 */
	public TerminationQueue() {
		this.scheduledTerminations = new PriorityQueue<ScheduledTermination>();
	}

	/**
	 * Schedules a machine instance for (future) termination at a particular
	 * point in time.
	 * 
	 * @param scheduledTermination
	 *            The scheduled instance termination.
	 */
	public void add(ScheduledTermination scheduledTermination) {
		checkNotNull(scheduledTermination,
				"attempt to add a null scheduled termination");
		this.scheduledTerminations.add(scheduledTermination);
	}

	/**
	 * Schedules a set of machine instances for (future) termination.
	 * 
	 * @param scheduledTerminations
	 */
	public void addAll(Collection<ScheduledTermination> scheduledTerminations) {
		checkNotNull(scheduledTerminations, "null scheduled terminations");
		for (ScheduledTermination scheduledTermination : scheduledTerminations) {
			add(scheduledTermination);
		}
	}

	/**
	 * Returns the collection of machine instances that are currently scheduled
	 * for termination ordered in increasing order of termination time (earliest
	 * termination first).
	 * 
	 * @return
	 */
	public List<Machine> getQueuedInstances() {
		ScheduledTermination[] orderedArray = this.scheduledTerminations
				.toArray(new ScheduledTermination[0]);
		Arrays.sort(orderedArray);
		List<Machine> instances = Lists.newArrayList();
		for (ScheduledTermination scheduledTermination : orderedArray) {
			instances.add(scheduledTermination.getInstance());
		}
		return instances;
	}

	/**
	 * Returns the number of instances currently scheduled for termination.
	 * 
	 * @return
	 */
	public int size() {
		return this.scheduledTerminations.size();
	}

	/**
	 * Spares a number of instances scheduled for termination by removing them
	 * from the termination queue. Instances are spared starting at the head of
	 * the queue, meaning that the instance with earliest termination time will
	 * be spared first.
	 * 
	 * @param numInstancesToSpare
	 *            The number of instances to spare from termination.
	 * @return The instances that were spared from termination.
	 * @throws IllegalArgumentException
	 */
	public List<ScheduledTermination> spare(long numInstancesToSpare)
			throws IllegalArgumentException {
		checkArgument(numInstancesToSpare >= 0,
				"numInstancesToSpare must be non-negative");
		checkArgument(numInstancesToSpare <= size(),
				"Requested %s termination-marked instances "
						+ "to be spared but there are only "
						+ "%s termination-marked instances.",
				numInstancesToSpare, size());

		List<ScheduledTermination> sparedInstances = Lists.newArrayList();
		for (int i = 0; i < numInstancesToSpare; i++) {
			ScheduledTermination sparedInstance = this.scheduledTerminations
					.poll();
			sparedInstances.add(sparedInstance);
		}
		return sparedInstances;
	}

	/**
	 * Dequeues all {@link ScheduledTermination}'s for which termination is due.
	 * 
	 * @return The list of {@link ScheduledTermination}s that are due.
	 */
	public List<ScheduledTermination> popOverdueInstances() {
		List<ScheduledTermination> effectuatedTerminations = Lists
				.newArrayList();
		DateTime now = UtcTime.now();
		ScheduledTermination[] orderedArray = this.scheduledTerminations
				.toArray(new ScheduledTermination[0]);
		Arrays.sort(orderedArray);
		for (ScheduledTermination nextInstanceTermination : orderedArray) {
			if (now.isBefore(nextInstanceTermination.getTerminationTime())) {
				break;
			}
			effectuatedTerminations.add(nextInstanceTermination);
			this.scheduledTerminations.remove(nextInstanceTermination);
		}
		return effectuatedTerminations;
	}

	/**
	 * Filters out {@link ScheduledTermination}s from the
	 * {@link TerminationQueue} for any {@link Machine}s that are no longer part
	 * of the instance pool.
	 * <p/>
	 * This method prevents obsolete {@link Machine}s that, for example have
	 * been terminated by a third party, from occupying a spot in the
	 * {@link TerminationQueue}.
	 * 
	 * @param poolMembers
	 *            The set of {@link Machine}s that are in the machine pool and
	 *            hence are to be kept in the {@link TerminationQueue}.
	 */
	public void filter(Collection<Machine> poolMembers) {
		List<ScheduledTermination> terminations = Lists
				.newArrayList(this.scheduledTerminations);
		for (ScheduledTermination termination : terminations) {
			if (!poolMembers.contains(termination.getInstance())) {
				this.scheduledTerminations.remove(termination);
			}
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.scheduledTerminations);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TerminationQueue) {
			TerminationQueue that = TerminationQueue.class.cast(obj);
			return Objects.equal(this.scheduledTerminations,
					that.scheduledTerminations);
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (ScheduledTermination scheduledTermination : this.scheduledTerminations) {
			sb.append(" " + scheduledTermination.getInstance().getId() + ":"
					+ scheduledTermination.getTerminationTime());
		}
		sb.append(" ]");
		return sb.toString();
	}
}
