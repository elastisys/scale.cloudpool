package com.elastisys.scale.cloudadapters.commons.adapter;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;

import com.elastisys.scale.commons.net.smtp.alerter.Alert;
import com.elastisys.scale.commons.net.smtp.alerter.AlertSeverity;
import com.google.common.base.Objects;

public class IsAlert extends TypeSafeMatcher<Alert> {

	private final String topic;
	private final AlertSeverity severity;

	public IsAlert(String topic, AlertSeverity severity) {
		this.topic = topic;
		this.severity = severity;
	}

	@Override
	public boolean matchesSafely(Alert someAlert) {
		return Objects.equal(this.topic, someAlert.getTopic())
				&& Objects.equal(this.severity, someAlert.getSeverity());
	}

	@Override
	public void describeTo(Description description) {
		description.appendText(String.format(
				"alert with topic '%s' and severity '%s'", this.topic,
				this.severity));
	}

	@Factory
	public static <T> Matcher<Alert> isAlert(String topic, AlertSeverity severity) {
		return new IsAlert(topic, severity);
	}
}