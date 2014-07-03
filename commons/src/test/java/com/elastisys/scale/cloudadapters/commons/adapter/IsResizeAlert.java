package com.elastisys.scale.cloudadapters.commons.adapter;

import static com.google.common.base.Objects.equal;
import static java.lang.String.format;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;

import com.elastisys.scale.cloudadapters.commons.adapter.alerts.AlertTopics;
import com.elastisys.scale.commons.net.smtp.alerter.Alert;

public class IsResizeAlert extends TypeSafeMatcher<Alert> {

	private final int oldSize;
	private final int newSize;

	public IsResizeAlert(int oldSize, int newSize) {
		this.oldSize = oldSize;
		this.newSize = newSize;
	}

	@Override
	public boolean matchesSafely(Alert someAlert) {
		String changePattern = format("changed from %d to %d", this.oldSize,
				this.newSize);
		return equal(AlertTopics.RESIZE.name(), someAlert.getTopic())
				&& someAlert.getMessage().contains(changePattern);
	}

	@Override
	public void describeTo(Description description) {
		description.appendText(String.format(
				"scaling group resize alert from %d to %d", this.oldSize,
				this.newSize));
	}

	@Factory
	public static <T> Matcher<Alert> isResizeAlert(int oldSize, int newSize) {
		return new IsResizeAlert(oldSize, newSize);
	}
}