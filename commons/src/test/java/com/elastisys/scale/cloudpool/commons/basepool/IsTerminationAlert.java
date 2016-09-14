package com.elastisys.scale.cloudpool.commons.basepool;

import static com.google.common.base.Objects.equal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class IsTerminationAlert extends TypeSafeMatcher<Alert> {

    private final List<String> machineIds;

    public IsTerminationAlert(List<String> machineIds) {
        this.machineIds = machineIds;
    }

    @Override
    public boolean matchesSafely(Alert someAlert) {
        if (!equal(AlertTopics.RESIZE.name(), someAlert.getTopic())) {
            return false;
        }
        for (String machineId : this.machineIds) {
            Map<String, JsonElement> alertTags = someAlert.getMetadata();
            if (alertTags == null || !alertTags.containsKey("terminatedMachines")) {
                return false;
            }
            JsonArray terminated = alertTags.get("terminatedMachines").getAsJsonArray();
            if (!terminated.contains(new JsonPrimitive(machineId))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("termination alert for %s", this.machineIds));
    }

    @Factory
    public static <T> Matcher<Alert> isTerminationAlert(String... machineIds) {
        return new IsTerminationAlert(Arrays.asList(machineIds));
    }
}