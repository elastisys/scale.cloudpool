package com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a cluster entry in a {@link KubeConfig}.
 */
public class ClusterEntry {
    /**
     * A name which acts as a dictionary key for the cluster within this
     * kubeconfig file.
     */
    @JsonProperty("name")
    String name;

    /** Endpoint data for a kubernetes cluster. */
    @JsonProperty("cluster")
    Cluster cluster;

    /**
     * A name which acts as a dictionary key for the cluster within this
     * kubeconfig file.
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * Endpoint data for a kubernetes cluster.
     *
     * @return
     */
    public Cluster getCluster() {
        return this.cluster;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.cluster);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClusterEntry) {
            ClusterEntry that = (ClusterEntry) obj;
            return Objects.equals(this.name, that.name) //
                    && Objects.equals(this.cluster, that.cluster);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.name != null, "clusters: cluster entry missing name field");
        checkArgument(this.cluster != null, "clusters: cluster entry '%s' missing cluster field", this.name);

        try {
            this.cluster.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("clusters: cluster entry '%s': %s", this.name, e.getMessage()), e);
        }
    }
}
