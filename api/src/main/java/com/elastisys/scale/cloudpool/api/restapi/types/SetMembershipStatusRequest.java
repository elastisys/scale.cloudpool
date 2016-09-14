package com.elastisys.scale.cloudpool.api.restapi.types;

import com.elastisys.scale.cloudpool.api.restapi.CloudPoolHandler;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.google.common.base.Objects;

/**
 * REST API request type that requests a certain membership status be set for a
 * machine in the pool.
 *
 * @see CloudPoolHandler#setMembershipStatus(String,
 *      com.elastisys.scale.cloudpool.api.restapi.SetMembershipStatusRequest)
 */
public class SetMembershipStatusRequest {

    /**
     * Membership status to set for instance.
     */
    private MembershipStatus membershipStatus;

    public SetMembershipStatusRequest(MembershipStatus membershipStatus) {
        this.membershipStatus = membershipStatus;
    }

    public MembershipStatus getMembershipStatus() {
        return this.membershipStatus;
    }

    public void setMembershipStatus(MembershipStatus membershipStatus) {
        this.membershipStatus = membershipStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.membershipStatus);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SetMembershipStatusRequest) {
            SetMembershipStatusRequest that = (SetMembershipStatusRequest) obj;
            return Objects.equal(this.membershipStatus, that.membershipStatus);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.membershipStatus.toString();
    }
}
