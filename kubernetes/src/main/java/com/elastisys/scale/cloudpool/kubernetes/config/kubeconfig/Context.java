package com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.commons.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A {@link KubeConfig} context. A context is a tuple of references to a cluster
 * (how do I communicate with a kubernetes cluster), a user (how do I identify
 * myself), and a namespace (what subset of resources do I want to work with).
 *
 * See https://godoc.org/k8s.io/client-go/tools/clientcmd/api#Context
 */
public class Context {
    /** The default namespace when none is specified. */
    public static final String DEFAULT_NS = "default";

    /** The name of the cluster for this context. */
    @JsonProperty("cluster")
    String cluster;
    /**
     * The default namespace to use on unspecified requests. May be null.
     * Defaults to {@code default}.
     */
    @JsonProperty("namespace")
    String namespace;
    /** The name of the user for this context. */
    @JsonProperty("user")
    String user;

    /**
     * The name of the cluster for this context.
     *
     * @return
     */
    public String getCluster() {
        return this.cluster;
    }

    /**
     * The default namespace to use on unspecified requests.
     *
     * @return
     */
    public String getNamespace() {
        return Optional.ofNullable(this.namespace).orElse(DEFAULT_NS);
    }

    /**
     * The name of the user for this context.
     *
     * @return
     */
    public String getUser() {
        return this.user;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.cluster, this.namespace, this.user);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Context) {
            Context that = (Context) obj;
            return Objects.equals(this.cluster, that.cluster) //
                    && Objects.equals(this.namespace, that.namespace) //
                    && Objects.equals(this.user, that.user);
        }
        return false;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.cluster != null, "context: missing cluster field");
        checkArgument(this.user != null, "context: missing user field");
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
