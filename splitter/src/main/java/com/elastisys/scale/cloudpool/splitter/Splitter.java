package com.elastisys.scale.cloudpool.splitter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jersey.repackaged.com.google.common.base.Throwables;
import jersey.repackaged.com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.elastisys.scale.cloudpool.splitter.config.SplitterConfig;
import com.elastisys.scale.cloudpool.splitter.poolcalculators.PoolSizeCalculationStrategy;
import com.elastisys.scale.cloudpool.splitter.requests.RequestFactory;
import com.elastisys.scale.cloudpool.splitter.requests.http.HttpRequestFactory;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.schema.JsonValidator;
import com.elastisys.scale.commons.json.schema.JsonValidatorException;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Atomics;
import com.google.gson.JsonObject;

/**
 * A cloud pool that splits up the {@link MachinePool} over a number of
 * configured {@link CloudPool}s. It communicates with the backend
 * {@link CloudPool}s via their REST protocol endpoints.
 * <p/>
 * Cloud pools are prioritized by a number between [0, 100]. The sums of these
 * priorities must equal 100, to ensure that we have an unambiguous
 * configuration. The splitter will instruct its underlying pool to obtain the
 * correct number of instances based on the priorities (in case of a tie, e.g.,
 * two pools each configured to handle 50% of the machine demand, the first in
 * the configuration file will win).
 * <p/>
 * The following is an example of what a configuration document for the
 * {@link Splitter} could look like:
 *
 * <pre>
 * {
 *   "poolSizeCalculator": "STRICT",
 *   "backendPools": [
 *     {
 *       "priority": 40,
 *       "cloudPoolHost": "cloudpool-host-1",
 *       "cloudPoolPort": 8443
 *     },
 *     {
 *       "priority": 40,
 *       "cloudPoolHost": "cloudpool-host-2",
 *       "cloudPoolPort": 8443,
 *       "basicCredentials": {
 *         "username": "admin",
 *         "password": "adminpassword"
 *       }
 *     },
 *     {
 *       "priority": 20,
 *       "cloudPoolHost": "cloudpool-host-3",
 *       "cloudPoolPort": 8443,
 *       "certificateCredentials": {
 *         "keystorePath": "/path/to/keystore/goes/here",
 *         "keystorePassword": "keystorepassword",
 *         "keystoreType": "PKCS12"
 *       }
 *     }
 *   ],
 *   "poolUpdatePeriod": 60
 * }
 * </pre>
 */
public class Splitter implements CloudPool {
	private static final Logger LOG = LoggerFactory.getLogger(Splitter.class);
	/** Maximum concurrency in the {@link #executor}. */
	private static final int MAX_CONCURRENCY = 20;
	/** JSON Schema describing valid configurations for the cloud pool. */
	public static final JsonObject CONFIG_SCHEMA = JsonUtils
			.parseJsonResource("splitter-config-schema.json");

	/** The configuration set for the {@link Splitter}. */
	private final AtomicReference<SplitterConfig> config;

	/**
	 * A factory for generating {@link Callable} tasks that execute requests
	 * against the remote cloud pools.
	 */
	private final RequestFactory requestFactory;

	/**
	 * Executor to carry out cloud adaper requests and the periodical pool
	 * update task.
	 */
	private final ScheduledExecutorService executor;
	/**
	 * Pool update task that periodically runs the {@link #updateMachinePool()}
	 * method to update the desired size of the child cloud pools. Note that it
	 * is useful to continuously do this since calls to set the desired size on
	 * child pools can fail.
	 */
	private ScheduledFuture<?> poolUpdateTask;

	/** Lock to prevent concurrent updates of the pool state. */
	private final Object updateLock = new Object();

	/** <code>true</code> if the {@link Splitter} is in a started state. */
	private final AtomicBoolean started;

	/** The currently set total desired size of all child pools. */
	private Integer desiredSize;

