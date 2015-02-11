package com.elastisys.scale.cloudadapters.commons.adapter;

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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapers.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudadapers.api.types.ServiceState;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.AlertSettings;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleDownConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.alerts.AlertTopics;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroup;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.StartMachinesException;
import com.elastisys.scale.cloudadapters.commons.resizeplanner.ResizePlan;
import com.elastisys.scale.cloudadapters.commons.resizeplanner.ResizePlanner;
import com.elastisys.scale.cloudadapters.commons.termqueue.ScheduledTermination;
import com.elastisys.scale.cloudadapters.commons.termqueue.TerminationQueue;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.schema.JsonValidator;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.net.smtp.SmtpServerSettings;
import com.elastisys.scale.commons.net.smtp.alerter.Alert;
import com.elastisys.scale.commons.net.smtp.alerter.AlertSeverity;
import com.elastisys.scale.commons.net.smtp.alerter.EmailAlerter;
import com.elastisys.scale.commons.net.smtp.alerter.SendSettings;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Atomics;
import com.google.gson.JsonObject;

/**
 * A generic {@link CloudAdapter} that is provided as a basis for building
 * cloud-specific {@link CloudAdapter}s.
 * <p/>
 * The {@link BaseCloudAdapter} implements sensible behavior for the
 * {@link CloudAdapter} methods and relieves implementors from dealing with the
 * details of continuously monitoring and re-scaling the pool with the right
 * amount of machines given the member machine states, handling
 * (re-)configurations, sending alerts, etc. Implementers of cloud-specific
 * {@link CloudAdapter}s only need to implements a small set of machine pool
 * management primitives for a particular cloud. These management primitives are
 * supplied to the {@link BaseCloudAdapter} at construction-time in the form of
 * a {@link ScalingGroup}, which implements the management primitives according
 * to the API of the targeted cloud.
 * <p/>
 * The configuration ({@link BaseCloudAdapterConfig}) for this adapter specifies
 * how the {@link BaseCloudAdapter}:
 * <ul>
 * <li>should configure its {@link ScalingGroup} to allow it to communicate with
 * its cloud API (the {@code scalingGroup} key).</li>
 * <li>provisions new instances when the scaling group needs to grow (the
 * {@code scaleUpConfig} key).</li>
 * <li>decommissions instances when the scaling group needs to shrink (the
 * {@code scaleDownConfig} key).</li>
 * <li>alerts system administrators (via email) of interesting events: resize
 * operations, error conditions, etc (the {@code alerts} key).</li>
 * </ul>
 * A configuration document may look as follows:
 *
 * <pre>
 * {
 *   "scalingGroup": {
 *     "name": "MyScalingGroup",
 *     "config": {
 *       "awsAccessKeyId": "ABC...XYZ",
 *       "awsSecretAccessKey": "abc...xyz",
 *       "region": "us-east-1"
 *     }
 *   },
 *   "scaleUpConfig": {
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
 *   "scaleDownConfig": {
 *     "victimSelectionPolicy": "CLOSEST_TO_INSTANCE_HOUR",
 *     "instanceHourMargin": 0
 *   },
 *   "alerts": {
 *     "subject": "[elastisys:scale] scaling group alert for MyScalingGroup",
 *     "recipients": ["receiver@destination.com"],
 *     "sender": "noreply@elastisys.com",
 *     "mailServer": {
 *       "smtpHost": "mail.server.com",
 *       "smtpPort": 465,
 *       "authentication": {"userName": "john", "password": "secret"},
 *       "useSsl": True
 *     }
 *   },
 *   "poolUpdatePeriod": 60
 * }
 * </pre>
 *
 * The {@link BaseCloudAdapter} operates according to the {@link CloudAdapter}
 * contract. Some details on how the {@link BaseCloudAdapter} satisfies the
 * contract are summarized below.
 * <p/>
 * <h3>Configuration:</h2>
 *
 * When {@link #configure} is called, the {@link BaseCloudAdapter} expects a
 * JSON document that validates against its JSON Schema (as returned by
 * {@link #getConfigurationSchema()}). The entire configuration document is
 * passed on to the {@link ScalingGroup} via a call to
 * {@link ScalingGroup#configure}. The parts of the configuration that are of
 * special interest to the {@link ScalingGroup}, such as cloud login details and
 * scaling group name, are located under the {@code scalingGroup} key. The
 * {@code scalingGroup/config} configuration key holds implementation-specific
 * settings for the particular {@link ScalingGroup} implementation. An example
 * configuration is given above.
 *
 * <h3>Identifying group members:</h2>
 *
 * When {@link #getMachinePool} is called, the scaling group members are
 * identified via a call to {@link ScalingGroup#listMachines()}.
 *
 * <h3>Handling resize requests:</h3>
 *
 * When {@link #setDesiredSize(int)} is called, the {@link BaseCloudAdapter}
 * notes the new desired size but does not immediately apply the necessary
 * changes to the machine pool. Instead, pool updates are carried out in a
 * periodical manner (with a period specified by the {@code poolUpdatePeriod}
 * configuration key).
 * <p/>
 * When a pool update is triggered, the actions taken depend on if the scaling
 * group needs to grow or shrink.
 *
 * <ul>
 * <li><i>scale up</i>: start by sparing machines from termination if the
 * termination queue is non-empty. For any remaining instances: request them to
 * be started by the ScalingGroup via {@link ScalingGroup#startMachines}. The
 * {@code scaleUpConfig} is passed to the {@link ScalingGroup}.</li>
 * <li><i>scale down</i>: start by terminating any machines in
 * {@link MachineState#REQUESTED} state, since these are likely to not yet incur
 * cost. Any such machines are terminated immediately. If additional capacity is
 * to be removed, select a victim according to the configured
 * {@code victimSelectionPolicy} and schedule it for termination according to
 * the configured {@code instanceHourMargin}. Each instance termination is
 * delegated to {@link ScalingGroup#terminateMachine(String)}.</li>
 * </ul>
 *
 * <h3>Alerts:</h3>
 *
 * If an alerts attribute is present in the configuration, the
 * {@link BaseCloudAdapter} will send alert emails to notify selected recipients
 * of interesting events (such as errors, scale-ups/scale-downs, liveness state
 * changes, etc).
 *
 * @see ScalingGroup
 */
