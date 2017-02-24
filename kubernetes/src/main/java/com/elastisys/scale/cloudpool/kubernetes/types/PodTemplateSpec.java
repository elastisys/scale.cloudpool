package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * See https://kubernetes.io/docs/api-reference/v1.5/#podtemplatespec-v1
 *
 */
public class PodTemplateSpec {
    public ObjectMeta metadata;
    public PodSpec spec;

    @Override
    public int hashCode() {
        return Objects.hash(this.metadata, this.spec);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PodTemplateSpec) {
            PodTemplateSpec that = (PodTemplateSpec) obj;
            return Objects.equals(this.metadata, that.metadata) //
                    && Objects.equals(this.spec, that.spec);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
