package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.List;
import java.util.Objects;

import org.joda.time.DateTime;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * See https://kubernetes.io/docs/api-reference/v1.5/#podstatus-v1
 *
 */
public class PodStatus {

    public List<JsonObject> conditions;
    public List<JsonObject> containerStatuses;
    public String hostIP;
    public String message;
    public String phase;
    public String podIP;
    public String reason;
    public DateTime startTime;

    @Override
    public int hashCode() {
        return Objects.hash(this.conditions, this.containerStatuses, this.hostIP, this.message, this.phase, this.podIP,
                this.reason, this.startTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PodStatus) {
            PodStatus that = (PodStatus) obj;
            return Objects.equals(this.conditions, that.conditions) //
                    && Objects.equals(this.containerStatuses, that.containerStatuses) //
                    && Objects.equals(this.hostIP, that.hostIP) //
                    && Objects.equals(this.message, that.message) //
                    && Objects.equals(this.phase, that.phase) //
                    && Objects.equals(this.podIP, that.podIP) //
                    && Objects.equals(this.reason, that.reason) //
                    && Objects.equals(this.startTime, that.startTime);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