public class BaseCloudAdapter implements CloudAdapter {

	/** {@link Logger} instance. */
	static final Logger LOG = LoggerFactory.getLogger(BaseCloudAdapter.class);

	/** Maximum concurrency in the {@link #executorService}. */
	private static final int MAX_CONCURRENCY = 20;

	/**
	 * The original undecorated (no liveness checking) {@link ScalingGroup}
	 * reference managed by this {@link BaseCloudAdapter}.
	 */
	private final ScalingGroup wrappedScalingGroup;
	/** The (possibly) liveness-check decorated {@link ScalingGroup}. */
	private ScalingGroup scalingGroup = null;

	/**
	 * The JSON Schema that describes the range of allowed configuration
	 * documents for the {@link BaseCloudAdapter}.
	 */
	private final JsonObject jsonSchema;
	/** The currently set configuration. */
	private final AtomicReference<BaseCloudAdapterConfig> config;
	/** {@link ExecutorService} handling execution of "background jobs". */
	private final ScheduledExecutorService executorService;
	/** <code>true</code> if adapter has been started. */
	private final AtomicBoolean started;

	/** The desired size of the machine pool. */
	private final AtomicReference<Integer> desiredSize;

	/**
	 * {@link EventBus} used to post {@link Alert} events that are to be
	 * forwarded by the {@link EmailAlerter} (if configured).
	 */
	private final EventBus eventBus;
	/**
	 * If email alerts are configured, will hold an {@link EmailAlerter} that is
	 * registered with the {@link EventBus} to forward {@link Alert}s to a list
	 * of configured email recipients.
	 */
	private final AtomicReference<EmailAlerter> smtpAlerter;
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
	 * Constructs a new {@link BaseCloudAdapter} managing a given
	 * {@link ScalingGroup}.
	 *
	 * @param scalingGroup
	 *            A cloud-specific management client for the scaling group.
	 */
	public BaseCloudAdapter(ScalingGroup scalingGroup) {
		this(scalingGroup, new EventBus());
	}

