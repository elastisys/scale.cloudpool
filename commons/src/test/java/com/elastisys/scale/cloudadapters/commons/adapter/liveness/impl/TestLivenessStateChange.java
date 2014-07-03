package com.elastisys.scale.cloudadapters.commons.adapter.liveness.impl;

import static com.elastisys.scale.cloudadapters.commons.adapter.BaseAdapterTestUtils.machine;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.types.LivenessState;
import com.elastisys.scale.cloudadapters.commons.adapter.alerts.AlertTopics;
import com.elastisys.scale.commons.net.smtp.alerter.Alert;
import com.elastisys.scale.commons.net.smtp.alerter.AlertSeverity;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.Maps;

/**
 * Exercises the {@link LivenessStateChange} class.
 *
 * 
 *
 */
public class TestLivenessStateChange {

	@Before
	public void onSetup() {
		FrozenTime.setFixed(UtcTime.parse("2014-04-14T12:00:00.000Z"));
	}

	/**
	 * Verifiy getters.
	 */
	@Test
	public void fieldAccess() {
		LivenessStateChange change = new LivenessStateChange(machine("i-1"),
				LivenessState.BOOTING, LivenessState.LIVE);
		assertThat(change.getMachine(), is(machine("i-1")));
		assertThat(change.getPreviousState(), is(LivenessState.BOOTING));
		assertThat(change.getNewState(), is(LivenessState.LIVE));

	}

	/**
	 * Test creation of all valid combinations of from-state and to-state.
	 */
	@Test
	public void creation() {
		// null -> *
		new LivenessStateChange(machine("i-1"), null, LivenessState.UNKNOWN);
		new LivenessStateChange(machine("i-1"), null, LivenessState.BOOTING);
		new LivenessStateChange(machine("i-1"), null, LivenessState.UNHEALTHY);
		new LivenessStateChange(machine("i-1"), null, LivenessState.LIVE);

		// UNKNOWN -> *
		new LivenessStateChange(machine("i-1"), LivenessState.UNKNOWN,
				LivenessState.BOOTING);
		new LivenessStateChange(machine("i-1"), LivenessState.UNKNOWN,
				LivenessState.UNHEALTHY);
		new LivenessStateChange(machine("i-1"), LivenessState.UNKNOWN,
				LivenessState.LIVE);

		// UNHEALTHY -> *
		new LivenessStateChange(machine("i-1"), LivenessState.UNHEALTHY,
				LivenessState.BOOTING);
		new LivenessStateChange(machine("i-1"), LivenessState.UNHEALTHY,
				LivenessState.UNKNOWN);
		new LivenessStateChange(machine("i-1"), LivenessState.UNHEALTHY,
				LivenessState.LIVE);

		// BOOTING -> *
		new LivenessStateChange(machine("i-1"), LivenessState.BOOTING,
				LivenessState.UNKNOWN);
		new LivenessStateChange(machine("i-1"), LivenessState.BOOTING,
				LivenessState.UNHEALTHY);
		new LivenessStateChange(machine("i-1"), LivenessState.BOOTING,
				LivenessState.LIVE);

		// LIVE -> *
		new LivenessStateChange(machine("i-1"), LivenessState.LIVE,
				LivenessState.BOOTING);
		new LivenessStateChange(machine("i-1"), LivenessState.LIVE,
				LivenessState.UNHEALTHY);
		new LivenessStateChange(machine("i-1"), LivenessState.LIVE,
				LivenessState.UNKNOWN);
	}

	@Test
	public void toAlert() {
		LivenessStateChange change = new LivenessStateChange(machine("i-1"),
				LivenessState.BOOTING, LivenessState.LIVE);
		Alert alert = change.toAlert();
		String expectedMessage = "a liveness state change occurred for machine \"i-1\"";
		assertThat(alert.getMessage(), is(expectedMessage));
		assertThat(alert.getTopic(), is(AlertTopics.LIVENESS.name()));
		assertThat(alert.getSeverity(), is(AlertSeverity.INFO));
		assertThat(alert.getTimestamp(), is(UtcTime.now()));

		Map<String, String> expectedTags = Maps.newHashMap();
		expectedTags.put("machine", "i-1");
		expectedTags.put("previousState", LivenessState.BOOTING.name());
		expectedTags.put("newState", LivenessState.LIVE.name());
		assertThat(alert.getTags().size(), is(3));
		assertThat(alert.getTags(), is(expectedTags));
	}

	/**
	 * Verify that alerts of a UNHEALTY state are created with severity WARN.
	 */
	@Test
	public void toAlertSeverity() {
		// state changes that are of severity INFO
		assertThat(new LivenessStateChange(machine("i-1"), null,
				LivenessState.BOOTING).toAlert().getSeverity(),
				is(AlertSeverity.INFO));
		assertThat(new LivenessStateChange(machine("i-1"), null,
				LivenessState.LIVE).toAlert().getSeverity(),
				is(AlertSeverity.INFO));
		assertThat(new LivenessStateChange(machine("i-1"),
				LivenessState.UNKNOWN, LivenessState.BOOTING).toAlert()
				.getSeverity(), is(AlertSeverity.INFO));
		assertThat(new LivenessStateChange(machine("i-1"),
				LivenessState.UNKNOWN, LivenessState.LIVE).toAlert()
				.getSeverity(), is(AlertSeverity.INFO));
		assertThat(new LivenessStateChange(machine("i-1"),
				LivenessState.BOOTING, LivenessState.LIVE).toAlert()
				.getSeverity(), is(AlertSeverity.INFO));

		// state changes that are of severity NOTICE
		assertThat(new LivenessStateChange(machine("i-1"),
				LivenessState.UNHEALTHY, LivenessState.LIVE).toAlert()
				.getSeverity(), is(AlertSeverity.NOTICE));

		// state changes that are of severity WARN
		assertThat(new LivenessStateChange(machine("i-1"), null,
				LivenessState.UNHEALTHY).toAlert().getSeverity(),
				is(AlertSeverity.WARN));
		assertThat(new LivenessStateChange(machine("i-1"),
				LivenessState.UNKNOWN, LivenessState.UNHEALTHY).toAlert()
				.getSeverity(), is(AlertSeverity.WARN));
		assertThat(new LivenessStateChange(machine("i-1"),
				LivenessState.BOOTING, LivenessState.UNHEALTHY).toAlert()
				.getSeverity(), is(AlertSeverity.WARN));
		assertThat(new LivenessStateChange(machine("i-1"), LivenessState.LIVE,
				LivenessState.UNHEALTHY).toAlert().getSeverity(),
				is(AlertSeverity.WARN));
		assertThat(new LivenessStateChange(machine("i-1"), null,
				LivenessState.UNKNOWN).toAlert().getSeverity(),
				is(AlertSeverity.WARN));
		assertThat(new LivenessStateChange(machine("i-1"),
				LivenessState.BOOTING, LivenessState.UNKNOWN).toAlert()
				.getSeverity(), is(AlertSeverity.WARN));
		assertThat(new LivenessStateChange(machine("i-1"), LivenessState.LIVE,
				LivenessState.UNKNOWN).toAlert().getSeverity(),
				is(AlertSeverity.WARN));

	}

	@Test(expected = IllegalArgumentException.class)
	public void nullMachine() {
		new LivenessStateChange(null, LivenessState.UNKNOWN,
				LivenessState.BOOTING);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullNewState() {
		new LivenessStateChange(machine("i-1"), LivenessState.UNKNOWN, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void identicalPreviousAndNewState() {
		new LivenessStateChange(machine("i-1"), LivenessState.LIVE,
				LivenessState.LIVE);
	}

}
