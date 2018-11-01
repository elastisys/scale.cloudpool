package com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a user entry in a {@link KubeConfig}.
 */
public class UserEntry {

    /**
     * A name which acts as a dictionary key for the user within this kubeconfig
     * file.
     */
    @JsonProperty("name")
    String name;
    /** Client credentials for authenticating to a kubernetes cluster. */
    @JsonProperty("user")
    User user;

    /**
     * A name which acts as a dictionary key for the user within this kubeconfig
     * file.
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * Client credentials for authenticating to a kubernetes cluster.
     *
     * @return
     */
    public User getUser() {
        return this.user;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.user);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UserEntry) {
            UserEntry that = (UserEntry) obj;
            return Objects.equals(this.name, that.name) //
                    && Objects.equals(this.user, that.user);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.name != null, "users: user entry missing name field");
        checkArgument(this.user != null, "users: user entry '%s' missing user field", this.name);
        try {
            this.user.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("users: invalid user '%s': %s", this.name, e.getMessage()),
                    e);
        }
    }
}
