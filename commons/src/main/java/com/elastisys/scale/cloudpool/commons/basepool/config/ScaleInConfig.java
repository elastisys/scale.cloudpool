package com.elastisys.scale.cloudpool.commons.basepool.config;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Objects;
import com.google.common.collect.Range;

/**
 * The section of a {@link BaseCloudPoolConfig} that describes how to
 * decommission servers (on scale-in).
 *
 * @see BaseCloudPoolConfig
 */
public class ScaleInConfig {
	/** Policy for selecting which server to terminate. */
	private final VictimSelectionPolicy victimSelectionPolicy;

	/**
	 * How many seconds prior to the next instance hour an acquired machine
	 * instance should be scheduled for termination. This should be set to a
	 * conservative and safe value to prevent the machine from being billed for
	 * an additional hour. A value of zero is used to specify immediate
	 * termination when a scale-down is ordered.
	 */
	private final Integer instanceHourMargin;

	/**
	 * Creates a new {@link ScaleInConfig}.
	 * 
	 * @param victimSelectionPolicy
	 *            Policy for selecting which server to terminate.
	 * @param instanceHourMargin
	 *            How many seconds prior to the next instance hour an acquired
	 *            machine instance should be scheduled for termination. This
	 *            should be set to a conservative and safe value to prevent the
	 *            machine from being billed for an additional hour. A value of
	 *            zero is used to specify immediate termination when a
	 *            scale-down is ordered.
	 */
	public ScaleInConfig(VictimSelectionPolicy victimSelectionPolicy,
			int instanceHourMargin) {
		this.victimSelectionPolicy = victimSelectionPolicy;
		this.instanceHourMargin = instanceHourMargin;
	}

	/**
	 * Policy for selecting which server to terminate.
	 *
	 * @return
	 */
	public VictimSelectionPolicy getVictimSelectionPolicy() {
		return this.victimSelectionPolicy;
	}

	/**
	 * How many seconds prior to the next instance hour an acquired machine
	 * instance should be scheduled for termination. This should be set to a
	 * conservative and safe value to prevent the machine from being billed for
	 * an additional hour. A value of zero is used to specify immediate
	 * termination when a scale-down is ordered.
	 *
	 * @return
	 */
	public Integer getInstanceHourMargin() {
		return this.instanceHourMargin;
	}

	public void validate() throws CloudPoolException {
		try {
			checkNotNull(this.victimSelectionPolicy,
					"victim selection policy cannot be null");
			checkArgument(
					Range.closedOpen(0, 3600).contains(this.instanceHourMargin),
					"instance hour margin must be in interval [0, 3600)");

		} catch (Exception e) {
			throw new CloudPoolException(
					format("failed to validate scaleDownConfig: %s",
							e.getMessage()),
					e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.victimSelectionPolicy,
				this.instanceHourMargin);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ScaleInConfig) {
			ScaleInConfig that = (ScaleInConfig) obj;
			return equal(this.victimSelectionPolicy, that.victimSelectionPolicy)
					&& equal(this.instanceHourMargin, that.instanceHourMargin);
		}
		return false;
	}

	@Override
	public String toString() {
		return JsonUtils.toPrettyString(JsonUtils.toJson(this, true));
	}
}