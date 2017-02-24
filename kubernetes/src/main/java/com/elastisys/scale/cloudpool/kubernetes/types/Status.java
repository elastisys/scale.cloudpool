package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * See https://kubernetes.io/docs/api-reference/v1.5/#status-unversioned
 *
 */
public class Status {
    public String apiVersion;
    public Integer code;
    public StatusDetails details;
    public String kind;
    public String message;
    public ListMeta metadata;
    public String reason;
    public String status;

    @Override
    public int hashCode() {
        return Objects.hash(this.apiVersion, this.code, this.details, this.kind, this.message, this.metadata,
                this.reason, this.status);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Status) {
            Status that = (Status) obj;
            return Objects.equals(this.apiVersion, that.apiVersion) //
                    && Objects.equals(this.code, that.code) //
                    && Objects.equals(this.details, that.details) //
                    && Objects.equals(this.kind, that.kind) //
                    && Objects.equals(this.message, that.message) //
                    && Objects.equals(this.metadata, that.metadata) //
                    && Objects.equals(this.reason, that.reason) //
                    && Objects.equals(this.status, that.status);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

}