	/**
	 * Constructs a new {@link BaseCloudAdapter} managing a given
	 * {@link ScalingGroup} and with an {@link EventBus} provided by the caller.
	 *
	 * @param scalingGroup
	 *            A cloud-specific management client for the scaling group.
	 * @param eventBus
	 *            The {@link EventBus} used to send {@link Alert}s and event
	 *            messages between components of the cloud adapter.
	 */
	public BaseCloudAdapter(ScalingGroup scalingGroup, EventBus eventBus) {
		checkArgument(scalingGroup != null, "scalingGroup is null");
		checkArgument(eventBus != null, "eventBus is null");

		this.wrappedScalingGroup = scalingGroup;
		this.scalingGroup = null;
		this.eventBus = eventBus;

		this.jsonSchema = JsonUtils
				.parseJsonResource("baseadapter-schema.json");
		this.executorService = Executors
				.newScheduledThreadPool(MAX_CONCURRENCY);

		this.config = Atomics.newReference();
		this.started = new AtomicBoolean(false);

		this.smtpAlerter = Atomics.newReference();

		this.terminationQueue = new TerminationQueue();
		this.desiredSize = Atomics.newReference();
	}

	@Override
	public void configure(JsonObject jsonConfig) throws CloudAdapterException {
		BaseCloudAdapterConfig configuration = validate(jsonConfig);

		synchronized (this.updateLock) {
			this.config.set(configuration);

			if (isStarted()) {
				stop();
			}
			start();
		}
	}

