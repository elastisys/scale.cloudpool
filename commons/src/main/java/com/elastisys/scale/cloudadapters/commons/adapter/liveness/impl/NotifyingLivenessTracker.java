package com.elastisys.scale.cloudadapters.commons.adapter.liveness.impl;

import static com.google.common.base.Preconditions.checkState;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.LivenessState;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.LivenessConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.LivenessTestResult;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.LivenessTracker;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.SshLivenessTest;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.SshLivenessTestFactory;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.impl.StandardSshLivenessTest;
import com.elastisys.scale.commons.net.smtp.alerter.Alert;
import com.elastisys.scale.commons.net.ssh.SshCommandResult;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Atomics;

/**
 * A {@link LivenessTracker} that reports observed {@link LivenessState} changes
 * for {@link Machine}s to an {@link EventBus}.
 * 
 * @see LivenessStateChange
 * 
 * 
 * 
 */
public class NotifyingLivenessTracker implements LivenessTracker {

	static Logger LOG = LoggerFactory.getLogger(NotifyingLivenessTracker.class);

	/**
	 * The factory that produces liveness test tasks when the
	 * {@link LivenessTracker} needs to execute a boot-time or run-time liveness
	 * test.
	 */
	private final SshLivenessTestFactory livenessTestFactory;

	/**
	 * The {@link EventBus} on which liveness state chance {@link Alert}s are
	 * posted.
	 */
	private final EventBus eventBus;
	/** Executor used to execute liveness tests in background threads. */
	private final ExecutorService executor;

	private final AtomicReference<LivenessConfig> config;
	/**
	 * Map of last observed {@link LivenessState}s for {@link Machine}s. Keyed
	 * on {@link Machine} ids.
	 */
	private final Map<String, LivenessState> livenessObservations;

	/**
	 * Creates a new {@link NotifyingLivenessTracker}.
	 * 
	 * @param livenessTestFactory
	 *            A factory that produces liveness test tasks when the
	 *            {@link LivenessTracker} needs to execute a boot-time or
	 *            run-time liveness test.
	 * @param eventBus
	 *            The {@link EventBus} on which liveness state chance
	 *            {@link Alert}s are posted.
	 * @param executor
	 *            Executor used to execute liveness tests in background threads.
	 */
	public NotifyingLivenessTracker(SshLivenessTestFactory livenessTestFactory,
			EventBus eventBus, ExecutorService executor) {
		this.livenessTestFactory = livenessTestFactory;
		this.eventBus = eventBus;
		this.executor = executor;

		this.config = Atomics.newReference();
		this.livenessObservations = new ConcurrentHashMap<>();
	}

	@Override
	public void configure(LivenessConfig config) throws CloudAdapterException {
		config.validate();
		this.config.set(config);
	}

	@Override
	public Future<LivenessTestResult> checkBootLiveness(Machine machine) {
		ensureConfigured();

		this.livenessObservations.put(machine.getId(), LivenessState.BOOTING);

		bootAlert(machine);

		LOG.debug("checking boot-time liveness for {}", machine.getId());
		SshLivenessTest bootCheck = this.livenessTestFactory
				.createBootTimeCheck(machine, config());
		return this.executor.submit(new LivenessCheckRunner(bootCheck));
	}

	@Override
	public Future<LivenessTestResult> checkRuntimeLiveness(Machine machine) {
		ensureConfigured();

		// if first time machine is encountered, set UNKNOWN liveness state
		getLiveness(machine);

		LOG.debug("checking run-time liveness for {}", machine.getId());
		SshLivenessTest runtimeCheck = this.livenessTestFactory
				.createRunTimeCheck(machine, config());

		return this.executor.submit(new LivenessCheckRunner(runtimeCheck));
	}

	@Override
	public LivenessState getLiveness(Machine machine) {
		ensureConfigured();

		if (LOG.isTraceEnabled()) {
			LOG.trace("asked for liveness of machine {}, "
					+ "with observations: {}", machine.getId(),
					this.livenessObservations);
		}

		if (!this.livenessObservations.containsKey(machine.getId())) {
			this.livenessObservations.put(machine.getId(),
					LivenessState.UNKNOWN);
		}
		return this.livenessObservations.get(machine.getId());
	}

	private void ensureConfigured() {
		checkState(isConfigured(),
				"attempt to use liveness tracker before configuring it");
	}

	boolean isConfigured() {
		return config() != null;
	}

	LivenessConfig config() {
		return this.config.get();
	}

	/**
	 * Post an Alert on the {@link EventBus} to signal that a machine has
	 * started booting.
	 * 
	 * @param machine
	 */
	private void bootAlert(Machine machine) {
		LivenessStateChange bootStateChange = new LivenessStateChange(machine,
				null, LivenessState.BOOTING);
		Alert bootAlert = bootStateChange.toAlert();
		LOG.info("liveness state change: " + bootStateChange);
		this.eventBus.post(bootAlert);
	}

	/**
	 * Update the last observed liveness state for a machine based on a liveness
	 * test result, and post an Alert on the {@link EventBus} if the liveness
	 * state of the machine has changed.
	 * 
	 * @param livenessTestResult
	 */
	private void updateStateAndAlertOnChange(
			LivenessTestResult livenessTestResult) {
		Machine machine = livenessTestResult.getMachine();
		LivenessState newState = livenessTestResult.getState();

		String id = machine.getId();
		LivenessState previousState = this.livenessObservations.get(id);
		this.livenessObservations.put(machine.getId(), newState);

		if ((previousState == null) || (previousState != newState)) {
			LivenessStateChange livenessChange = new LivenessStateChange(
					machine, previousState, newState);
			LOG.info("liveness state change: " + livenessChange);
			Alert alert = livenessChange.toAlert();
			if (livenessTestResult.getError().isPresent()) {
				alert = alert.withTag("commandError", livenessTestResult
						.getError().get().getMessage());
			}
			if (livenessTestResult.getCommandResult().isPresent()) {
				SshCommandResult command = livenessTestResult
						.getCommandResult().get();
				alert = alert.withTag("commandExitCode",
						String.valueOf(command.getExitStatus()));
				alert = alert.withTag("commandStdout", command.getStdout());
				alert = alert.withTag("commandStderr", command.getStderr());
			}
			this.eventBus.post(alert);
		}
	}

	/**
	 * A task that runs a {@link StandardSshLivenessTest} and registers the
	 * {@link LivenessState} for the {@link Machine} on completion.
	 * 
	 * 
	 * 
	 */
	private class LivenessCheckRunner implements Callable<LivenessTestResult> {

		private final SshLivenessTest livenessCheckTask;

		public LivenessCheckRunner(SshLivenessTest livenessTestTask) {
			this.livenessCheckTask = livenessTestTask;
		}

		@Override
		public LivenessTestResult call() throws Exception {
			Machine machine = this.livenessCheckTask.getMachine();

			LivenessTestResult result = null;
			try {
				SshCommandResult commandResult = this.livenessCheckTask.call();
				LivenessState state = (commandResult.getExitStatus() == 0) ? LivenessState.LIVE
						: LivenessState.UNHEALTHY;
				result = new LivenessTestResult(machine, state, commandResult);
			} catch (Exception e) {
				LOG.warn("liveness check failed: {}", e.getMessage());
				result = new LivenessTestResult(machine,
						LivenessState.UNHEALTHY, e);
			}
			updateStateAndAlertOnChange(result);
			return result;
		}
	}
}