	/**
	 * Constructs a {@link Splitter} in an unconfigured state.
	 */
	public Splitter() {
		this(new HttpRequestFactory());
	}

	/**
	 * Constructs a {@link Splitter} with a specific {@link RequestFactory}.
	 * Useful during testing.
	 *
	 * @param requestFactory
	 */
	public Splitter(RequestFactory requestFactory) {
		this.requestFactory = requestFactory;
		this.config = Atomics.newReference();
		this.executor = Executors.newScheduledThreadPool(MAX_CONCURRENCY);
		this.started = new AtomicBoolean(false);
		this.desiredSize = null;
	}

	@Override
	public Optional<JsonObject> getConfigurationSchema() {
		return Optional.of(CONFIG_SCHEMA);
	}

	@Override
	public void configure(JsonObject configuration)
			throws IllegalArgumentException, CloudPoolException {
		SplitterConfig config = validate(configuration);

		synchronized (this.updateLock) {
			this.config.set(config);
			if (isStarted()) {
				stop();
			}
			start();
		}
	}

	private SplitterConfig validate(JsonObject configuration) {
		try {
			JsonValidator.validate(CONFIG_SCHEMA, configuration);
			SplitterConfig config = JsonUtils.toObject(configuration,
					SplitterConfig.class);
			config.validate();
			return config;
		} catch (JsonValidatorException e) {
			Throwables.propagateIfInstanceOf(e, IllegalArgumentException.class);
			throw new IllegalArgumentException(String.format(
					"failed to validate configuration: %s", e.getMessage()), e);
		}
	}

	/**
	 * (Re-)start the pool update task.
	 */
	private void start() {
		if (isStarted()) {
			return;
		}
		determineDesiredSizeIfUnset();
		long poolUpdatePeriod = config().getPoolUpdatePeriod();
		this.poolUpdateTask = this.executor.scheduleWithFixedDelay(
				new PoolUpdateTask(), poolUpdatePeriod, poolUpdatePeriod,
				TimeUnit.SECONDS);
		this.started.set(true);
		LOG.info(getClass().getSimpleName() + " started");
	}

	/**
	 * Stop the pool update task.
	 */
	private void stop() {
		if (!isStarted()) {
			return;
		}
		this.poolUpdateTask.cancel(true);
		this.poolUpdateTask = null;
		this.started.set(false);
		LOG.info(getClass().getSimpleName() + " stopped");
	}

	private boolean isStarted() {
		return this.started.get();
	}

	@Override
	public Optional<JsonObject> getConfiguration() {
		SplitterConfig currentConfig = config();
		if (currentConfig == null) {
			return Optional.absent();
		}
		return Optional.of(JsonUtils.toJson(currentConfig).getAsJsonObject());
	}

	@Override
	public MachinePool getMachinePool() throws CloudPoolException {
		checkState(isConfigured(), "cannot use before being configured");

		LOG.debug("getting child pools ...");
		// dispatch call to every cloudpool and merge results.
		List<Callable<MachinePool>> requests = Lists.newArrayList();
		for (PrioritizedCloudPool pool : pools()) {
			requests.add(this.requestFactory.newGetMachinePoolRequest(pool));
		}
		try {
			List<MachinePool> subPools = inParallel(requests, requests.size());
			MachinePool pool = new MachinePool(merge(subPools), UtcTime.now());
			// if we haven't yet determined the desired size, we do so now
			setDesiredSizeIfUnset(pool);
			return pool;
		} catch (Exception e) {
			String message = String.format(
					"could not get pool from all child pools: %s",
					e.getMessage());
			throw new CloudPoolException(message, e);
		}
	}

	@Override
	public PoolSizeSummary getPoolSize() throws CloudPoolException {
		checkState(isConfigured(), "cannot use before being configured");

		MachinePool pool = getMachinePool();
		return new PoolSizeSummary(this.desiredSize, pool
				.getAllocatedMachines().size(), pool.getActiveMachines().size());
	}

