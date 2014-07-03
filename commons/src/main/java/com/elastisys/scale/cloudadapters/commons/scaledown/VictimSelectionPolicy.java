package com.elastisys.scale.cloudadapters.commons.scaledown;

import com.elastisys.scale.cloudadapters.commons.scaledown.strategies.ClosestToInstanceHourVictimSelectionStrategy;
import com.elastisys.scale.cloudadapters.commons.scaledown.strategies.NewestInstanceVictimSelectionStrategy;
import com.elastisys.scale.cloudadapters.commons.scaledown.strategies.OldestInstanceVictimSelectionStrategy;

/**
 * The collection of valid victim selection policies that governs how to select
 * a machine instance to be terminated when a scale-down has been ordered.
 * <p/>
 * Each policy is paired with a {@link VictimSelectionStrategy} that implements
 * the selection algorithm for the policy.
 * 
 * @see VictimSelectionStrategy
 * 
 * 
 */
public enum VictimSelectionPolicy {
	/**
	 * Specify this if you want the oldest instance in your Auto Scaling group
	 * to be terminated.
	 */
	OLDEST_INSTANCE(OldestInstanceVictimSelectionStrategy.INSTANCE),
	/** Specify this if you want the last launched instance to be terminated. */
	NEWEST_INSTANCE(NewestInstanceVictimSelectionStrategy.INSTANCE),
	/**
	 * Specify this if you want the instance that is closest to completing its
	 * most recently started instance hour to be terminated.
	 */
	CLOSEST_TO_INSTANCE_HOUR(
			ClosestToInstanceHourVictimSelectionStrategy.INSTANCE);

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
	private VictimSelectionPolicy(
			VictimSelectionStrategy victimSelectionStrategy) {
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