	private BaseCloudAdapterConfig validate(JsonObject jsonConfig)
			throws CloudAdapterException {
		try {
			JsonValidator.validate(this.jsonSchema, jsonConfig);
			BaseCloudAdapterConfig configuration = JsonUtils.toObject(
					jsonConfig, BaseCloudAdapterConfig.class);
			configuration.validate();
			return configuration;
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, CloudAdapterException.class);
			throw new CloudAdapterException(
					"failed to validate cloud adapter configuration: "
							+ e.getMessage(), e);
		}
	}

	@Override
	public Optional<JsonObject> getConfigurationSchema() {
		return Optional.of(this.jsonSchema);
	}

	@Override
	public Optional<JsonObject> getConfiguration() {
		BaseCloudAdapterConfig currentConfig = this.config.get();
		if (currentConfig == null) {
			return Optional.absent();
		}
		return Optional.of(JsonUtils.toJson(currentConfig).getAsJsonObject());
	}

	private void start() throws CloudAdapterException {
		checkState(getConfiguration().isPresent(),
				"attempt to start cloud adapter before being configured");
		if (isStarted()) {
			return;
		}
		LOG.info("starting {} driving a {}", getClass().getSimpleName(),
				this.wrappedScalingGroup.getClass().getSimpleName());

		// initialize to original scaling group (without liveness tracking)
		this.scalingGroup = this.wrappedScalingGroup;

		// re-configure scalinggroup
		LOG.info("configuring scaling group '{}'", config().getScalingGroup()
				.getName());
		this.scalingGroup.configure(config());
		determineDesiredSizeIfUnset();

		// start pool update task that periodically runs updateMachinepool()
		int poolUpdatePeriod = config().getPoolUpdatePeriod();
		this.poolUpdateTask = this.executorService.scheduleWithFixedDelay(
				new PoolUpdateTask(), poolUpdatePeriod, poolUpdatePeriod,
				TimeUnit.SECONDS);

		setUpSmtpAlerter(config());
		this.started.set(true);
		LOG.info(getClass().getSimpleName() + " started.");
	}

	/**
	 * In case no {@link #desiredSize} has been set yet, this method determines
	 * the (initial) desired size for the {@link ScalingGroup} as the current
	 * size of the {@link ScalingGroup}. On failure to determine the pool size
	 * (for example, due to a temporary cloud provider API outage), an alert is
	 * sent out (if alerting has been set up).
	 */
	private void determineDesiredSizeIfUnset() {
		if (this.desiredSize.get() != null) {
			return;
		}

		try {
			LOG.debug("determining initial desired scaling group size");
			setDesiredSizeIfUnset(getMachinePool());
		} catch (CloudAdapterException e) {
			String message = format(
					"failed to determine initial size of scaling group: %s\n%s",
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
		// exclude out-of-service instances since they aren't actually part
		// of the desiredSize (they have been replaced with stand-ins)
		int effectiveSize = pool.getEffectiveMachines().size();
		int allocated = pool.getAllocatedMachines().size();
		int outOfService = pool.getOutOfServiceMachines().size();
		this.desiredSize.set(effectiveSize);
		LOG.info("initial desiredSize is {} (allocated: {}, outOfService: {})",
				this.desiredSize, allocated, outOfService);
	}

	private void stop() {
		if (isStarted()) {
			LOG.debug("stopping {} ...", getClass().getSimpleName());
			// cancel tasks (allow any running tasks to finish)
			this.poolUpdateTask.cancel(false);
			this.poolUpdateTask = null;
			takeDownSmtpAlerter();
			this.started.set(false);
		}
		LOG.info(getClass().getSimpleName() + " stopped.");
	}

	boolean isStarted() {
		return this.started.get();
	}

	@Override
	public MachinePool getMachinePool() throws CloudAdapterException {
		checkState(getConfiguration().isPresent(),
				"cloud adapter needs to be configured before use");

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
	public PoolSizeSummary getPoolSize() throws CloudAdapterException {
		checkState(getConfiguration().isPresent(),
				"cloud adapter needs to be configured before use");

		MachinePool pool = getMachinePool();
		return new PoolSizeSummary(this.desiredSize.get(), pool
				.getAllocatedMachines().size(), pool.getOutOfServiceMachines()
				.size());
	}

	/**
	 * Lists the {@link Machine}s in the {@link ScalingGroup}. Raises a
	 * {@link CloudAdapterException} on failure and sends alert (if configured).
	 *
	 * @return
	 *
	 * @throws CloudAdapterException
	 */
	private List<Machine> listMachines() {
		return this.scalingGroup.listMachines();
	}

	@Override
	public void setDesiredSize(int desiredSize)
			throws IllegalArgumentException, CloudAdapterException {
		checkState(getConfiguration().isPresent(),
				"cloud adapter needs to be configured before use");
		checkArgument(desiredSize >= 0, "negative desired pool size");

		// prevent concurrent pool modifications
		synchronized (this.updateLock) {
			LOG.info("set desiredSize to {}", desiredSize);
			this.desiredSize.set(desiredSize);
		}
	}

	@Override
	public void terminateMachine(String machineId, boolean decrementDesiredSize)
			throws IllegalArgumentException, CloudAdapterException {
		checkState(getConfiguration().isPresent(),
				"cloud adapter needs to be configured before use");

		// prevent concurrent pool modifications
		synchronized (this.updateLock) {
			LOG.debug("terminating {}", machineId);
			this.scalingGroup.terminateMachine(machineId);
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
			throws IllegalArgumentException, CloudAdapterException {
		checkState(getConfiguration().isPresent(),
				"cloud adapter needs to be configured before use");

		// prevent concurrent pool modifications
		synchronized (this.updateLock) {
			LOG.debug("attaching instance {} to group", machineId);
			this.scalingGroup.attachMachine(machineId);
			// implicitly increases pool size
			setDesiredSize(this.desiredSize.get() + 1);
		}
		attachAlert(machineId);
	}

	@Override
	public void detachMachine(String machineId, boolean decrementDesiredSize)
			throws IllegalArgumentException, CloudAdapterException {
		checkState(getConfiguration().isPresent(),
				"cloud adapter needs to be configured before use");

		// prevent concurrent pool modifications
		synchronized (this.updateLock) {
			LOG.debug("detaching {} from group", machineId);
			this.scalingGroup.detachMachine(machineId);
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
				"cloud adapter needs to be configured before use");

		LOG.debug("service state {} assigned to {}", serviceState.name(),
				machineId);
		this.scalingGroup.setServiceState(machineId, serviceState);
		serviceStateAlert(machineId, serviceState);
	}

	/**
	 * Sets up an {@link EmailAlerter}, in case the configuration contains an
	 * {@link AlertSettings}.
	 *
	 * @param configuration
	 */
	private void setUpSmtpAlerter(BaseCloudAdapterConfig configuration) {
		AlertSettings alertsConfig = configuration.getAlerts();
		if (alertsConfig != null) {
			LOG.debug("configuring SMTP alerter.");
			SendSettings sendSettings = new SendSettings(
					alertsConfig.getRecipients(), alertsConfig.getSender(),
					alertsConfig.getSubject(), alertsConfig.getSeverityFilter());
			SmtpServerSettings mailServerSettings = alertsConfig
					.getMailServer().toSmtpServerSettings();
			this.smtpAlerter.set(new EmailAlerter(mailServerSettings,
					sendSettings, standardAlertTags()));
			this.eventBus.register(this.smtpAlerter.get());
		}
	}

	/**
	 * Standard {@link Alert} tags to include in all {@link Alert} mails sent by
	 * the {@link EmailAlerter}.
	 *
	 * @return
	 */
	private Map<String, String> standardAlertTags() {
		Map<String, String> standardTags = Maps.newHashMap();
		List<String> ipv4Addresses = Lists.newArrayList();
		for (InetAddress inetAddr : HostUtils.hostIpv4Addresses()) {
			ipv4Addresses.add(inetAddr.getHostAddress());
		}
		String ipAddresses = Joiner.on(", ").join(ipv4Addresses);
		standardTags.put("cloudAdapterIps", ipAddresses);
		standardTags.put("scalingGroup", config().getScalingGroup().getName());
		return standardTags;
	}

	/**
	 * Unregisters the {@link EmailAlerter} from the {@link EventBus}, if an
	 * {@link EmailAlerter} has been set up.
	 */
	private void takeDownSmtpAlerter() {
		if (this.smtpAlerter.get() != null) {
			this.eventBus.unregister(this.smtpAlerter.get());
		}
	}

	BaseCloudAdapterConfig config() {
		return this.config.get();
	}

	private String scalingGroup() {
		return config().getScalingGroup().getName();
	}

	private ScaleUpConfig scaleUpConfig() {
		return config().getScaleUpConfig();
	}

	private ScaleDownConfig scaleDownConfig() {
		return config().getScaleDownConfig();
	}

	/**
	 * Updates the size of the machine pool to match the currently set desired
	 * size. This may involve terminating termination-due machines and placing
	 * new server requests to replace terminated servers.
	 * <p/>
	 * Waits for the {@link #updateLock} to avoid concurrent pool updates.
	 *
	 * @throws CloudAdapterException
	 */
	void updateMachinePool() throws CloudAdapterException {
		// check if we need to determine desired size (it may not have been
		// possible on startup, e.g., due to cloud API being ureachable)
		determineDesiredSizeIfUnset();
		if (this.desiredSize.get() == null) {
			LOG.warn("cannot update scaling group: haven't been able to "
					+ "determine initial desired size");
			return;
		}

		// prevent multiple threads from concurrently updating pool
		synchronized (this.updateLock) {
			int targetSize = this.desiredSize.get();
			try {
				doPoolUpdate(targetSize);
			} catch (Throwable e) {
				String message = format("failed to adjust scaling group "
						+ "\"%s\" to desired size %d: %s\n%s", scalingGroup(),
						targetSize, e.getMessage(),
						Throwables.getStackTraceAsString(e));
				this.eventBus.post(new Alert(AlertTopics.RESIZE.name(),
						AlertSeverity.ERROR, UtcTime.now(), message));
				throw new CloudAdapterException(message, e);
			}
		}
	}

	private void doPoolUpdate(int newSize) throws CloudAdapterException {
		LOG.info("updating pool size to desired size {}", newSize);

		MachinePool pool = getMachinePool();
		// Clean out obsolete machines from termination queue. Note: this also
		// cleans out machines that have been taken out of service after being
		// scheduled for termination.
		int poolSize = pool.getEffectiveMachines().size();
		this.terminationQueue.filter(pool.getEffectiveMachines());
		ResizePlanner resizePlanner = new ResizePlanner(pool,
				this.terminationQueue, scaleDownConfig()
						.getVictimSelectionPolicy(), scaleDownConfig()
						.getInstanceHourMargin());
		int netSize = resizePlanner.getEffectiveSize();

		ResizePlan resizePlan = resizePlanner.calculateResizePlan(newSize);
		if (resizePlan.isScaleUp()) {
			List<Machine> startedMachines = scaleOut(poolSize, resizePlan);
			poolSize += startedMachines.size();
		} else if (resizePlan.isScaleDown()) {
			List<ScheduledTermination> terminations = resizePlan
					.getToTerminate();
			LOG.info("scheduling {} server(s) for termination",
					terminations.size());
			for (ScheduledTermination termination : terminations) {
				this.terminationQueue.add(termination);
				LOG.debug("scheduling server {} for termination at {}",
						termination.getInstance().getId(),
						termination.getTerminationTime());
			}
			LOG.debug("termination queue: {}", this.terminationQueue);
		} else {
			LOG.info("scaling group is already properly sized ({})", netSize);
		}
		// effectuate scheduled terminations that are (over)due
		List<Machine> terminated = terminateOverdueMachines();
		if (!terminated.isEmpty()) {
			scaleInAlert(poolSize, terminated);
		}
	}

	private List<Machine> scaleOut(int originalPoolSize, ResizePlan resizePlan)
			throws StartMachinesException {
		LOG.info("sparing {} machine(s) from termination, "
				+ "placing {} new request(s)", resizePlan.getToSpare(),
				resizePlan.getToRequest());
		this.terminationQueue.spare(resizePlan.getToSpare());

		try {
			List<Machine> startedMachines = this.scalingGroup.startMachines(
					resizePlan.getToRequest(), scaleUpConfig());
			scaleOutAlert(originalPoolSize, startedMachines);
			return startedMachines;
		} catch (StartMachinesException e) {
			// may have failed part-way through. notify of machines that were
			// started before error occurred.
			scaleOutAlert(originalPoolSize, e.getStartedMachines());
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
				this.scalingGroup.terminateMachine(victimId);
				terminated.add(overdueInstance.getInstance());
			} catch (Exception e) {
				String message = format(
						"failed to terminate instance '%s': %s\n%s", victimId,
						e.getMessage(), Throwables.getStackTraceAsString(e));
				LOG.error(message);
				this.eventBus.post(new Alert(AlertTopics.RESIZE.name(),
						AlertSeverity.ERROR, UtcTime.now(), message));
			}
		}

		return terminated;
	}

	/**
	 * Post an {@link Alert} that the scaling group has grown.
	 *
	 * @param oldSize
	 *            The previous size of the group.
	 * @param startedMachines
	 *            The new machine instances that have been started.
	 */
	private void scaleOutAlert(Integer oldSize, List<Machine> startedMachines) {
		if (startedMachines.isEmpty()) {
			return;
		}

		int newSize = oldSize + startedMachines.size();
		String message = String.format(
				"size of scaling group \"%s\" changed from %d to %d",
				scalingGroup(), oldSize, newSize);
		LOG.info(message);
		Map<String, String> tags = Maps.newHashMap();
		List<String> startedMachineIds = Lists.newArrayList();
		for (Machine startedMachine : startedMachines) {
			startedMachineIds.add(startedMachine.getId());
		}
		tags.put("startedMachines", Joiner.on(", ").join(startedMachineIds));
		this.eventBus.post(new Alert(AlertTopics.RESIZE.name(),
				AlertSeverity.INFO, UtcTime.now(), message, tags));
	}

	/**
	 * Post an {@link Alert} that the scaling group has shrunk.
	 *
	 * @param oldSize
	 *            The previous size of the group.
	 * @param terminatedMachines
	 *            The machine instances that were terminated.
	 */
	private void scaleInAlert(Integer oldSize, List<Machine> terminatedMachines) {
		int newSize = oldSize - terminatedMachines.size();
		String message = String.format(
				"size of scaling group \"%s\" changed from %d to %d",
				scalingGroup(), oldSize, newSize);
		LOG.info(message);
		Map<String, String> tags = Maps.newHashMap();
		List<String> terminatedMachineIds = Lists.newArrayList();
		for (Machine terminatedMachine : terminatedMachines) {
			terminatedMachineIds.add(terminatedMachine.getId());
		}
		tags.put("terminatedMachines",
				Joiner.on(", ").join(terminatedMachineIds));
		this.eventBus.post(new Alert(AlertTopics.RESIZE.name(),
				AlertSeverity.INFO, UtcTime.now(), message, tags));
	}

	/**
	 * Post an {@link Alert} that a machine was terminated from the group.
	 *
	 * @param machineId
	 */
	private void terminationAlert(String machineId) {
		Map<String, String> tags = ImmutableMap.of("terminatedMachines",
				machineId);
		String message = String.format("Terminated machine %s.", machineId);
		this.eventBus.post(new Alert(AlertTopics.RESIZE.name(),
				AlertSeverity.INFO, UtcTime.now(), message, tags));
	}

	/**
	 * Post an {@link Alert} that a machine was attached to the group.
	 *
	 * @param machineId
	 */
	private void attachAlert(String machineId) {
		Map<String, String> tags = ImmutableMap.of("attachedMachines",
				machineId);
		String message = String.format("Attached machine %s to scaling group.",
				machineId);
		this.eventBus.post(new Alert(AlertTopics.RESIZE.name(),
				AlertSeverity.INFO, UtcTime.now(), message, tags));
	}

	/**
	 * Post an {@link Alert} that a machine was detached from the group.
	 *
	 * @param machineId
	 */
	private void detachAlert(String machineId) {
		Map<String, String> tags = ImmutableMap.of("detachedMachines",
				machineId);
		String message = String.format(
				"Detached machine %s from scaling group.", machineId);
		this.eventBus.post(new Alert(AlertTopics.RESIZE.name(),
				AlertSeverity.INFO, UtcTime.now(), message, tags));
	}

	/**
	 * Post an {@link Alert} that a group member had its {@link ServiceState}
	 * set.
	 *
	 * @param machineId
	 */
	private void serviceStateAlert(String machineId, ServiceState state) {
		Map<String, String> tags = ImmutableMap.of();
		String message = String.format(
				"Service state set to %s for machine %s.", state.name(),
				machineId);
		this.eventBus.post(new Alert(AlertTopics.SERVICE_STATE.name(),
				AlertSeverity.INFO, UtcTime.now(), message, tags));
	}

	/**
	 * Task that, when executed, runs
	 * {@link BaseCloudAdapter#updateMachinePool()}.
	 */
	public class PoolUpdateTask implements Runnable {
		@Override
		public void run() {
			try {
				updateMachinePool();
			} catch (CloudAdapterException e) {
				LOG.error(
						format("machine pool update task failed: %s",
								e.getMessage()), e);
			}
		}
	}

}
