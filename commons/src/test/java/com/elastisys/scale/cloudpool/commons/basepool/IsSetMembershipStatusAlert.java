package com.elastisys.scale.cloudpool.commons.basepool;

import static java.lang.String.format;

import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics;
import com.elastisys.scale.commons.net.alerter.Alert;

public class IsSetMembershipStatusAlert extends TypeSafeMatcher<Alert> {

    private final String machineId;
    private final MembershipStatus membershipStatus;

    public IsSetMembershipStatusAlert(String machineId, MembershipStatus membershipStatus) {
        this.machineId = machineId;
        this.membershipStatus = membershipStatus;
    }

    @Override
    public boolean matchesSafely(Alert someAlert) {
        String messagePattern = format("Membership status set to %s for machine %s", this.membershipStatus,
                this.machineId);
        return Objects.equals(AlertTopics.MEMBERSHIP_STATUS.name(), someAlert.getTopic())
                && someAlert.getMessage().contains(messagePattern);
    }

    @Override
    public void describeTo(Description description) {
        description
                .appendText(String.format("membership status alert (%s, %s)", this.machineId, this.membershipStatus));
    }

    @Factory
    public static <T> Matcher<Alert> isMembershipStatusAlert(String machineId, MembershipStatus membershipStatus) {
        return new IsSetMembershipStatusAlert(machineId, membershipStatus);
    }
}