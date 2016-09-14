package com.elastisys.scale.cloudpool.commons.basepool;

import static com.google.common.base.Objects.equal;
import static java.lang.String.format;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics;
import com.elastisys.scale.commons.net.alerter.Alert;

public class IsSetServiceStateAlert extends TypeSafeMatcher<Alert> {

    private final String machineId;
    private final ServiceState serviceState;

    public IsSetServiceStateAlert(String machineId, ServiceState serviceState) {
        this.machineId = machineId;
        this.serviceState = serviceState;
    }

    @Override
    public boolean matchesSafely(Alert someAlert) {
        String messagePattern = format("Service state set to %s for machine %s", this.serviceState, this.machineId);
        return equal(AlertTopics.SERVICE_STATE.name(), someAlert.getTopic())
                && someAlert.getMessage().contains(messagePattern);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("service state alert (%s, %s)", this.machineId, this.serviceState));
    }

    @Factory
    public static <T> Matcher<Alert> isSetServiceStateAlert(String machineId, ServiceState serviceState) {
        return new IsSetServiceStateAlert(machineId, serviceState);
    }
}