	@Override
	public void setDesiredSize(int desiredSize)
			throws IllegalArgumentException, CloudPoolException {
		checkState(isConfigured(), "cannot use before being configured");
		checkArgument(desiredSize >= 0,
				"desiredSize cannot be a negative value");

		synchronized (this.updateLock) {
			LOG.info("setting desiredSize to {}", desiredSize);
			this.desiredSize = desiredSize;
			// calculate and propagate new desired sizes immediately to
			// sub-pools in order to not introduce unnecessary delay
			try {
				updateMachinePool();
			} catch (Exception e) {
				String message = String.format(
						"failed to update desired sizes for child pools "
								+ "(but will continue to retry): %s",
						e.getMessage());
				throw new CloudPoolException(message, e);
			}
		}
	}

	@Override
	public void terminateMachine(String machineId, boolean decrementDesiredSize)
			throws NotFoundException, CloudPoolException {
		checkState(isConfigured(), "cannot use before being configured");
		synchronized (this.updateLock) {
			LOG.debug("terminating machine '{}' ...", machineId);
			// dispatch call to every cloudpool and make sure one call is
			// successful
			List<Callable<Void>> requests = Lists.newArrayList();
			for (PrioritizedCloudPool pool : pools()) {
				requests.add(this.requestFactory.newTerminateMachineRequest(
						pool, machineId, decrementDesiredSize));
			}
			try {
				int mustComplete = 1;
				inParallel(requests, mustComplete);
			} catch (Exception e) {
				String message = String.format(
						"no child pool accepted call to terminate '%s': %s",
						machineId, e.getMessage());
				throw new CloudPoolException(message, e);
			}
			if (decrementDesiredSize) {
				// note: decrement unless desiredSize has been set to 0 (without
				// having been effectuated yet)
				this.desiredSize = Math.max(this.desiredSize - 1, 0);
				LOG.debug("decrementing desiredSize to {}", this.desiredSize);
			}
		}
	}

	@Override
	public void setServiceState(String machineId, ServiceState serviceState)
			throws NotFoundException, CloudPoolException {
		checkState(isConfigured(), "cannot use before being configured");

		LOG.debug("setting service state {} for {} ...", serviceState.name(),
				machineId);
		// dispatch call to every cloudpool and make sure one call is
		// successful
		List<Callable<Void>> requests = Lists.newArrayList();
		for (PrioritizedCloudPool pool : pools()) {
			requests.add(this.requestFactory.newSetServiceStateRequest(pool,
					machineId, serviceState));
		}
		try {
			int mustComplete = 1;
			inParallel(requests, mustComplete);
		} catch (Exception e) {
			String message = format(
					"no child pool accepted call to set service "
							+ "state on '%s': %s", machineId, e.getMessage());
			throw new CloudPoolException(message, e);
		}
	}

	@Override
	public void setMembershipStatus(String machineId,
			MembershipStatus membershipStatus) throws NotFoundException,
			CloudPoolException {
		checkState(isConfigured(), "cannot use before being configured");

		LOG.debug("setting membership status {} for {} ...", membershipStatus,
				machineId);
		// dispatch call to every cloudpool and make sure one call is
		// successful
		List<Callable<Void>> requests = Lists.newArrayList();
		for (PrioritizedCloudPool pool : pools()) {
			requests.add(this.requestFactory.newSetMembershipStatusRequest(
					pool, machineId, membershipStatus));
		}
		try {
			int mustComplete = 1;
			inParallel(requests, mustComplete);
		} catch (Exception e) {
			String message = format(
					"no child pool accepted call to set membership "
							+ "status on '%s': %s", machineId, e.getMessage());
			throw new CloudPoolException(message, e);
		}

	}

