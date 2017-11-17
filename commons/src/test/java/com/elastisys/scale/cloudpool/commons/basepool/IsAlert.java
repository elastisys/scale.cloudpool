package com.elastisys.scale.cloudpool.commons.basepool;

import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;

public class IsAlert extends TypeSafeMatcher<Alert> {

    private final String topic;
    private final AlertSeverity severity;

    public IsAlert(String topic, AlertSeverity severity) {
        this.topic = topic;
        this.severity = severity;
    }

    @Override
    public boolean matchesSafely(Alert someAlert) {
        return Objects.equals(this.topic, someAlert.getTopic())
                && Objects.equals(this.severity, someAlert.getSeverity());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("alert with topic '%s' and severity '%s'", this.topic, this.severity));
    }

    @Factory
    public static <T> Matcher<Alert> isAlert(String topic, AlertSeverity severity) {
        return new IsAlert(topic, severity);
    }
}