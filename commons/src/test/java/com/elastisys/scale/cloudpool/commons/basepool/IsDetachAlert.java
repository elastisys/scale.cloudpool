package com.elastisys.scale.cloudpool.commons.basepool;

import static java.lang.String.format;

import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics;
import com.elastisys.scale.commons.net.alerter.Alert;

public class IsDetachAlert extends TypeSafeMatcher<Alert> {

    private final String machineId;

    public IsDetachAlert(String machineId) {
        this.machineId = machineId;
    }

    @Override
    public boolean matchesSafely(Alert someAlert) {
        String messagePattern = format("Detached machine %s", this.machineId);
        return Objects.equals(AlertTopics.RESIZE.name(), someAlert.getTopic())
                && someAlert.getMessage().contains(messagePattern);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("detach alert for %s", this.machineId));
    }

    @Factory
    public static <T> Matcher<Alert> isDetachAlert(String machineId) {
        return new IsDetachAlert(machineId);
    }
}