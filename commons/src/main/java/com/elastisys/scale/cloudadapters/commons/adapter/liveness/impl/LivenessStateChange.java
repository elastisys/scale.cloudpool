package com.elastisys.scale.cloudadapters.commons.adapter.liveness.impl;

import static com.elastisys.scale.cloudadapers.api.types.LivenessState.LIVE;
import static com.elastisys.scale.cloudadapers.api.types.LivenessState.UNHEALTHY;
import static com.elastisys.scale.cloudadapers.api.types.LivenessState.UNKNOWN;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;

import java.util.Map;

import com.elastisys.scale.cloudadapers.api.types.LivenessState;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapter;
import com.elastisys.scale.cloudadapters.commons.adapter.alerts.AlertTopics;
import com.elastisys.scale.commons.net.smtp.alerter.Alert;
import com.elastisys.scale.commons.net.smtp.alerter.AlertSeverity;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;

/**
 * Represents a liveness state change for a particular {@link Machine}.
 * <p/>
 * Used by the {@link NotifyingLivenessTracker} to report observed
 * {@link LivenessState} changes for {@link Machine}s on the
 * {@link BaseCloudAdapter}'s {@link EventBus}. To this end, the
 * {@link LivenessStateChange} contain a convenience method for turning it into
 * an {@link Alert},
 *
 * @see NotifyingLivenessTracker
 *
 * 
 *
 */
public class LivenessStateChange {

	private final Machine machine;
	private final LivenessState previousState;
	private final LivenessState newState;

	public LivenessStateChange(Machine machine, LivenessState previousState,
			LivenessState newState) {
		checkArgument(machine != null, "machine cannot be null");
		checkArgument(newState != null, "newState cannot be null");
		checkArgument(previousState != newState,
				"previousState is same as newState");
		this.machine = machine;
		this.previousState = previousState;
		this.newState = newState;
	}

	public Machine getMachine() {
		return this.machine;
	}

	public LivenessState getPreviousState() {
		return this.previousState;
	}

	public LivenessState getNewState() {
		return this.newState;
	}

	/**
	 * Converts this {@link LivenessStateChange} into an {@link Alert}
	 * that can be posted on the {@link BaseCloudAdapter}'s {@link EventBus} for
	 * notifying clients of liveness state changes.
	 *
	 * @return The corresponding {@link Alert}.
	 */
	public Alert toAlert() {
		AlertSeverity severity;
		if (asList(UNHEALTHY, UNKNOWN).contains(this.newState)) {
			// only transitions to -> UNHEALTHY/UNKNOWN are warning signs
			severity = AlertSeverity.WARN;
		} else {
			if (this.previousState == UNHEALTHY && this.newState == LIVE) {
				// going from an unhealthy state to a healthy state should
				// render a NOTICE
				severity = AlertSeverity.NOTICE;
			} else {
				severity = AlertSeverity.INFO;
			}
		}

		String message = String.format("a liveness state change "
				+ "occurred for machine \"%s\"", this.machine.getId());
		Map<String, String> tags = Maps.newHashMap();
		tags.put("machine", this.machine.getId());
		tags.put("previousState", String.valueOf(this.previousState));
		tags.put("newState", this.newState.name());

		return new Alert(AlertTopics.LIVENESS.name(), severity,
				UtcTime.now(), message, tags);
	}

	@Override
	public int hashCode() {
		return Objects
				.hashCode(this.machine, this.previousState, this.newState);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LivenessStateChange) {
			LivenessStateChange that = (LivenessStateChange) obj;
			return Objects.equal(this.machine, that.machine)
					&& Objects.equal(this.previousState, that.previousState)
					&& Objects.equal(this.newState, that.newState);
		}
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return String.format("{%s: %s -> %s}", this.machine.getId(),
				this.previousState, this.newState);
	};
}
