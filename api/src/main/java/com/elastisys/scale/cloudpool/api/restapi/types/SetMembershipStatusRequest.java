package com.elastisys.scale.cloudpool.api.restapi.types;

import java.util.Objects;

import com.elastisys.scale.cloudpool.api.restapi.CloudPoolRestApi;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * REST API request type that requests a certain membership status be set for a
 * particular machine in the pool.
 *
 * @see CloudPoolRestApi#setMembershipStatus(SetMembershipStatusRequest)
 */
public class SetMembershipStatusRequest {

    /** The machine for which to set membership status. */
    private final String machineId;

    /** Membership status to set for machine. */
    private final MembershipStatus membershipStatus;

    public SetMembershipStatusRequest(String machineId, MembershipStatus membershipStatus) {
        this.machineId = machineId;
        this.membershipStatus = membershipStatus;
    }

    public String getMachineId() {
        return this.machineId;
    }

    public MembershipStatus getMembershipStatus() {
        return this.membershipStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.machineId, this.membershipStatus);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SetMembershipStatusRequest) {
            SetMembershipStatusRequest that = (SetMembershipStatusRequest) obj;
            return Objects.equals(this.machineId, that.machineId) //
                    && Objects.equals(this.membershipStatus, that.membershipStatus);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
