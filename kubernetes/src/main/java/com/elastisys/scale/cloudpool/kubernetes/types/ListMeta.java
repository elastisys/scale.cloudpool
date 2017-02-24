package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * See https://kubernetes.io/docs/api-reference/v1.5/#listmeta-unversioned
 */
public class ListMeta {

    public String resourceVersion;
    public String selfLink;

    @Override
    public int hashCode() {
        return Objects.hash(this.resourceVersion, this.selfLink);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ListMeta) {
            ListMeta that = (ListMeta) obj;
            return Objects.equals(this.resourceVersion, that.resourceVersion) //
                    && Objects.equals(this.selfLink, that.selfLink);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