	@Override
	public void attachMachine(String machineId) throws NotFoundException,
			CloudPoolException {
		checkState(isConfigured(), "cannot use before being configured");

		synchronized (this.updateLock) {
			LOG.debug("attaching machine '{}' ...", machineId);
			// dispatch call to every cloudpool and make sure one call is
			// successful
			List<Callable<Void>> requests = Lists.newArrayList();
			for (PrioritizedCloudPool pool : pools()) {
				requests.add(this.requestFactory.newAttachMachineRequest(pool,
						machineId));
			}
			try {
				int mustComplete = 1;
				inParallel(requests, mustComplete);
			} catch (Exception e) {
				String message = String.format(
						"no child pool accepted call to attach '%s': %s",
						machineId, e.getMessage());
				throw new CloudPoolException(message, e);
			}
			// implicitly increments desired size
			this.desiredSize++;
			LOG.debug("incrementing desiredSize to {}", this.desiredSize);
		}
	}

	@Override
	public void detachMachine(String machineId, boolean decrementDesiredSize)
			throws NotFoundException, CloudPoolException {
		checkState(isConfigured(), "cannot use before being configured");
		synchronized (this.updateLock) {
			LOG.debug("detaching machine '{}' ...", machineId);
			// dispatch call to every cloudpool and make sure one call is
			// successful
			List<Callable<Void>> requests = Lists.newArrayList();
			for (PrioritizedCloudPool pool : pools()) {
				requests.add(this.requestFactory.newDetachMachineRequest(pool,
						machineId, decrementDesiredSize));
			}
			try {
				int mustComplete = 1;
				inParallel(requests, mustComplete);
			} catch (Exception e) {
				String message = String.format(
						"no child pool accepted call to detach '%s': %s",
						machineId, e.getMessage());
				throw new CloudPoolException(message, e);
			}
			if (decrementDesiredSize) {
				// note: decrement unless desiredSize has been set to 0 (without
				// having been effectuated yet)
				this.desiredSize = Math.max(this.desiredSize - 1, 0);
				LOG.debug("decrementing desiredSize to {}", this.desiredSize);
			}
		}
	}

	/**
	 * Updates the child pools according to the desired size and the configured
	 * priorities in a thread-safe manner.
	 */
	public void updateMachinePool() {
		synchronized (this.updateLock) {
			updateChildPools();
		}
	}

	/**
	 * Sets the desired sizes of the child pools.
	 */
	private void updateChildPools() {
		determineDesiredSizeIfUnset();
		if (this.desiredSize == null) {
			LOG.warn("cannot update pool size: no desired size has been "
					+ "set/determined yet");
			return;
		}
		Map<PrioritizedCloudPool, Integer> poolSizes = calculatePoolSizes();
		LOG.info("updating child pool sizes as follows: {}", poolSizes);

		// create setDesiredSize requests for all child pools
		List<Callable<Void>> requests = Lists.newArrayList();
		for (Entry<PrioritizedCloudPool, Integer> poolSize : poolSizes
				.entrySet()) {
			PrioritizedCloudPool pool = poolSize.getKey();
			int desiredSize = poolSize.getValue();
			requests.add(this.requestFactory.newSetDesiredSizeRequest(pool,
					desiredSize));
		}
		// dispatch calls to child pools
		try {
			inParallel(requests, requests.size());
		} catch (Exception e) {
			String message = format(
					"failed to set desired size on cloudpools: %s",
					e.getMessage());
			throw new CloudPoolException(message, e);
		}
	}

	protected Map<PrioritizedCloudPool, Integer> calculatePoolSizes() {
		PoolSizeCalculationStrategy calculator = config()
				.getPoolSizeCalculator().getCalculationStrategy();
		List<PrioritizedCloudPool> backendpools = config().getBackendPools();
		Map<PrioritizedCloudPool, Integer> poolSizes = calculator
				.calculatePoolSizes(backendpools, this.desiredSize);
		return poolSizes;
	}

	/**
	 * @return <code>true</code> if a configuration has been set.
	 */
	private boolean isConfigured() {
		return config() != null;
	}

	/**
	 * Returns the currently set configuration.
	 *
	 * @return
	 */
	SplitterConfig config() {
		return this.config.get();
	}

