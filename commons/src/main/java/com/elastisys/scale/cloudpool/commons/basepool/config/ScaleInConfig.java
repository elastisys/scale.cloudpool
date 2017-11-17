package com.elastisys.scale.cloudpool.commons.basepool.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * The section of a {@link BaseCloudPoolConfig} that describes how to
 * decommission servers (on scale-in).
 *
 * @see BaseCloudPoolConfig
 */
public class ScaleInConfig {
    public static final VictimSelectionPolicy DEFAULT_VICTIM_SELECTION_POLICY = VictimSelectionPolicy.NEWEST;

    /** Policy for selecting which server to terminate. */
    private final VictimSelectionPolicy victimSelectionPolicy;

    /**
     * Creates a new {@link ScaleInConfig}.
     *
     * @param victimSelectionPolicy
     *            Policy for selecting which server to terminate.
     */
    public ScaleInConfig(VictimSelectionPolicy victimSelectionPolicy) {
        this.victimSelectionPolicy = victimSelectionPolicy;
    }

    /**
     * Policy for selecting which server to terminate.
     *
     * @return
     */
    public VictimSelectionPolicy getVictimSelectionPolicy() {
        return this.victimSelectionPolicy;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.victimSelectionPolicy != null, "scaleInConfig: missing victimSelectionPolicy");
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.victimSelectionPolicy);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScaleInConfig) {
            ScaleInConfig that = (ScaleInConfig) obj;
            return Objects.equals(this.victimSelectionPolicy, that.victimSelectionPolicy);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}