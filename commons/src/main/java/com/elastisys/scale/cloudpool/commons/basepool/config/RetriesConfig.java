package com.elastisys.scale.cloudpool.commons.basepool.config;

import static com.google.common.base.Preconditions.checkArgument;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.google.common.base.Objects;

/**
 * Retry handling when fetching pool members from the cloud API fails.
 */
public class RetriesConfig {

	/**
	 * Maximum number of retries to make on each attempt to fetch pool members.
	 */
	private final int maxRetries;
	/**
	 * Initial delay to use in exponential back-off on retries. May be zero,
	 * which results in no delay between retries.
	 */
	private final TimeInterval initialBackoffDelay;

	/**
	 * Creates a {@link RetriesConfig}.
	 *
	 * @param maxRetries
	 *            Maximum number of retries to make on each attempt to fetch
	 *            pool members.
	 * @param initialBackoffDelay
	 *            Initial delay to use in exponential back-off on retries. May
	 *            be zero, which results in no delay between retries.
	 */
	public RetriesConfig(int maxRetries, TimeInterval initialBackoffDelay) {
		this.maxRetries = maxRetries;
		this.initialBackoffDelay = initialBackoffDelay;
	}

	/**
	 * Maximum number of retries to make on each attempt to fetch pool members.
	 *
	 * @return
	 */
	public int getMaxRetries() {
		return this.maxRetries;
	}

	/**
	 * Initial delay to use in exponential back-off on retries. May be zero,
	 * which results in no delay between retries.
	 *
	 * @return
	 */
	public TimeInterval getInitialBackoffDelay() {
		return this.initialBackoffDelay;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.maxRetries, this.initialBackoffDelay);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RetriesConfig) {
			RetriesConfig that = (RetriesConfig) obj;
			return Objects.equal(this.maxRetries, that.maxRetries) && Objects
					.equal(this.initialBackoffDelay, that.initialBackoffDelay);

		}
		return false;
	}

	@Override
	public String toString() {
		return JsonUtils.toPrettyString(JsonUtils.toJson(this));
	}

	public void validate() throws IllegalArgumentException {
		checkArgument(this.maxRetries >= 0,
				"retries: maxRetries must be positive");
		checkArgument(this.initialBackoffDelay.getTime() >= 0,
				"retries: initialBackoffDelay must be >= 0");
	}
}
