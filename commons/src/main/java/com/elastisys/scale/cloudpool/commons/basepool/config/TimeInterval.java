package com.elastisys.scale.cloudpool.commons.basepool.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.concurrent.TimeUnit;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Objects;

/**
 * Represents a duration of time. For example, "ten minutes".
 */
public class TimeInterval {

	/** The time value. For example, {@code 10}. */
	private final Long time;
	/**
	 * Time unit. For example, "minutes". Possible values given by
	 * {@link TimeUnit}.
	 */
	private final String unit;

	/**
	 * Creates a new {@link TimeInterval}.
	 *
	 * @param time
	 *            The time value. For example, {@code 10}.
	 * @param unit
	 *            Time unit. For example, "minutes".
	 */
	public TimeInterval(Long time, TimeUnit unit) {
		checkArgument(time != null, "null time");
		checkArgument(unit != null, "null time unit");
		this.time = time;
		this.unit = unit.name().toLowerCase();
		validate();
	}

	/**
	 * Creates a new {@link TimeInterval}.
	 *
	 * @param time
	 *            The time value. For example, {@code 10}.
	 * @param unit
	 *            Time unit. For example, "minutes". Possible values given by
	 *            {@link TimeUnit}.
	 */
	public TimeInterval(Long time, String unit) {
		checkArgument(time != null, "null time");
		checkArgument(unit != null, "null time unit");
		this.time = time;
		this.unit = unit.toLowerCase();
		validate();
	}

	/**
	 * The time value.
	 *
	 * @return
	 */
	public Long getTime() {
		return this.time;
	}

	/**
	 * The time unit. Possible values given by {@link TimeUnit}.
	 *
	 * @return
	 */
	public TimeUnit getUnit() {
		return TimeUnit.valueOf(this.unit.toUpperCase());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.time, this.unit);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TimeInterval) {
			TimeInterval that = (TimeInterval) obj;
			return Objects.equal(this.time, that.time)
					&& Objects.equal(this.unit, that.unit);
		}
		return false;
	}

	@Override
	public String toString() {
		return JsonUtils.toPrettyString(JsonUtils.toJson(this));
	}

	public void validate() {
		checkArgument(this.time >= 0, "time interval must be non-negative");
		// make sure the specified unit is permitted
		getUnit();
	}

}
