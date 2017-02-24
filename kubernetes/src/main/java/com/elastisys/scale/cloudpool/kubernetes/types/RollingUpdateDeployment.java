package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * See
 * https://kubernetes.io/docs/api-reference/v1.5/#rollingupdatedeployment-v1beta1
 */
public class RollingUpdateDeployment {
    public String maxSurge;
    public String maxUnavailable;

    @Override
    public int hashCode() {
        return Objects.hash(this.maxSurge, this.maxUnavailable);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RollingUpdateDeployment) {
            RollingUpdateDeployment that = (RollingUpdateDeployment) obj;
            return Objects.equals(this.maxSurge, that.maxSurge) //
                    && Objects.equals(this.maxUnavailable, that.maxUnavailable);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
