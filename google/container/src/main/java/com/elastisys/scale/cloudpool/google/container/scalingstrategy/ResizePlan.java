package com.elastisys.scale.cloudpool.google.container.scalingstrategy;

import java.net.URL;
import java.util.Map;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * The output from a {@link ScalingStrategy}, which describes the target sizes
 * to set for each of the instance groups that comprise a certain container
 * cluster.
 *
 * @see ScalingStrategy
 */
public class ResizePlan {

    /**
     * A map where keys are instance group URLs and values are the target sizes
     * that is to be assigned to a certain instance group.
     */
    private final Map<URL, Integer> instanceGroupTargetSizes;

    /**
     * A map where keys are instance group URLs and values are the target sizes
     * that is to be assigned to a certain instance group.
     *
     * @param instanceGroupUrls
     */
    public ResizePlan(Map<URL, Integer> instanceGroupTargetSizes) {
        this.instanceGroupTargetSizes = instanceGroupTargetSizes;
    }

    /**
     * A map where keys are instance group URLs and values are the target sizes
     * that is to be assigned to a certain instance group.
     *
     * @return
     */
    public Map<URL, Integer> getInstanceGroupTargetSizes() {
        return this.instanceGroupTargetSizes;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
