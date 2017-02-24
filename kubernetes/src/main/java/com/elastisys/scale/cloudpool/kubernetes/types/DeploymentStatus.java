package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.List;
import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * See https://kubernetes.io/docs/api-reference/v1.5/#deploymentstatus-v1beta1
 *
 */
public class DeploymentStatus {
    public Integer availableReplicas;
    public List<JsonObject> conditions;
    public Integer observedGeneration;
    public Integer replicas;
    public Integer unavailableReplicas;
    public Integer updatedReplicas;

    @Override
    public int hashCode() {
        return Objects.hash(this.availableReplicas, this.conditions, this.observedGeneration, this.replicas,
                this.unavailableReplicas, this.updatedReplicas);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DeploymentStatus) {
            DeploymentStatus that = (DeploymentStatus) obj;
            return Objects.equals(this.availableReplicas, that.availableReplicas) //
                    && Objects.equals(this.conditions, that.conditions) //
                    && Objects.equals(this.observedGeneration, that.observedGeneration) //
                    && Objects.equals(this.replicas, that.replicas) //
                    && Objects.equals(this.unavailableReplicas, that.unavailableReplicas) //
                    && Objects.equals(this.updatedReplicas, that.updatedReplicas);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

}
