package com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.impl;

import static com.elastisys.scale.cloudadapers.api.types.Machine.withLivenessState;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static java.lang.String.format;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.types.LivenessState;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.LivenessTestResult;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.LivenessTracker;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroup;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.StartMachinesException;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * A {@link ScalingGroup} that tracks liveness states for machines observed in a
 * wrapped {@link ScalingGroup} and decorates the machines returned by the
 * wrapped {@link ScalingGroup} with liveness state.
 * <p/>
 * The runtime liveness of existing group members is continuously re-evaluated
 * by running periodical <i>runtime liveness checks</i>.
 * 
 * 
 * 
 */
public class LivenessTrackingScalingGroup implements ScalingGroup {
	static Logger LOG = LoggerFactory
			.getLogger(LivenessTrackingScalingGroup.class);

	/**
	 * The wrapped {@link ScalingGroup}, whose machines will be
	 * liveness-monitored.
	 */
	private final ScalingGroup delegate;
	/** {@link LivenessTracker} used to run periodical liveness tests. */
	private final LivenessTracker livenessTracker;
	/** Executor service used to execute liveness test tasks. */
	private final ScheduledExecutorService executorService;

	/**
	 * Lock to synchronize liveness tests with starting of new machines, to
	 * prevent booting machines from being included in runtime liveness tests.
	 */
	private final Lock lock = new ReentrantLock();
	/** Delay (in seconds) between two consecutive runtime liveness test runs. */
	private int livenessCheckPeriod;

	/**
	 * Constructs a new {@link LivenessTrackingScalingGroup}.
	 * 
	 * @param scalingGroup
	 *            The {@link ScalingGroup} for which liveness will be tracked.
	 * @param livenessTracker
	 *            {@link LivenessTracker} used to run periodical liveness tests.
	 * @param executorService
	 *            Executor service used to execute liveness test tasks.
	 * @param livenessCheckPeriod
	 *            Delay (in seconds) between two consecutive runtime liveness
	 *            test runs.
	 */
	public LivenessTrackingScalingGroup(ScalingGroup scalingGroup,
			LivenessTracker livenessTracker,
			ScheduledExecutorService executorService, int livenessCheckPeriod) {
		this.delegate = scalingGroup;
		this.livenessTracker = livenessTracker;
		this.executorService = executorService;
		this.livenessCheckPeriod = livenessCheckPeriod;
	}

	@Override
	public void configure(BaseCloudAdapterConfig configuration)
			throws ScalingGroupException {
		this.delegate.configure(configuration);

		// start periodical execution of runtime liveness checks
		this.executorService.scheduleWithFixedDelay(new RuntimeLivenessTask(),
				this.livenessCheckPeriod, this.livenessCheckPeriod,
				TimeUnit.SECONDS);
	}

	@Override
	public List<Machine> listMachines() throws ScalingGroupException {
		// Decorates each machine returned by the wrapped ScalingGroup with
		// liveness state (if any has been observed)
		List<Machine> livenessDecoratedMachines = Lists.newArrayList();
		List<Machine> machines = this.delegate.listMachines();
		for (Machine machine : machines) {
			if (Machine.isActive().apply(machine)) {
				// liveness is only relevant for active members
				LivenessState liveness = this.livenessTracker
						.getLiveness(machine);
				machine.setLiveness(liveness);
			}
			livenessDecoratedMachines.add(machine);
		}
		return livenessDecoratedMachines;
	}

	@Override
	public List<Machine> startMachines(int count, ScaleUpConfig scaleUpConfig)
			throws StartMachinesException {

		// apply locking to prevent newly started machines (without liveness
		// state yet set to BOOTING) from being included in a concurrent runtime
		// liveness checks
		List<Machine> startedMachines = null;
		try {
			this.lock.lock();
			try {
				startedMachines = this.delegate.startMachines(count,
						scaleUpConfig);
			} catch (StartMachinesException e) {
				// start boot-time liveness checks for the machines that were
				// started (if any) before error occurred
				startBootLivenessChecks(e.getStartedMachines());
				throw e;
			}
			startBootLivenessChecks(startedMachines);
		} finally {
			this.lock.unlock();
		}

		// we don't await completion of the boot liveness check, but just return
		// the started machines
		return startedMachines;
	}

