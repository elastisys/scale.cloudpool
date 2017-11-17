package com.elastisys.scale.cloudpool.commons.basepool;

import static java.lang.String.format;

import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics;
import com.elastisys.scale.commons.net.alerter.Alert;

public class IsAttachAlert extends TypeSafeMatcher<Alert> {

    private final String machineId;

    public IsAttachAlert(String machineId) {
        this.machineId = machineId;
    }

    @Override
    public boolean matchesSafely(Alert someAlert) {
        String messagePattern = format("Attached machine %s", this.machineId);
        return Objects.equals(AlertTopics.RESIZE.name(), someAlert.getTopic())
                && someAlert.getMessage().contains(messagePattern);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("attach alert for %s", this.machineId));
    }

    @Factory
    public static <T> Matcher<Alert> isAttachAlert(String machineId) {
        return new IsAttachAlert(machineId);
    }
}