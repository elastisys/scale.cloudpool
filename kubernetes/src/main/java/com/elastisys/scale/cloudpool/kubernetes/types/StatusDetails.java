package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.List;
import java.util.Objects;

import com.google.gson.JsonObject;

/**
 * See https://kubernetes.io/docs/api-reference/v1.5/#statusdetails-unversioned
 */
public class StatusDetails {

    public List<JsonObject> causes;
    public String group;
    public String kind;
    public String name;
    public Integer retryAfterSeconds;

    @Override
    public int hashCode() {
        return Objects.hash(this.causes, this.group, this.kind, this.name, this.retryAfterSeconds);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StatusDetails) {
            StatusDetails that = (StatusDetails) obj;
            return Objects.equals(this.causes, that.causes) //
                    && Objects.equals(this.group, that.group) //
                    && Objects.equals(this.kind, that.kind) //
                    && Objects.equals(this.name, that.name) //
                    && Objects.equals(this.retryAfterSeconds, that.retryAfterSeconds);
        }
        return false;
    }

}
