package com.elastisys.scale.cloudadapters.splitter.cloudadapter;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.PrioritizedRemoteCloudAdapter;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.StandardPrioritizedRemoteCloudAdapter;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config.PrioritizedRemoteCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.commands.GetMachinePoolCommand;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.commands.PoolResizeCommand;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.config.SplitterCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.poolcalculators.PoolSizeCalculationStrategy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.schema.JsonValidator;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Atomics;
import com.google.gson.JsonObject;

/**
 * A cloud adapter that splits up the {@link MachinePool} to a number of
 * configured {@link CloudAdapter}s. It communicates with the underlying
 * {@link CloudAdapter}s via their REST protocol endpoints.
 *
 * <p>
 * Cloud adapters are prioritized by a number in [0, 100]. The sums of these
 * priorities must equal 100, to ensure that we have an unambiguous
 * configuration. The splitter adapter will instruct its underlying adapters to
 * obtain the correct number of instances based on the priorities (in case of a
 * tie, e.g., two adapters each configured to handle 50% of the machine demand,
 * the first in the configuration file will win).
 * </p>
 *
 * <p>
 * This class is thread-safe. It guards its modification methods,
 * {@link #configure(JsonObject)} and {@link #resizeMachinePool(int)} with its
 * own intrinsic lock. It delegates thread-safety to thread-safe members for
 * {@link #getConfiguration()}, {@link #getConfigurationSchema()}, and
 * {@link #getMachinePool()}.
 * </p>
 */
public class SplitterCloudAdapter implements CloudAdapter {
	/**
	 * JSON Schema describing valid {@link OpenStackScalingGroupConfig}
	 * instances.
	 */
	private static final JsonObject CONFIG_SCHEMA = JsonUtils
			.parseJsonResource("splitter-adapter-config-schema.json");

	private static final Logger logger = LoggerFactory
			.getLogger(SplitterCloudAdapter.class);

	private final AtomicReference<SplitterCloudAdapterConfig> config;
	private final AtomicReference<ImmutableList<PrioritizedRemoteCloudAdapter>> adapters;
	private final ExecutorService executorService;
	private final AtomicReference<PoolSizeCalculationStrategy> poolSizeCalculator;

	private final Class<? extends PrioritizedRemoteCloudAdapter> adapterClass;

	/**
	 * Creates a new instance in an unconfigured state.
	 */
	public SplitterCloudAdapter() {
		this(StandardPrioritizedRemoteCloudAdapter.class);
	}

	/**
	 * Creates a new instance in an unconfigured state, with the ability to
	 * specify the implementation class of the remote cloud adapter (useful for
	 * testing).
	 *
	 * @param adapterClass
	 *            The class from which remote cloud adapters will be
	 *            instantiated.
	 */
	public SplitterCloudAdapter(
			Class<? extends PrioritizedRemoteCloudAdapter> adapterClass) {
		this.config = Atomics.newReference();
		this.adapters = Atomics.newReference();
		this.executorService = Executors.newCachedThreadPool();
		this.poolSizeCalculator = Atomics.newReference();
		this.adapterClass = adapterClass;
	}

	private SplitterCloudAdapterConfig getOrThrowConfig() {
		final SplitterCloudAdapterConfig config = this.config.get();

		checkNotNull(config,
				"Operation cannot be performed until adapter is configured!");

		return config;
	}

	/**
	 * This method is thread-safe by delegation (uses only a private final
	 * static constant).
	 */
	@Override
	public Optional<JsonObject> getConfigurationSchema() {
		// Thread-safe by design
		return Optional.of(CONFIG_SCHEMA);
	}

	/**
	 * This method is thread-safe, guarded by the class' intrinsic lock for the
	 * actual update part of the code, making changes to the configuration
	 * appear to be atomic.
	 */
	@Override
	public void configure(JsonObject configuration)
			throws IllegalArgumentException, CloudAdapterException {

		try {
			// validate against client config schema
			JsonValidator.validate(CONFIG_SCHEMA, configuration);

			// parse and validate entire configuration
			SplitterCloudAdapterConfig config = JsonUtils.toObject(
					configuration, SplitterCloudAdapterConfig.class);
			config.validate();

			synchronized (this) {
				/*
				 * The configuration, which has now passed checks, is activated
				 * atomically
				 */

				this.config.set(config);

				List<PrioritizedRemoteCloudAdapter> adapters = new LinkedList<PrioritizedRemoteCloudAdapter>();
				for (PrioritizedRemoteCloudAdapterConfig adapterConfiguration : config
						.getAdapterConfigurations()) {
					PrioritizedRemoteCloudAdapter adapter = this.adapterClass
							.newInstance();
					adapter.configure(adapterConfiguration);
					adapters.add(adapter);
				}
				this.adapters.set(ImmutableList
						.<PrioritizedRemoteCloudAdapter> copyOf(adapters));

				this.poolSizeCalculator.set(config
						.getPoolSizeCalculatorStrategy());
			}
		} catch (Exception e) {
			Throwables.propagateIfInstanceOf(e, CloudAdapterException.class);
			throw new CloudAdapterException(format(
					"failed to apply configuration: %s", e.getMessage()), e);
		}
	}

	/**
	 * This method is thread-safe by delegation to an {@link AtomicReference}.
	 */
	@Override
	public Optional<JsonObject> getConfiguration() {
		return Optional.of((JsonObject) JsonUtils.toJson(getOrThrowConfig()));
	}

