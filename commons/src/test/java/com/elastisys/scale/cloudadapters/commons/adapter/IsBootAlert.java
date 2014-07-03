package com.elastisys.scale.cloudadapters.commons.adapter;

import static com.google.common.base.Objects.equal;

import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.commons.adapter.alerts.AlertTopics;
import com.elastisys.scale.commons.net.smtp.alerter.Alert;

public class IsBootAlert extends TypeSafeMatcher<Alert> {

	private final Machine machine;

	public IsBootAlert(Machine machine) {
		this.machine = machine;
	}

	@Override
	public boolean matchesSafely(Alert someAlert) {
		Map<String, String> tags = someAlert.getTags();
		return equal(AlertTopics.LIVENESS.name(), someAlert.getTopic())
				&& tags.get("machine").equals(this.machine.getId())
				&& tags.get("previousState").equals("null")
				&& tags.get("newState").equals("BOOTING");
	}

	@Override
	public void describeTo(Description description) {
		description.appendText(String.format(
				"boot liveness state change for machine '%s'",
				this.machine.getId()));
	}

	@Factory
	public static <T> Matcher<Alert> isBootAlert(Machine forMachine) {
		return new IsBootAlert(forMachine);
	}
}