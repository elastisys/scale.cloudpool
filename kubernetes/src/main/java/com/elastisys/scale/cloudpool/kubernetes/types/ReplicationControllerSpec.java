package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * See
 * https://kubernetes.io/docs/api-reference/v1.5/#replicationcontrollerspec-v1
 *
 */
public class ReplicationControllerSpec {

    public Integer minReadySeconds;
    public Integer replicas;
    public Map<String, String> selector;
    public PodTemplateSpec template;

    /**
     * Returns a string representation of the label selector as a
     * comma-separated list of {@code key=value} pairs.
     *
     * @return
     */
    public String getLabelSelectorExpression() {
        List<String> selectors = new ArrayList<>();
        for (Entry<String, String> selector : this.selector.entrySet()) {
            selectors.add(String.format("%s=%s", selector.getKey(), selector.getValue()));
        }
        return String.join(",", selectors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.minReadySeconds, this.replicas, this.selector, this.template);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ReplicationControllerSpec) {
            ReplicationControllerSpec that = (ReplicationControllerSpec) obj;
            return Objects.equals(this.minReadySeconds, that.minReadySeconds) //
                    && Objects.equals(this.replicas, that.replicas) //
                    && Objects.equals(this.selector, that.selector) //
                    && Objects.equals(this.template, that.template);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

}
