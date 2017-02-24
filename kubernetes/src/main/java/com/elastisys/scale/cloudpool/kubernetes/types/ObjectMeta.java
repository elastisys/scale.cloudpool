package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.joda.time.DateTime;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * See https://kubernetes.io/docs/api-reference/v1.5/#objectmeta-v1
 */
public class ObjectMeta {
    public Map<String, String> annotations;
    public String clusterName;
    public DateTime creationTimestamp;
    public Integer deletionGracePeriodSeconds;
    public DateTime deletionTimestamp;
    public List<String> finalizers;
    public String generateName;
    public Integer generation;
    public Map<String, String> labels;
    public String name;
    public String namespace;
    public List<JsonObject> ownerReferences;
    public String resourceVersion;
    public String selfLink;
    public String uid;

    @Override
    public int hashCode() {
        return Objects.hash(this.annotations, this.clusterName, this.creationTimestamp, this.deletionGracePeriodSeconds,
                this.deletionTimestamp, this.finalizers, this.generateName, this.generation, this.labels, this.name,
                this.namespace, this.ownerReferences, this.resourceVersion, this.selfLink, this.uid);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ObjectMeta) {
            ObjectMeta that = (ObjectMeta) obj;
            return Objects.equals(this.annotations, that.annotations) //
                    && Objects.equals(this.clusterName, that.clusterName) //
                    && Objects.equals(this.creationTimestamp, that.creationTimestamp) //
                    && Objects.equals(this.deletionGracePeriodSeconds, that.deletionGracePeriodSeconds) //
                    && Objects.equals(this.deletionTimestamp, that.deletionTimestamp) //
                    && Objects.equals(this.finalizers, that.finalizers) //
                    && Objects.equals(this.generateName, that.generateName) //
                    && Objects.equals(this.generation, that.generation) //
                    && Objects.equals(this.labels, that.labels) //
                    && Objects.equals(this.name, that.name) //
                    && Objects.equals(this.namespace, that.namespace) //
                    && Objects.equals(this.ownerReferences, that.ownerReferences) //
                    && Objects.equals(this.resourceVersion, that.resourceVersion) //
                    && Objects.equals(this.selfLink, that.selfLink) //
                    && Objects.equals(this.uid, that.uid);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
