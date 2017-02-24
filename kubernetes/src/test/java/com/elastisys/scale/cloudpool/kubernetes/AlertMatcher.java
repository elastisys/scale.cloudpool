package com.elastisys.scale.cloudpool.kubernetes;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;

/**
 * Hamcrest matcher that will match that any {@link Alert}s with the specified
 * topic and severity.
 */
public class AlertMatcher extends TypeSafeMatcher<Alert> {

    private String expectedTopic;
    private AlertSeverity expectedSeverity;

    /**
     * Constructs an {@link AlertMatcher} that will match any
     * {@link AlertMessage}s with the specified topic and severity.
     *
     * @param expectedTopic
     * @param expectedSeverity
     */
    public AlertMatcher(String expectedTopic, AlertSeverity expectedSeverity) {
        this.expectedTopic = expectedTopic;
        this.expectedSeverity = expectedSeverity;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("of topic " + this.expectedTopic + " and severity " + this.expectedSeverity);
    }

    @Override
    public boolean matchesSafely(Alert message) {
        return this.expectedTopic.equals(message.getTopic()) && this.expectedSeverity == message.getSeverity();
    }

    /**
     * Constructs an {@link AlertMatcher} that will match any
     * {@link AlertMessage}s with the specified topic and severity.
     *
     * @param topic
     * @param severity
     * @return
     */
    @Factory
    public static <T> Matcher<Alert> alert(String topic, AlertSeverity severity) {
        return new AlertMatcher(topic, severity);
    }

    /**
     * Constructs an {@link AlertMatcher} that will match any
     * {@link AlertMessage}s with the specified topic and severity.
     *
     * @param topic
     * @param severity
     * @return
     */
    @Factory
    public static <T> Matcher<Alert> alertMessage(AlertTopics topic, AlertSeverity severity) {
        return new AlertMatcher(topic.name(), severity);
    }
}
