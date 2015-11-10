package com.elastisys.scale.cloudpool.commons.basepool.config;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Objects;
import com.google.gson.JsonObject;

/**
 * The section of a {@link BaseCloudPoolConfig} that describes how the
 * {@link CloudPoolDriver} implementation identifies pool members and connects
 * to its cloud provider.
 *
 * @see BaseCloudPoolConfig
 */
public class CloudPoolConfig {
	/**
	 * The name of the logical group of servers managed by the
	 * {@link CloudPoolDriver}.
	 */
	private final String name;

	/**
	 * {@link CloudPoolDriver}-specific JSON configuration document, the
	 * contents of which depends on the particular {@link CloudPoolDriver}
	 * -implementation being used. Typically, a minimum amount of configuration
	 * includes login credentials for connecting to the particular cloud API
	 * endpoint.
	 */
	private final JsonObject driverConfig;

	/**
	 * Creates a new {@link CloudPoolConfig}.
	 *
	 * @param name
	 *            The name of the logical group of servers managed by the
	 *            {@link CloudPoolDriver}.
	 * @param driverConfig
	 *            {@link CloudPoolDriver}-specific JSON configuration document,
	 *            the contents of which depends on the particular
	 *            {@link CloudPoolDriver} -implementation being used. Typically,
	 *            a minimum amount of configuration includes login credentials
	 *            for connecting to the particular cloud API endpoint.
	 */
	public CloudPoolConfig(String name, JsonObject driverConfig) {
		this.name = name;
		this.driverConfig = driverConfig;
	}

	/**
	 * The name of the logical group of servers managed by the
	 * {@link CloudPoolDriver}.
	 *
	 * @return
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * {@link CloudPoolDriver}-specific JSON configuration document, the
	 * contents of which depends on the particular {@link CloudPoolDriver}
	 * -implementation being used. Typically, a minimum amount of configuration
	 * includes login credentials for connecting to the particular cloud API
	 * endpoint.
	 *
	 * @return
	 */
	public JsonObject getDriverConfig() {
		return this.driverConfig;
	}

	/**
	 * Performs basic validation of this configuration.
	 *
	 * @throws CloudPoolException
	 */
	public void validate() throws CloudPoolException {
		try {
			checkNotNull(this.name, "missing name");
			checkNotNull(this.driverConfig, "missing driverConfig");
		} catch (Exception e) {
			throw new CloudPoolException(
					format("failed to validate cloudPool configuration: %s",
							e.getMessage()),
					e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.name, this.driverConfig);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CloudPoolConfig) {
			CloudPoolConfig that = (CloudPoolConfig) obj;
			return equal(this.name, that.name)
					&& equal(this.driverConfig, that.driverConfig);
		}
		return false;
	}

	@Override
	public String toString() {
		return JsonUtils.toPrettyString(JsonUtils.toJson(this, true));
	}
}