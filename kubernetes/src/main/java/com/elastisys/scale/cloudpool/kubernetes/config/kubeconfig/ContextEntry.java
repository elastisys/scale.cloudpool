package com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a context entry within a {@link KubeConfig}.
 */
public class ContextEntry {
    /**
     * A name which acts as a dictionary key for the context within this
     * kubeconfig file.
     */
    @JsonProperty("name")
    String name;
    /**
     * Context is a tuple of references to a cluster (how do I communicate with
     * a kubernetes cluster), a user (how do I identify myself), and a namespace
     * (what subset of resources do I want to work with).
     */
    @JsonProperty("context")
    Context context;

    /**
     * A name which acts as a dictionary key for the context within this
     * kubeconfig file.
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * Context is a tuple of references to a cluster (how do I communicate with
     * a kubernetes cluster), a user (how do I identify myself), and a namespace
     * (what subset of resources do I want to work with).
     *
     * @return
     */
    public Context getContext() {
        return this.context;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.context);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ContextEntry) {
            ContextEntry that = (ContextEntry) obj;
            return Objects.equals(this.name, that.name) //
                    && Objects.equals(this.context, that.context);
        }
        return false;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.name != null, "contexts: context entry missing name field");
        checkArgument(this.context != null, "contexts: context entry '%s' missing context field", this.name);
        try {
            this.context.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("contexts: context entry '%s': invalid context: %s", this.name, e.getMessage()), e);
        }
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