	Integer getDesiredSize() {
		return this.desiredSize;
	}

	RequestFactory getRequestFactory() {
		return this.requestFactory;
	}

	/**
	 * Merges all machines contained in a number of {@link MachinePool}s.
	 *
	 * @param pools
	 * @return
	 */
	private List<Machine> merge(List<MachinePool> pools) {
		List<Machine> merged = Lists.newArrayList();
		for (MachinePool pool : pools) {
			merged.addAll(pool.getMachines());
		}
		return merged;
	}

	/**
	 * Tries to determine and set the initial {@link #desiredSize}, if one
	 * hasn't already been determined or set via {@link #setDesiredSize(int)}.
	 * <p/>
	 * On failure to determine the desired size, a warning is logged and no
	 * exception is raised.
	 *
	 */
	private void determineDesiredSizeIfUnset() {
		if (this.desiredSize != null) {
			return;
		}

		try {
			LOG.info("trying to determine initial desired size ...");
			setDesiredSizeIfUnset(getMachinePool());
		} catch (Exception e) {
			LOG.warn("failed to determine initial pool size: {}",
					e.getMessage());
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
		if (this.desiredSize != null) {
			return;
		}
		// exclude out-of-service instances since they aren't actually part
		// of the desiredSize (they have been replaced with stand-ins)
		int effectiveSize = pool.getActiveMachines().size();
		int allocated = pool.getAllocatedMachines().size();
		int active = pool.getActiveMachines().size();
		this.desiredSize = effectiveSize;
		LOG.info("initial desiredSize is {} (allocated: {}, active: {})",
				this.desiredSize, allocated, active);
	}

	/**
	 * Executes a list of {@link Callable} tasks in parallel (in different
	 * threads) and requires at least {@code mustComplete} of them to return a
	 * value. The method returns a list of length {@code mustComplete} of return
	 * values. In case of more than {@code mustComplete} successful calls, which
	 * specific results are returned by the method is to be considered random.
	 *
	 * If not a sufficient number of calls produce a return value a
	 * {@link CloudPoolException} will be raised.
	 *
	 * @param tasks
	 *            The tasks to execute.
	 * @param mustComplete
	 *            The number of tasks that must complete with a return value.
	 *
	 * @return A list of length {@code mustComplete} with return values.
	 * @throws CloudPoolException
	 *             If not a sufficient number of tasks completed successfully.
	 */
	private <T> List<T> inParallel(List<Callable<T>> tasks, int mustComplete)
			throws CloudPoolException {
		checkArgument(mustComplete >= 0, "mustComplete must be >= 0");
		checkArgument(mustComplete <= tasks.size(),
				"mustComplete cannot be greater than number of tasks");

		List<T> returnValues = new ArrayList<>(mustComplete);
		List<Exception> errors = new ArrayList<>(tasks.size());
		try {
			// runs each request until completion
			List<Future<T>> results = this.executor.invokeAll(tasks);
			if (mustComplete == 0) {
				return returnValues;
			}
			// grab at least mustComplete return values and return them
			for (Future<T> result : results) {
				try {
					returnValues.add(result.get());
					if (returnValues.size() >= mustComplete) {
						return returnValues;
					}
				} catch (Exception e) {
					errors.add(e);
				}
			}
		} catch (InterruptedException e) {
			throw new CloudPoolException(
					"interrupted before finishing execution of all tasks: ", e);
		}
		// not sufficient number of return values
		MultiCauseException causes = new MultiCauseException(errors);
		throw new CloudPoolException(String.format(
				"%d call(s) successful, expected %d. errors: %s",
				returnValues.size(), mustComplete, causes.getMessage()), causes);
	}

	private List<PrioritizedCloudPool> pools() {
		return ImmutableList.copyOf(config().getBackendPools());
	}

	private class PoolUpdateTask implements Runnable {
		@Override
		public void run() {
			updateMachinePool();
		}
	}
}
