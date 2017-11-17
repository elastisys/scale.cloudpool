package com.elastisys.scale.cloudpool.aws.spot.driver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.elastisys.scale.cloudpool.aws.spot.driver.alerts.AlertTopics;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class IsCancelAlert extends TypeSafeMatcher<Alert> {

    private final List<String> spotRequestIds;

    public IsCancelAlert(List<String> spotRequestIds) {
        this.spotRequestIds = spotRequestIds;
    }

    @Override
    public boolean matchesSafely(Alert someAlert) {
        if (!Objects.equals(AlertTopics.SPOT_REQUEST_CANCELLATION.name(), someAlert.getTopic())) {
            return false;
        }
        for (String spotRequestId : this.spotRequestIds) {
            Map<String, JsonElement> alertTags = someAlert.getMetadata();
            if (alertTags == null || !alertTags.containsKey("cancelledRequests")) {
                return false;
            }
            JsonArray requested = alertTags.get("cancelledRequests").getAsJsonArray();
            if (!requested.contains(new JsonPrimitive(spotRequestId))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("cancellation alert for %s", this.spotRequestIds));
    }

    @Factory
    public static <T> Matcher<Alert> isCancelAlert(String... spotRequestIds) {
        return new IsCancelAlert(Arrays.asList(spotRequestIds));
    }
}