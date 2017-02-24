package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.List;
import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * See
 * https://kubernetes.io/docs/api-reference/v1.5/#replicationcontrollerstatus-v1
 *
 */
public class ReplicationControllerStatus {
    public Integer availableReplicas;
    public List<JsonObject> conditions;
    public Integer fullyLabeledReplicas;
    public Integer observedGeneration;
    public Integer readyReplicas;
    public Integer replicas;

    @Override
    public int hashCode() {
        return Objects.hash(this.availableReplicas, this.conditions, this.fullyLabeledReplicas, this.observedGeneration,
                this.readyReplicas, this.replicas);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ReplicationControllerStatus) {
            ReplicationControllerStatus that = (ReplicationControllerStatus) obj;
            return Objects.equals(this.availableReplicas, that.availableReplicas) //
                    && Objects.equals(this.conditions, that.conditions) //
                    && Objects.equals(this.fullyLabeledReplicas, that.fullyLabeledReplicas) //
                    && Objects.equals(this.observedGeneration, that.observedGeneration) //
                    && Objects.equals(this.readyReplicas, that.readyReplicas) //
                    && Objects.equals(this.replicas, that.replicas);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
