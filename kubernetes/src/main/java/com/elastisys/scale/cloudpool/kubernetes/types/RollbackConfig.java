package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * See https://kubernetes.io/docs/api-reference/v1.5/#rollbackconfig-v1beta1
 *
 */
public class RollbackConfig {
    public Integer revision;

    @Override
    public int hashCode() {
        return Objects.hash(this.revision);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RollbackConfig) {
            RollbackConfig that = (RollbackConfig) obj;
            return Objects.equals(this.revision, that.revision);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