	/**
	 * Queries all configured adapters and summarizes the results.
	 *
	 * This method is thread-safe by delegation to {@link AtomicReference}.
	 */
	@Override
	public MachinePool getMachinePool() throws CloudAdapterException {
		getOrThrowConfig();

		logger.debug("Will query machine pools...");

		final ImmutableList<PrioritizedRemoteCloudAdapter> adapters = this.adapters
				.get();

		Map<PrioritizedRemoteCloudAdapter, MachinePool> adapterToMachinePool = queryMachinePools(adapters);

		List<Machine> machines = new LinkedList<Machine>();
		for (MachinePool machinePool : adapterToMachinePool.values()) {
			machines.addAll(machinePool.getMachines());
		}

		final MachinePool machinePool = new MachinePool(machines,
				new DateTime());

		logger.debug(
				"Done querying machine pools: {} allocated machines ({} of which are active)",
				machinePool.getAllocatedMachines().size(), machinePool
						.getActiveMachines().size());

		return machinePool;
	}

	/**
	 * This method is made thread-safe by guarding it with the class' intrinsic
	 * lock.
	 */
	@Override
	public synchronized void resizeMachinePool(final int desiredSize)
			throws IllegalArgumentException, CloudAdapterException {
		getOrThrowConfig();

		logger.debug("Setting total desired size of machine pools to {}...",
				desiredSize);

		final ImmutableList<PrioritizedRemoteCloudAdapter> adapters = this.adapters
				.get();

		Map<PrioritizedRemoteCloudAdapter, Long> desiredPoolSizes = this.poolSizeCalculator
				.get().calculatePoolSizes(queryMachinePools(adapters),
						adapters, desiredSize);

		logger.debug("Will set the following desired pools {}",
				desiredPoolSizes);

		performPoolResizes(desiredPoolSizes);

		logger.debug("Done resizing machine pools");
	}

	/**
	 * Resizes the pools according to the given map between adapters and desired
	 * sizes.
	 *
	 * This method is thread-safe by relying on delegation to
	 * {@link AtomicReference} and its input.
	 *
	 * @param adapterToDesiredSize
	 *            The mapping between
	 * @throws CloudAdapterException
	 */
	private void performPoolResizes(
			Map<PrioritizedRemoteCloudAdapter, Long> adapterToDesiredSize)
					throws CloudAdapterException {
		Map<PrioritizedRemoteCloudAdapter, Future<Void>> adapterToResult = new HashMap<PrioritizedRemoteCloudAdapter, Future<Void>>();

		for (PrioritizedRemoteCloudAdapter adapter : adapterToDesiredSize
				.keySet()) {
			adapterToResult.put(adapter, this.executorService
					.submit(new PoolResizeCommand(adapter, adapterToDesiredSize
							.get(adapter))));
		}

		List<Throwable> errors = new LinkedList<Throwable>();
		for (PrioritizedRemoteCloudAdapter adapter : adapterToResult.keySet()) {
			try {
				adapterToResult.get(adapter).get();
			} catch (InterruptedException e) {
				logger.warn("Pool update interrupted for adapter {}", adapter);
			} catch (ExecutionException e) {
				logger.error(
						"Machine pool update for adapter {} failed due to {}",
						adapter, e.getCause());
				errors.add(e.getCause());
			}
		}

		// Did something go wrong?
		if (errors.size() > 0) {
			throw new CloudAdapterException(
					format("Failed to update the machine pools of %d cloud adapters, see logs",
							errors.size()));
		}
	}

	private Map<PrioritizedRemoteCloudAdapter, MachinePool> queryMachinePools(
			ImmutableList<PrioritizedRemoteCloudAdapter> adapters)
			throws CloudAdapterException {

		final Map<PrioritizedRemoteCloudAdapter, Future<MachinePool>> adapterToFutureMachinePool = new HashMap<PrioritizedRemoteCloudAdapter, Future<MachinePool>>();

		// Run each query in separate threads
		for (PrioritizedRemoteCloudAdapter adapter : adapters) {
			adapterToFutureMachinePool.put(adapter, this.executorService
					.submit(new GetMachinePoolCommand(adapter)));
		}

		// Collect results
		final Map<PrioritizedRemoteCloudAdapter, MachinePool> adapterToMachinePool = new HashMap<PrioritizedRemoteCloudAdapter, MachinePool>();
		final List<Throwable> errors = new LinkedList<Throwable>();
		for (PrioritizedRemoteCloudAdapter adapter : adapterToFutureMachinePool
				.keySet()) {
			try {
				MachinePool machinePool = adapterToFutureMachinePool.get(
						adapter).get();
				adapterToMachinePool.put(adapter, machinePool);
			} catch (InterruptedException e) {
				logger.warn("Machine pool listing for adapter {} interrupted!",
						adapter);
			} catch (ExecutionException e) {
				logger.error(
						"Machine pool listing for adapter {} failed due to {}",
						adapter, e.getCause());
				errors.add(e.getCause());
			}
		}

		// Did something go wrong?
		if (errors.size() > 0) {
			throw new CloudAdapterException(
					format("Failed to query the machine pool from %d cloud adapters, see logs",
							errors.size()));
		}

		logger.info("Adapter to machine pool mapping: {}", adapterToMachinePool);

		return adapterToMachinePool;
	}
}