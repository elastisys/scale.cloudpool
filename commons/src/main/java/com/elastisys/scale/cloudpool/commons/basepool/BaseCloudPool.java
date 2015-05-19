package com.elastisys.scale.cloudpool.commons.basepool;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.max;
import static java.lang.String.format;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jersey.repackaged.com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.CloudPoolMetadata;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics;
import com.elastisys.scale.cloudpool.commons.basepool.config.AlertsConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleInConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlan;
import com.elastisys.scale.cloudpool.commons.resizeplanner.ResizePlanner;
import com.elastisys.scale.cloudpool.commons.termqueue.ScheduledTermination;
import com.elastisys.scale.cloudpool.commons.termqueue.TerminationQueue;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.net.alerter.Alerter;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerter;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerter;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
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
 *     "bootScript": [
 *       "#!/bin/bash",
 *       "sudo apt-get update -qy",
 *       "sudo apt-get install -qy apache2"
 *     ]
 *   },
 *   "scaleInConfig": {
 *     "victimSelectionPolicy": "CLOSEST_TO_INSTANCE_HOUR",
 *     "instanceHourMargin": 0
 *   },
 *   "alerts": {
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
 *   "poolUpdatePeriod": 60
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
 * When {@link #getMachinePool} is called, the pool members are identified via a
 * call to {@link CloudPoolDriver#listMachines()}.
 *
 * <h3>Handling resize requests:</h3>
 *
 * When {@link #setDesiredSize(int)} is called, the {@link BaseCloudPool} notes
 * the new desired size but does not immediately apply the necessary changes to
 * the machine pool. Instead, pool updates are carried out in a periodical
 * manner (with a period specified by the {@code poolUpdatePeriod} configuration
 * key).
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

	/** Maximum concurrency in the {@link #executorService}. */
	private static final int MAX_CONCURRENCY = 20;

	/** A cloud-specific management driver for the cloud pool. */
	private CloudPoolDriver cloudDriver = null;

	/** The currently set configuration. */
	private final AtomicReference<BaseCloudPoolConfig> config;
	/** {@link ExecutorService} handling execution of "background jobs". */
	private final ScheduledExecutorService executorService;
	/** <code>true</code> if pool has been started. */
	private final AtomicBoolean started;

	/** The desired size of the machine pool. */
	private final AtomicReference<Integer> desiredSize;

	/**
	 * {@link EventBus} used to post {@link Alert} events that are to be
	 * forwarded by configured {@link Alerter}s (if any).
	 */
	private final EventBus eventBus;
	/**
	 * Holds the list of configured {@link Alerter}s (if any). Each
	 * {@link Alerter} is registered with the {@link EventBus} to forward posted
	 * {@link Alert}s.
	 */
	private final AtomicReference<List<Alerter>> alerters;
	/**
	 * Pool update task that periodically runs the {@link #updateMachinePool()}
	 * method to (1) effectuate pending instance terminations in the /
	 * termination queue and (2) replace terminated instances.
	 */
	private ScheduledFuture<?> poolUpdateTask;
	/** Lock to protect the machine pool from concurrent modifications. */
	private final Object updateLock = new Object();

	/**
	 * The queue of already termination-marked instances (these will be used to
	 * filter out instances already scheduled for termination from the candidate
	 * set).
	 */
	private final TerminationQueue terminationQueue;

	/**
	 * Constructs a new {@link BaseCloudPool} managing a given
	 * {@link CloudPoolDriver}.
	 *
	 * @param cloudDriver
	 *            A cloud-specific management driver for the cloud pool.
	 */
	public BaseCloudPool(CloudPoolDriver cloudDriver) {
		this(cloudDriver, new EventBus());
	}

	/**
	 * Constructs a new {@link BaseCloudPool} managing a given
	 * {@link CloudPoolDriver} and with an {@link EventBus} provided by the
	 * caller.
	 *
	 * @param cloudDriver
	 *            A cloud-specific management driver for the cloud pool.
	 * @param eventBus
	 *            The {@link EventBus} used to send {@link Alert}s and event
	 *            messages between components of the cloud pool.
	 */
	public BaseCloudPool(CloudPoolDriver cloudDriver, EventBus eventBus) {
		checkArgument(cloudDriver != null, "cloudDriver is null");
		checkArgument(eventBus != null, "eventBus is null");

		this.cloudDriver = cloudDriver;
		this.eventBus = eventBus;

		ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setDaemon(true).setNameFormat("cloudpool-%d").build();
		this.executorService = Executors.newScheduledThreadPool(
				MAX_CONCURRENCY, threadFactory);

		this.config = Atomics.newReference();
		this.started = new AtomicBoolean(false);

		this.alerters = Atomics.newReference();

		this.terminationQueue = new TerminationQueue();
		this.desiredSize = Atomics.newReference();
	}

	@Override
	public void configure(JsonObject jsonConfig)
			throws IllegalArgumentException, CloudPoolException {
		BaseCloudPoolConfig configuration = validate(jsonConfig);

		synchronized (this.updateLock) {
			this.config.set(configuration);

			if (isStarted()) {
				stop();
			}
			start();
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
							+ e.getMessage(), e);
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

	private void start() throws CloudPoolException {
		checkState(getConfiguration().isPresent(),
				"attempt to start cloud pool before being configured");
		if (isStarted()) {
			return;
		}
		LOG.info("starting {} driving a {}", getClass().getSimpleName(),
				this.cloudDriver.getClass().getSimpleName());

		// re-configure driver
		LOG.info("configuring cloud pool '{}'", config().getCloudPool()
				.getName());
		this.cloudDriver.configure(config());
		determineDesiredSizeIfUnset();

		// start pool update task that periodically runs updateMachinepool()
		int poolUpdatePeriod = config().getPoolUpdatePeriod();
		this.poolUpdateTask = this.executorService.scheduleWithFixedDelay(
				new PoolUpdateTask(), poolUpdatePeriod, poolUpdatePeriod,
				TimeUnit.SECONDS);

		setUpAlerters(config());
		this.started.set(true);
		LOG.info(getClass().getSimpleName() + " started.");
	}

	/**
	 * In case no {@link #desiredSize} has been set yet, this method determines
	 * the (initial) desired size for the {@link CloudPoolDriver} as the current
	 * size of the {@link CloudPoolDriver}. On failure to determine the pool
	 * size (for example, due to a temporary cloud provider API outage), an
	 * alert is sent out (if alerting has been set up).
	 */
	private void determineDesiredSizeIfUnset() {
		if (this.desiredSize.get() != null) {
			return;
		}

		try {
			LOG.debug("determining initial desired pool size");
			setDesiredSizeIfUnset(getMachinePool());
		} catch (CloudPoolException e) {
			String message = format(
					"failed to determine initial size of pool: %s\n%s",
					e.getMessage(), Throwables.getStackTraceAsString(e));
			this.eventBus.post(new Alert(AlertTopics.POOL_FETCH.name(),
					AlertSeverity.ERROR, UtcTime.now(), message));
			LOG.error(message);
		}
	}

	/**
	 * Initializes the {@link #desiredSize} (if one hasn't already been set)
	 * from a given {@link MachinePool} .
	 * <p/>
	 * If {@link #desiredSize} is already set, this method returns immediately.
	 *
	 * @param pool
	 */
	private void setDesiredSizeIfUnset(MachinePool pool) {
		if (this.desiredSize.get() != null) {
			return;
		}
		// exclude inactive instances since they aren't actually part
		// of the desiredSize (they are to be replaced)
		int effectiveSize = pool.getActiveMachines().size();
		int allocated = pool.getAllocatedMachines().size();
		this.desiredSize.set(effectiveSize);
		LOG.info("initial desiredSize is {} (allocated: {}, effective: {})",
				this.desiredSize, allocated, effectiveSize);
	}

	private void stop() {
		if (isStarted()) {
			LOG.debug("stopping {} ...", getClass().getSimpleName());
			// cancel tasks (allow any running tasks to finish)
			this.poolUpdateTask.cancel(false);
			this.poolUpdateTask = null;
			takeDownAlerters();
			this.started.set(false);
		}
		LOG.info(getClass().getSimpleName() + " stopped.");
	}

	boolean isStarted() {
		return this.started.get();
	}

	@Override
	public MachinePool getMachinePool() throws CloudPoolException {
		checkState(getConfiguration().isPresent(),
				"cloud pool needs to be configured before use");

		List<Machine> machines = listMachines();
		MachinePool pool = new MachinePool(machines, UtcTime.now());
		// if we haven't yet determined the desired size, we do so now
		setDesiredSizeIfUnset(pool);
		return pool;
	}

	Integer desiredSize() {
		return this.desiredSize.get();
	}

	@Override
	public PoolSizeSummary getPoolSize() throws CloudPoolException {
		checkState(getConfiguration().isPresent(),
				"cloud pool needs to be configured before use");

		MachinePool pool = getMachinePool();
		return new PoolSizeSummary(this.desiredSize.get(), pool
				.getAllocatedMachines().size(), pool.getActiveMachines().size());
	}

	/**
	 * Lists the {@link Machine}s in the {@link CloudPoolDriver}. Raises a
	 * {@link CloudPoolException} on failure and sends alert (if configured).
	 *
	 * @return
	 *
	 * @throws CloudPoolException
	 */
	private List<Machine> listMachines() {
		return this.cloudDriver.listMachines();
	}

	@Override
	public void setDesiredSize(int desiredSize)
			throws IllegalArgumentException, CloudPoolException {
		checkState(getConfiguration().isPresent(),
				"cloud pool needs to be configured before use");
		checkArgument(desiredSize >= 0, "negative desired pool size");

		// prevent concurrent pool modifications
		synchronized (this.updateLock) {
			LOG.info("set desiredSize to {}", desiredSize);
			this.desiredSize.set(desiredSize);
		}
	}

	@Override
	public void terminateMachine(String machineId, boolean decrementDesiredSize)
			throws IllegalArgumentException, CloudPoolException {
		checkState(getConfiguration().isPresent(),
				"cloud pool needs to be configured before use");

		// prevent concurrent pool modifications
		synchronized (this.updateLock) {
			LOG.debug("terminating {}", machineId);
			this.cloudDriver.terminateMachine(machineId);
			if (decrementDesiredSize) {
				// note: decrement unless desiredSize has been set to 0 (without
				// having been effectuated yet)
				int newSize = max(this.desiredSize.get() - 1, 0);
				LOG.debug("decrementing desiredSize to {}", newSize);
				setDesiredSize(newSize);
			}
		}
		terminationAlert(machineId);
	}

	@Override
	public void attachMachine(String machineId)
			throws IllegalArgumentException, CloudPoolException {
		checkState(getConfiguration().isPresent(),
				"cloud pool needs to be configured before use");

		// prevent concurrent pool modifications
		synchronized (this.updateLock) {
			LOG.debug("attaching instance {} to pool", machineId);
			this.cloudDriver.attachMachine(machineId);
			// implicitly increases pool size
			setDesiredSize(this.desiredSize.get() + 1);
		}
		attachAlert(machineId);
	}

	@Override
	public void detachMachine(String machineId, boolean decrementDesiredSize)
			throws IllegalArgumentException, CloudPoolException {
		checkState(getConfiguration().isPresent(),
				"cloud pool needs to be configured before use");

		// prevent concurrent pool modifications
		synchronized (this.updateLock) {
			LOG.debug("detaching {} from pool", machineId);
			this.cloudDriver.detachMachine(machineId);
			if (decrementDesiredSize) {
				// note: decrement unless desiredSize has been set to 0 (without
				// having been effectuated yet)
				int newSize = max(this.desiredSize.get() - 1, 0);
				LOG.debug("decrementing desiredSize to {}", newSize);
				setDesiredSize(newSize);
			}
		}
		detachAlert(machineId);
	}

	@Override
	public void setServiceState(String machineId, ServiceState serviceState)
			throws IllegalArgumentException {
		checkState(getConfiguration().isPresent(),
				"cloud pool needs to be configured before use");

		LOG.debug("service state {} assigned to {}", serviceState.name(),
				machineId);
		this.cloudDriver.setServiceState(machineId, serviceState);
		serviceStateAlert(machineId, serviceState);
	}

	@Override
	public void setMembershipStatus(String machineId,
			MembershipStatus membershipStatus) throws NotFoundException,
			CloudPoolException {
		checkState(getConfiguration().isPresent(),
				"cloud pool needs to be configured before use");

		LOG.debug("membership status {} assigned to {}", membershipStatus,
				machineId);
		this.cloudDriver.setMembershipStatus(machineId, membershipStatus);
		membershipStatusAlert(machineId, membershipStatus);
	}

	@Override
	public CloudPoolMetadata getMetadata() {
		return this.cloudDriver.getMetadata();
	}

	/**
	 * Sets up {@link Alerter}s, in case the configuration contains an
	 * {@link AlertsConfig}.
	 *
	 * @param configuration
	 */
	private void setUpAlerters(BaseCloudPoolConfig configuration) {
		AlertsConfig alertsConfig = configuration.getAlerts();
		if (alertsConfig == null) {
			LOG.debug("no alerts configuration, no alerters set up");
			return;
		}

		List<Alerter> newAlerters = Lists.newArrayList();
		Map<String, JsonElement> standardAlertMetadataTags = standardAlertMetadata();
		// add SMTP alerters
		List<SmtpAlerterConfig> smtpAlerters = alertsConfig.getSmtpAlerters();
		LOG.debug("adding {} SMTP alerter(s)", smtpAlerters.size());
		for (SmtpAlerterConfig smtpAlerterConfig : smtpAlerters) {
			newAlerters.add(new SmtpAlerter(smtpAlerterConfig,
					standardAlertMetadataTags));
		}
		// add HTTP alerters
		List<HttpAlerterConfig> httpAlerters = alertsConfig.getHttpAlerters();
		LOG.debug("adding {} HTTP alerter(s)", httpAlerters.size());
		for (HttpAlerterConfig httpAlerterConfig : httpAlerters) {
			newAlerters.add(new HttpAlerter(httpAlerterConfig,
					standardAlertMetadataTags));
		}
		// register every alerter with event bus
		for (Alerter alerter : newAlerters) {
			this.eventBus.register(alerter);
		}
		this.alerters.set(newAlerters);
	}

	/**
	 * Standard {@link Alert} tags to include in all {@link Alert} mails sent by
	 * the configured {@link Alerter}s.
	 *
	 * @return
	 */
	private Map<String, JsonElement> standardAlertMetadata() {
		Map<String, JsonElement> standardTags = Maps.newHashMap();
		List<String> ipv4Addresses = Lists.newArrayList();
		for (InetAddress inetAddr : HostUtils.hostIpv4Addresses()) {
			ipv4Addresses.add(inetAddr.getHostAddress());
		}
		String ipAddresses = Joiner.on(", ").join(ipv4Addresses);
		standardTags.put("cloudPoolEndpointIps", JsonUtils.toJson(ipAddresses));
		standardTags.put("cloudPoolName",
				JsonUtils.toJson(config().getCloudPool().getName()));
		return standardTags;
	}

	/**
	 * Unregisters all configured {@link Alerter}s from the {@link EventBus}.
	 */
	private void takeDownAlerters() {
		if (this.alerters.get() != null) {
			List<Alerter> alerterList = this.alerters.get();
			for (Alerter alerter : alerterList) {
				this.eventBus.unregister(alerter);
			}
		}
	}

	BaseCloudPoolConfig config() {
		return this.config.get();
	}

	private String poolName() {
		return config().getCloudPool().getName();
	}

	private ScaleOutConfig scaleOutConfig() {
		return config().getScaleOutConfig();
	}

	private ScaleInConfig scaleInConfig() {
		return config().getScaleInConfig();
	}

	/**
	 * Updates the size of the machine pool to match the currently set desired
	 * size. This may involve terminating termination-due machines and placing
	 * new server requests to replace terminated servers.
	 * <p/>
	 * Waits for the {@link #updateLock} to avoid concurrent pool updates.
	 *
	 * @throws CloudPoolException
	 */
	void updateMachinePool() throws CloudPoolException {
		// check if we need to determine desired size (it may not have been
		// possible on startup, e.g., due to cloud API being ureachable)
		determineDesiredSizeIfUnset();
		if (this.desiredSize.get() == null) {
			LOG.warn("cannot update pool: haven't been able to "
					+ "determine initial desired size");
			return;
		}

		// prevent multiple threads from concurrently updating pool
		synchronized (this.updateLock) {
			int targetSize = this.desiredSize.get();
			try {
				doPoolUpdate(targetSize);
			} catch (Throwable e) {
				String message = format("failed to adjust pool "
						+ "\"%s\" to desired size %d: %s\n%s", poolName(),
						targetSize, e.getMessage(),
						Throwables.getStackTraceAsString(e));
				this.eventBus.post(new Alert(AlertTopics.RESIZE.name(),
						AlertSeverity.ERROR, UtcTime.now(), message));
				throw new CloudPoolException(message, e);
			}
		}
	}

	private void doPoolUpdate(int newSize) throws CloudPoolException {
		LOG.info("updating pool size to desired size {}", newSize);

		MachinePool pool = getMachinePool();
		LOG.debug("current pool members: {}",
				Lists.transform(pool.getMachines(), Machine.toShortString()));
		this.terminationQueue.filter(pool.getActiveMachines());
		ResizePlanner resizePlanner = new ResizePlanner(pool,
				this.terminationQueue, scaleInConfig()
						.getVictimSelectionPolicy(), scaleInConfig()
						.getInstanceHourMargin());
		int netSize = resizePlanner.getNetSize();

		ResizePlan resizePlan = resizePlanner.calculateResizePlan(newSize);
		if (resizePlan.hasScaleOutActions()) {
			scaleOut(resizePlan);
		}
		if (resizePlan.hasScaleInActions()) {
			List<ScheduledTermination> terminations = resizePlan
					.getToTerminate();
			LOG.info("scheduling {} machine(s) for termination",
					terminations.size());
			for (ScheduledTermination termination : terminations) {
				this.terminationQueue.add(termination);
				LOG.debug("scheduling machine {} for termination at {}",
						termination.getInstance().getId(),
						termination.getTerminationTime());
			}
			LOG.debug("termination queue: {}", this.terminationQueue);
		}
		if (resizePlan.noChanges()) {
			LOG.info("pool is already properly sized ({})", netSize);
		}
		// effectuate scheduled terminations that are (over)due
		terminateOverdueMachines();
	}

	private List<Machine> scaleOut(ResizePlan resizePlan)
			throws StartMachinesException {
		LOG.info("sparing {} machine(s) from termination, "
				+ "placing {} new request(s)", resizePlan.getToSpare(),
				resizePlan.getToRequest());
		this.terminationQueue.spare(resizePlan.getToSpare());

		try {
			List<Machine> startedMachines = this.cloudDriver.startMachines(
					resizePlan.getToRequest(), scaleOutConfig());
			startAlert(startedMachines);
			return startedMachines;
		} catch (StartMachinesException e) {
			// may have failed part-way through. notify of machines that were
			// started before error occurred.
			startAlert(e.getStartedMachines());
			throw e;
		}
	}

	private List<Machine> terminateOverdueMachines() {
		LOG.debug("checking termination queue for overdue machines: {}",
				this.terminationQueue);
		List<ScheduledTermination> overdueInstances = this.terminationQueue
				.popOverdueInstances();
		if (overdueInstances.isEmpty()) {
			return Collections.emptyList();
		}

		List<Machine> terminated = Lists.newArrayList();
		LOG.info("Terminating {} overdue machine(s): {}",
				overdueInstances.size(), overdueInstances);
		for (ScheduledTermination overdueInstance : overdueInstances) {
			String victimId = overdueInstance.getInstance().getId();
			try {
				this.cloudDriver.terminateMachine(victimId);
				terminated.add(overdueInstance.getInstance());
			} catch (Exception e) {
				// only warn, since a failure to terminate an instance is not
				// necessarily an error condition, as the machine, e.g., may
				// have been terminated by external means since we last checked
				// the pool members
				String message = format(
						"failed to terminate instance '%s': %s\n%s", victimId,
						e.getMessage(), Throwables.getStackTraceAsString(e));
				LOG.warn(message);
				this.eventBus.post(new Alert(AlertTopics.RESIZE.name(),
						AlertSeverity.WARN, UtcTime.now(), message));
			}
		}
		if (!terminated.isEmpty()) {
			terminationAlert(terminated);
		}

		return terminated;
	}

	/**
	 * Post an {@link Alert} that new machines have been started in the pool.
	 *
	 * @param startedMachines
	 *            The new machine instances that have been started.
	 */
	void startAlert(List<Machine> startedMachines) {
		if (startedMachines.isEmpty()) {
			return;
		}

		String message = String.format(
				"%d machine(s) were requested from cloud pool %s",
				startedMachines.size(), poolName());
		LOG.info(message);
		Map<String, JsonElement> tags = Maps.newHashMap();
		List<String> startedMachineIds = Lists.transform(startedMachines,
				Machine.toId());
		tags.put("requestedMachines",
				JsonUtils.toJson(Joiner.on(", ").join(startedMachineIds)));
		tags.put("poolMembers", poolMembersTag());
		this.eventBus.post(new Alert(AlertTopics.RESIZE.name(),
				AlertSeverity.INFO, UtcTime.now(), message, tags));
	}

	/**
	 * Post an {@link Alert} that the members have been terminated from the
	 * pool.
	 *
	 * @param terminatedMachines
	 *            The machine instances that were terminated.
	 */
	void terminationAlert(List<Machine> terminatedMachines) {
		String message = String.format(
				"%d machine(s) were terminated in cloud pool %s",
				terminatedMachines.size(), poolName());
		LOG.info(message);
		Map<String, JsonElement> tags = Maps.newHashMap();
		List<String> terminatedMachineIds = Lists.transform(terminatedMachines,
				Machine.toId());
		tags.put("terminatedMachines",
				JsonUtils.toJson(Joiner.on(", ").join(terminatedMachineIds)));
		tags.put("poolMembers", poolMembersTag());
		this.eventBus.post(new Alert(AlertTopics.RESIZE.name(),
				AlertSeverity.INFO, UtcTime.now(), message, tags));
	}

	/**
	 * Post an {@link Alert} that a machine was terminated from the pool.
	 *
	 * @param machineId
	 */
	void terminationAlert(String machineId) {
		Map<String, JsonElement> tags = Maps.newHashMap();
		tags.put("terminatedMachines", JsonUtils.toJson(machineId));
		tags.put("poolMembers", poolMembersTag());
		String message = String.format("Terminated machine %s.", machineId);
		this.eventBus.post(new Alert(AlertTopics.RESIZE.name(),
				AlertSeverity.INFO, UtcTime.now(), message, tags));
	}

	/**
	 * Post an {@link Alert} that a machine was attached to the pool.
	 *
	 * @param machineId
	 */
	void attachAlert(String machineId) {
		Map<String, JsonElement> tags = ImmutableMap.of("attachedMachines",
				JsonUtils.toJson(machineId));
		String message = String.format("Attached machine %s to pool.",
				machineId);
		this.eventBus.post(new Alert(AlertTopics.RESIZE.name(),
				AlertSeverity.INFO, UtcTime.now(), message, tags));
	}

	/**
	 * Post an {@link Alert} that a machine was detached from the pool.
	 *
	 * @param machineId
	 */
	void detachAlert(String machineId) {
		Map<String, JsonElement> tags = ImmutableMap.of("detachedMachines",
				JsonUtils.toJson(machineId));
		String message = String.format("Detached machine %s from pool.",
				machineId);
		this.eventBus.post(new Alert(AlertTopics.RESIZE.name(),
				AlertSeverity.INFO, UtcTime.now(), message, tags));
	}

	/**
	 * Post an {@link Alert} that a pool member had its {@link ServiceState}
	 * set.
	 *
	 * @param machineId
	 * @param state
	 */
	void serviceStateAlert(String machineId, ServiceState state) {
		Map<String, JsonElement> tags = ImmutableMap.of();
		String message = String.format(
				"Service state set to %s for machine %s.", state.name(),
				machineId);
		this.eventBus.post(new Alert(AlertTopics.SERVICE_STATE.name(),
				AlertSeverity.DEBUG, UtcTime.now(), message, tags));
	}

	/**
	 * Post an {@link Alert} that a pool member had its {@link MembershipStatus}
	 * set.
	 *
	 * @param machineId
	 * @param membershipStatus
	 */
	void membershipStatusAlert(String machineId,
			MembershipStatus membershipStatus) {
		Map<String, JsonElement> tags = ImmutableMap.of();
		String message = String.format(
				"Membership status set to %s for machine %s.",
				membershipStatus, machineId);
		this.eventBus.post(new Alert(AlertTopics.MEMBERSHIP_STATUS.name(),
				AlertSeverity.DEBUG, UtcTime.now(), message, tags));
	}

	private JsonElement poolMembersTag() {
		try {
			List<Machine> poolMembers = listMachines();
			// exclude metadata field (noisy)
			List<Machine> shortFormatMembers = Lists.transform(poolMembers,
					Machine.toShortFormat());
			return JsonUtils.toJson(shortFormatMembers);
		} catch (Exception e) {
			LOG.warn("failed to retrieve pool members: {}", e.getMessage());
			return JsonUtils.toJson(String.format("N/A (call failed: %s)",
					e.getMessage()));
		}
	}

	/**
	 * Task that, when executed, runs {@link BaseCloudPool#updateMachinePool()}.
	 */
	public class PoolUpdateTask implements Runnable {
		@Override
		public void run() {
			try {
				updateMachinePool();
			} catch (CloudPoolException e) {
				LOG.error(
						format("machine pool update task failed: %s",
								e.getMessage()), e);
			}
		}
	}

}
