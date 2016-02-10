package com.elastisys.scale.cloudpool.commons.basepool;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotConfiguredException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.NotStartedException;
import com.elastisys.scale.cloudpool.api.types.CloudPoolMetadata;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.impl.CachingPoolFetcher;
import com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.impl.RetryingPoolFetcher;
import com.elastisys.scale.cloudpool.commons.basepool.poolupdater.PoolUpdater;
import com.elastisys.scale.cloudpool.commons.basepool.poolupdater.impl.StandardPoolUpdater;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.Alerter;
import com.elastisys.scale.commons.net.alerter.multiplexing.MultiplexingAlerter;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Atomics;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A generic {@link CloudPool} that is provided as a basis for building
 * cloud-specific {@link CloudPool}s.
 * <p/>
 * The {@link BaseCloudPool} implements sensible behavior for the
 * {@link CloudPool} methods and relieves implementors from dealing with the
 * details of continuously monitoring and re-scaling the pool with the right
 * amount of machines given the member machine states, handling
 * (re-)configurations, sending alerts, etc. Implementers of cloud-specific
 * {@link CloudPool}s only need to implements a small set of machine pool
 * management primitives for a particular cloud. These management primitives are
 * supplied to the {@link BaseCloudPool} at construction-time in the form of a
 * {@link CloudPoolDriver}, which implements the management primitives according
 * to the API of the targeted cloud.
 * <p/>
 * The configuration ({@link BaseCloudPoolConfig}) specifies how the
 * {@link BaseCloudPool}:
 * <ul>
 * <li>should configure its {@link CloudPoolDriver} to allow it to communicate
 * with its cloud API (the {@code driverConfig} key).</li>
 * <li>provisions new instances when the pool needs to grow (the
 * {@code scaleOutConfig} key).</li>
 * <li>decommissions instances when the pool needs to shrink (the
 * {@code scaleInConfig} key).</li>
 * <li>alerts system administrators (via email) or other systems (via webhooks)
 * of interesting events: resize operations, error conditions, etc (the
 * {@code alerts} key).</li>
 * </ul>
 * A configuration document may look as follows:
 *
 * <pre>
 * {
 *   "cloudPool": {
 *     "name": "MyScalingPool",
 *     "driverConfig": {
 *       "awsAccessKeyId": "ABC...XYZ",
 *       "awsSecretAccessKey": "abc...xyz",
 *       "region": "us-east-1"
 *     }
 *   },
 *   "scaleOutConfig": {
 *     "size": "m1.small",
 *     "image": "ami-018c9568",
 *     "keyPair": "instancekey",
 *     "securityGroups": ["webserver"],
 *     "encodedUserData": "IyEvYmluL2Jhc2gKCnN1ZG8gYXB0LWdldCB1cGRhdGUgLXF5CnN1ZG8gYXB0LWdldCBpbnN0YWxsIC1xeSBhcGFjaGUyCg=="
 *   },
 *   "scaleInConfig": {
 *     "victimSelectionPolicy": "CLOSEST_TO_INSTANCE_HOUR",
 *     "instanceHourMargin": 0
 *   },
 *   "alerts": {
 *     "duplicateSuppression": { "time": 5, "unit": "minutes" },
 *     "smtp": [
 *       {
 *         "subject": "[elastisys:scale] cloud pool alert for MyScalingPool",
 *         "recipients": ["receiver@destination.com"],
 *         "sender": "noreply@elastisys.com",
 *         "smtpClientConfig": {
 *           "smtpHost": "mail.server.com",
 *           "smtpPort": 465,
 *           "authentication": {"userName": "john", "password": "secret"},
 *           "useSsl": True
 *         }
 *       }
 *     ],
 *     "http": [
 *       {
 *         "destinationUrls": ["https://some.host1:443/"],
 *         "severityFilter": "ERROR|FATAL",
 *         "auth": {
 *           "basicCredentials": { "username": "user1", "password": "secret1" }
 *         }
 *       }
 *     ]
 *   },
 *   "poolFetch": {
 *     "retries": {
 *       "maxRetries": 3,
 *       "initialBackoffDelay": {"time": 3, "unit": "seconds"}
 *     },
 *     "refreshInterval": {"time": 30, "unit": "seconds"},
 *     "reachabilityTimeout": {"time": 5, "unit": "minutes"}
 *   },
 *   "poolUpdate": {
 *     "updateInterval": {"time": 15, "unit": "seconds"}
 *   }
 * }
 * </pre>
 *
 * The {@link BaseCloudPool} operates according to the {@link CloudPool}
 * contract. Some details on how the {@link BaseCloudPool} satisfies the
 * contract are summarized below.
 * <p/>
 * <h3>Configuration:</h2>
 *
 * When {@link #configure} is called, the {@link BaseCloudPool} expects a JSON
 * document that validates against its JSON Schema (as returned by
 * {@link #getConfigurationSchema()}). The entire configuration document is
 * passed on to the {@link CloudPoolDriver} via a call to
 * {@link CloudPoolDriver#configure}. The parts of the configuration that are of
 * special interest to the {@link CloudPoolDriver}, such as cloud login details
 * and the logical pool name, are located under the {@code cloudPool} key. The
 * {@code cloudPool/driverConfig} configuration key holds
 * implementation-specific settings for the particular {@link CloudPoolDriver}
 * implementation. An example configuration is given above.
 *
 * <h3>Identifying pool members:</h2>
 *
 * Pool members are identified via a call to
 * {@link CloudPoolDriver#listMachines()}.
 *
 * <h3>Handling resize requests:</h3>
 *
 * When {@link #setDesiredSize(int)} is called, the {@link BaseCloudPool} notes
 * the new desired size but does not immediately apply the necessary changes to
 * the machine pool. Instead, pool updates are carried out in a periodical
 * manner (with a period specified by the {@code poolUpdate} configuration key).
 * <p/>
 * When a pool update is triggered, the actions taken depend on if the pool
 * needs to grow or shrink.
 *
 * <ul>
 * <li><i>scale out</i>: start by sparing machines from termination if the
 * termination queue is non-empty. For any remaining instances: request them to
 * be started by the {@link CloudPoolDriver} via
 * {@link CloudPoolDriver#startMachines}. The {@code scaleOutConfig} is passed
 * to the {@link CloudPoolDriver}.</li>
 * <li><i>scale in</i>: start by terminating any machines in
 * {@link MachineState#REQUESTED} state, since these are likely to not yet incur
 * cost. Any such machines are terminated immediately. If additional capacity is
 * to be removed, select a victim according to the configured
 * {@code victimSelectionPolicy} and schedule it for termination according to
 * the configured {@code instanceHourMargin}. Each instance termination is
 * delegated to {@link CloudPoolDriver#terminateMachine(String)}.</li>
 * </ul>
 *
 * <h3>Alerts:</h3>
 *
 * If email and/or HTTP webhook alerts have been configured, the
 * {@link BaseCloudPool} will send alerts to notify selected recipients of
 * interesting events (such as error conditions, scale-ups/scale-downs, etc).
 *
 * @see CloudPoolDriver
 */
public class BaseCloudPool implements CloudPool {

	/** {@link Logger} instance. */
	static final Logger LOG = LoggerFactory.getLogger(BaseCloudPool.class);

	/** Declares where the runtime state is stored. */
	private final StateStorage stateStorage;
	/** A cloud-specific management driver for the cloud pool. */
	private CloudPoolDriver cloudDriver = null;

	/**
	 * {@link EventBus} used to post {@link Alert} events that are to be
	 * forwarded by configured {@link Alerter}s (if any).
	 */
	private final EventBus eventBus;

	/** The currently set configuration. */
	private final AtomicReference<BaseCloudPoolConfig> config;
	/** <code>true</code> if pool has been started. */
	private final AtomicBoolean started;

	/**
	 * Dispatches {@link Alert}s sent on the {@link EventBus} to configured
	 * {@link Alerter}s.
	 */
	private final MultiplexingAlerter alerter;

	/** Retrieves {@link MachinePool} members. */
	private CachingPoolFetcher poolFetcher;
	/** Manages the machine pool to keep it at its desired size. */
	private PoolUpdater poolUpdater;

	/**
	 * Constructs a new {@link BaseCloudPool} managing a given
	 * {@link CloudPoolDriver}.
	 *
	 * @param stateStorage
	 *            Declares where the runtime state is stored.
	 * @param cloudDriver
	 *            A cloud-specific management driver for the cloud pool.
	 */
	public BaseCloudPool(StateStorage stateStorage,
			CloudPoolDriver cloudDriver) {
		this(stateStorage, cloudDriver, new EventBus());
	}

	/**
	 * Constructs a new {@link BaseCloudPool} managing a given
	 * {@link CloudPoolDriver} and using an {@link EventBus} provided by the
	 * caller.
	 *
	 * @param stateStorage
	 *            Declares where the runtime state is stored.
	 * @param cloudDriver
	 *            A cloud-specific management driver for the cloud pool.
	 * @param eventBus
	 *            The {@link EventBus} used to send {@link Alert}s and event
	 *            messages between components of the cloud pool.
	 */
	public BaseCloudPool(StateStorage stateStorage, CloudPoolDriver cloudDriver,
			EventBus eventBus) {
		checkArgument(stateStorage != null, "no stateStorage given");
		checkArgument(cloudDriver != null, "no cloudDriver given");
		checkArgument(eventBus != null, "no eventBus given");

		this.stateStorage = stateStorage;
		this.cloudDriver = cloudDriver;
		this.eventBus = eventBus;

		this.alerter = new MultiplexingAlerter();
		this.eventBus.register(this.alerter);

		this.config = Atomics.newReference();
		this.started = new AtomicBoolean(false);
	}

	@Override
	public void configure(JsonObject jsonConfig)
			throws IllegalArgumentException, CloudPoolException {
		BaseCloudPoolConfig configuration = validate(jsonConfig);

		synchronized (this) {
			boolean wasStarted = isStarted();
			if (wasStarted) {
				stop();
			}

			LOG.debug("setting new configuration: {}",
					JsonUtils.toPrettyString(jsonConfig));
			this.config.set(configuration);
			// re-configure driver
			this.cloudDriver.configure(configuration);
			// alerters may have changed
			this.alerter.unregisterAlerters();
			this.alerter.registerAlerters(config().getAlerts(),
					standardAlertMetadata());

			if (wasStarted) {
				start();
			}
		}
	}

	private BaseCloudPoolConfig validate(JsonObject jsonConfig)
			throws IllegalArgumentException {
		try {
			BaseCloudPoolConfig configuration = JsonUtils.toObject(jsonConfig,
					BaseCloudPoolConfig.class);
			configuration.validate();
			return configuration;
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, IllegalArgumentException.class);
			throw new IllegalArgumentException(
					"failed to validate cloud pool configuration: "
							+ e.getMessage(),
					e);
		}
	}

	@Override
	public Optional<JsonObject> getConfiguration() {
		BaseCloudPoolConfig currentConfig = this.config.get();
		if (currentConfig == null) {
			return Optional.absent();
		}
		return Optional.of(JsonUtils.toJson(currentConfig).getAsJsonObject());
	}

	@Override
	public void start() throws NotConfiguredException {
		ensureConfigured();

		if (isStarted()) {
			return;
		}
		LOG.info("starting {} driving a {}", getClass().getSimpleName(),
				this.cloudDriver.getClass().getSimpleName());

		RetryingPoolFetcher retryingFetcher = new RetryingPoolFetcher(
				this.cloudDriver, config().getPoolFetch().getRetries());
		// note: we wait for first attempt to get the pool to complete
		this.poolFetcher = new CachingPoolFetcher(this.stateStorage,
				retryingFetcher, config().getPoolFetch(), this.eventBus);
		this.poolFetcher.awaitFirstFetch();
		this.poolUpdater = new StandardPoolUpdater(this.cloudDriver,
				this.poolFetcher, this.eventBus, config());

		this.started.set(true);
		LOG.info(getClass().getSimpleName() + " started.");
	}

	@Override
	public void stop() {
		if (isStarted()) {
			LOG.debug("stopping {} ...", getClass().getSimpleName());
			// cancel tasks (allow any running tasks to finish)
			this.poolFetcher.close();
			this.poolUpdater.close();
			this.started.set(false);
		}
		LOG.info(getClass().getSimpleName() + " stopped.");
	}

	@Override
	public CloudPoolStatus getStatus() {
		return new CloudPoolStatus(isStarted(), isConfigured());
	}

	private boolean isConfigured() {
		return getConfiguration().isPresent();
	}

	/**
	 * Checks that a configuration has been set for the {@link CloudPool} or
	 * throws a {@link NotConfiguredException}.
	 */
	private void ensureConfigured() throws NotConfiguredException {
		if (!isConfigured()) {
			throw new NotConfiguredException("cloud pool is not configured");
		}
	}

	boolean isStarted() {
		return this.started.get();
	}

	@Override
	public MachinePool getMachinePool() throws CloudPoolException {
		ensureStarted();

		return this.poolFetcher.get();
	}

	/**
	 * Ensures that the {@link CloudPool} has been started or otherwise throws a
	 * {@link NotStartedException}.
	 */
	private void ensureStarted() throws NotStartedException {
		if (!isStarted()) {
			throw new NotStartedException(
					"attempt to use cloud pool that is stopped");
		}
	}

	@Override
	public PoolSizeSummary getPoolSize() throws CloudPoolException {
		ensureStarted();

		MachinePool pool = this.poolFetcher.get();
		return new PoolSizeSummary(pool.getTimestamp(),
				this.poolUpdater.getDesiredSize(),
				pool.getAllocatedMachines().size(),
				pool.getActiveMachines().size());
	}

	@Override
	public void setDesiredSize(int desiredSize)
			throws IllegalArgumentException, CloudPoolException {
		ensureStarted();

		this.poolUpdater.setDesiredSize(desiredSize);
	}

	@Override
	public void terminateMachine(String machineId, boolean decrementDesiredSize)
			throws IllegalArgumentException, CloudPoolException {
		ensureStarted();

		this.poolUpdater.terminateMachine(machineId, decrementDesiredSize);
	}

	@Override
	public void attachMachine(String machineId)
			throws IllegalArgumentException, CloudPoolException {
		ensureStarted();

		this.poolUpdater.attachMachine(machineId);
	}

	@Override
	public void detachMachine(String machineId, boolean decrementDesiredSize)
			throws IllegalArgumentException, CloudPoolException {
		ensureStarted();

		this.poolUpdater.detachMachine(machineId, decrementDesiredSize);
	}

	@Override
	public void setServiceState(String machineId, ServiceState serviceState)
			throws IllegalArgumentException {
		ensureStarted();

		this.poolUpdater.setServiceState(machineId, serviceState);
	}

	@Override
	public void setMembershipStatus(String machineId,
			MembershipStatus membershipStatus)
					throws NotFoundException, CloudPoolException {
		ensureStarted();

		this.poolUpdater.setMembershipStatus(machineId, membershipStatus);
	}

	@Override
	public CloudPoolMetadata getMetadata() {
		return this.cloudDriver.getMetadata();
	}

	/**
	 * Standard tags that are to be included in all sent out {@link Alert}s (in
	 * addition to those already set on the {@link Alert} itself).
	 *
	 * @return
	 */
	private Map<String, JsonElement> standardAlertMetadata() {
		Map<String, JsonElement> standardTags = Maps.newHashMap();
		List<String> ipv4Addresses = Lists.newArrayList();
		for (InetAddress inetAddr : HostUtils.hostIpv4Addresses()) {
			ipv4Addresses.add(inetAddr.getHostAddress());
		}
		standardTags.put("cloudPoolEndpointIps",
				JsonUtils.toJson(ipv4Addresses));
		standardTags.put("cloudPoolName",
				JsonUtils.toJson(config().getCloudPool().getName()));
		return standardTags;
	}

	BaseCloudPoolConfig config() {
		return this.config.get();
	}

	void updateMachinePool() {
		this.poolUpdater.resize(config());
	}

}
