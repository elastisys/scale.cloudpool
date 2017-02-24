package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * See https://kubernetes.io/docs/api-reference/v1.5/#deploymentstrategy-v1beta1
 *
 */
public class DeploymentStrategy {
    public RollingUpdateDeployment rollingUpdate;
    public String type;

    @Override
    public int hashCode() {
        return Objects.hash(this.rollingUpdate, this.type);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DeploymentStrategy) {
            DeploymentStrategy that = (DeploymentStrategy) obj;
            return Objects.equals(this.rollingUpdate, that.rollingUpdate) //
                    && Objects.equals(this.type, that.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

}
