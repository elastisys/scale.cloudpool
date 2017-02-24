package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.List;
import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * See https://kubernetes.io/docs/api-reference/v1.5/#podlist-v1
 *
 */
public class PodList {

    public String apiVersion;
    public List<Pod> items;
    public String kind;
    public ListMeta metadata;

    @Override
    public int hashCode() {
        return Objects.hash(this.apiVersion, this.items, this.kind, this.metadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PodList) {
            PodList that = (PodList) obj;
            return Objects.equals(this.apiVersion, that.apiVersion) //
                    && Objects.equals(this.items, that.items) //
                    && Objects.equals(this.kind, that.kind) //
                    && Objects.equals(this.metadata, that.metadata);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
