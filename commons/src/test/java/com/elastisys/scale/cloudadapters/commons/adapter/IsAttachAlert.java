package com.elastisys.scale.cloudadapters.commons.adapter;

import static com.google.common.base.Objects.equal;
import static java.lang.String.format;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.elastisys.scale.cloudadapters.commons.adapter.alerts.AlertTopics;
import com.elastisys.scale.commons.net.smtp.alerter.Alert;

public class IsAttachAlert extends TypeSafeMatcher<Alert> {

	private final String machineId;

	public IsAttachAlert(String machineId) {
		this.machineId = machineId;
	}

	@Override
	public boolean matchesSafely(Alert someAlert) {
		String messagePattern = format("Attached machine %s", this.machineId);
		return equal(AlertTopics.RESIZE.name(), someAlert.getTopic())
				&& someAlert.getMessage().contains(messagePattern);
	}

	@Override
	public void describeTo(Description description) {
		description.appendText(String.format("attach alert for %s",
				this.machineId));
	}

	@Factory
	public static <T> Matcher<Alert> isAttachAlert(String machineId) {
		return new IsAttachAlert(machineId);
	}
}