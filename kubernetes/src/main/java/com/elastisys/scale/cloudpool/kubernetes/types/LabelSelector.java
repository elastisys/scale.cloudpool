package com.elastisys.scale.cloudpool.kubernetes.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * See https://kubernetes.io/docs/api-reference/v1.5/#labelselector-unversioned
 */
public class LabelSelector {
    public List<LabelSelectorRequirement> matchExpressions;
    public Map<String, String> matchLabels;

    /**
     * Converts this {@link LabelSelector} to a {@link String} that can be used
     * as <a href=
     * "https://kubernetes.io/docs/user-guide/labels/#label-selectors">label
     * selector</a> expression, for example when listing {@link Pod}s.
     *
     * @return
     */
    public String toLabelSelectorExpression() {
        List<String> selectors = new ArrayList<>();
        if (this.matchExpressions != null) {
            for (LabelSelectorRequirement labelSelector : this.matchExpressions) {
                selectors.add(labelSelector.toLabelSelector());
            }
        }
        if (this.matchLabels != null) {
            for (Entry<String, String> matchLabelEntry : this.matchLabels.entrySet()) {
                selectors.add(String.format("%s=%s", matchLabelEntry.getKey(), matchLabelEntry.getValue()));
            }
        }
        return String.join(",", selectors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.matchExpressions, this.matchLabels);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LabelSelector) {
            LabelSelector that = (LabelSelector) obj;
            return Objects.equals(this.matchExpressions, that.matchExpressions) //
                    && Objects.equals(this.matchLabels, that.matchLabels);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

}