	/**
	 * Asynchronously starts a collection of boot-time liveness tests for a
	 * number of {@link Machine}s.
	 * <p/>
	 * When this method returns, all {@link Machine}s are registered as BOOTING
	 * with the liveness tracker and all boot-time liveness checks have started
	 * in separate threads of execution.
	 * 
	 * @param startedMachines
	 * @return
	 */
	private List<Future<LivenessTestResult>> startBootLivenessChecks(
			List<Machine> startedMachines) {
		// filter out any machines in REQUESTED state since no liveness
		// tests can be carried out for them (until they've been assigned an IP
		// address)
		Iterable<Machine> bootingMachines = filter(startedMachines,
				not(Machine.withState(MachineState.REQUESTED)));

		List<Future<LivenessTestResult>> bootLivenessChecks = Lists
				.newArrayList();
		for (Machine startedMachine : bootingMachines) {
			bootLivenessChecks.add(this.livenessTracker
					.checkBootLiveness(startedMachine));
		}
		return bootLivenessChecks;
	}

	@Override
	public void terminateMachine(String machineId) throws ScalingGroupException {
		this.delegate.terminateMachine(machineId);
	}

	@Override
	public String getScalingGroupName() {
		return this.delegate.getScalingGroupName();
	}

	/**
	 * Runs runtime liveness checks for all active machines that aren't in the
	 * process of being booted.
	 */
	void runLivenessChecks() {
		LOG.info("running runtime liveness test for scaling group '{}'",
				getScalingGroupName());
		Iterable<Machine> bootedMachines = null;

		// apply locking to prevent just launched machines (not yet assigned
		// BOOTING state) from being included in runtime liveness tests
		Lock lock = LivenessTrackingScalingGroup.this.lock;
		try {
			lock.lock();
			bootedMachines = getBootedMachines();
		} finally {
			lock.unlock();
		}

		// spawn tasks
		List<Future<LivenessTestResult>> tasks = Lists.newArrayList();
		for (Machine machine : bootedMachines) {
			Future<LivenessTestResult> task = LivenessTrackingScalingGroup.this.livenessTracker
					.checkRuntimeLiveness(machine);
			tasks.add(task);
		}

		// collect results
		for (Future<LivenessTestResult> task : tasks) {
			try {
				LivenessTestResult result = task.get();
				LOG.debug("liveness test result: {}", result);
			} catch (Exception e) {
				LOG.warn("a runtime liveness test failed: {}", e.getMessage());
			}
		}
	}

	private Iterable<Machine> getBootedMachines() {
		try {
			List<Machine> scalingGroupMachines = listMachines();
			// exclude inactive machines from runtime liveness checking
			Iterable<Machine> activeMachines = Iterables.filter(
					scalingGroupMachines, Machine.isActive());
			// exclude any booting machines from runtime liveness checking
			Iterable<Machine> bootedMachines = filter(activeMachines,
					not(withLivenessState(LivenessState.BOOTING)));
			return bootedMachines;
		} catch (ScalingGroupException e) {
			String message = format(
					"failed to retrieve liveness check failed for scaling group "
							+ "\"%s\": %s", getScalingGroupName(), e.getMessage());
			LOG.error(message, e);
		}
		return Collections.emptyList();
	}

	/**
	 * A {@link Runnable} task that is scheduled for periodical execution to run
	 * liveness checks for the machines in the scaling group
	 * 
	 * 
	 * 
	 */
	private class RuntimeLivenessTask implements Runnable {
		@Override
		public void run() {
			runLivenessChecks();
		}

	}
}
