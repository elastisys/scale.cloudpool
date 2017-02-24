package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * See https://kubernetes.io/docs/api-reference/v1.5/#replicaset-v1beta1
 *
 */
public class ReplicaSet {
    public String apiVersion;
    public String kind;
    public ObjectMeta metadata;
    public ReplicaSetSpec spec;
    public ReplicaSetStatus status;

    @Override
    public int hashCode() {
        return Objects.hash(this.apiVersion, this.kind, this.metadata, this.spec, this.status);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ReplicaSet) {
            ReplicaSet that = (ReplicaSet) obj;
            return Objects.equals(this.apiVersion, that.apiVersion) //
                    && Objects.equals(this.kind, that.kind) //
                    && Objects.equals(this.metadata, that.metadata) //
                    && Objects.equals(this.spec, that.spec) //
                    && Objects.equals(this.status, that.status);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

}
