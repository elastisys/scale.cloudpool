package com.elastisys.scale.cloudpool.juju.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.elastisys.scale.cloudpool.commons.basepool.config.PoolUpdateConfig;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Configuration for a Juju cloud pool.
 *
 * @author Elastisys AB <techteam@elastisys.com>
 */
public class JujuCloudPoolConfig {

	public JujuEnvironmentConfig getEnvironment() {
		return this.environment;
	}

	public OperationsMode getMode() {
		return this.mode;
	}

	/** Default update interval. */
	private static final PoolUpdateConfig DEFAULT_UPDATE_CONFIG = new PoolUpdateConfig(
			new TimeInterval(10L, TimeUnit.SECONDS));

	/**
	 * The Juju environment configuration.
	 */
	private final JujuEnvironmentConfig environment;

	/**
	 * Whether we are operating on Juju Charm units or the set of machines
	 * available to Juju.
	 */
	private final OperationsMode mode;

	/**
	 * The time interval (in seconds) between periodical pool size updates.
	 */
	private final PoolUpdateConfig poolUpdate;

	public JujuCloudPoolConfig(JujuEnvironmentConfig environment, OperationsMode mode, PoolUpdateConfig poolUpdate) {
		this.environment = environment;
		this.mode = mode;
		this.poolUpdate = poolUpdate;
	}

	/**
	 * Validates that we have a complete configuration.
	 */
	public void validate() {
		checkArgument(this.environment != null, "config: no environment given");
		checkArgument(this.mode != null, "config: no mode given");
		// poolUpdate is optional

		this.environment.validate();
		getPoolUpdate().validate();
	}

	/**
	 * The time interval (in seconds) between periodical pool size updates.
	 *
	 * @return
	 */
	public PoolUpdateConfig getPoolUpdate() {
		return Optional.ofNullable(this.poolUpdate).orElse(DEFAULT_UPDATE_CONFIG);
	}
}
