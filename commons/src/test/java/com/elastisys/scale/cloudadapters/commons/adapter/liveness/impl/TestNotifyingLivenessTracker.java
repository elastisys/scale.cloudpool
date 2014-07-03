package com.elastisys.scale.cloudadapters.commons.adapter.liveness.impl;

import static com.elastisys.scale.cloudadapters.commons.adapter.BaseAdapterTestUtils.machine;
import static com.elastisys.scale.cloudadapters.commons.adapter.BaseAdapterTestUtils.validConfig2;
import static com.elastisys.scale.cloudadapters.commons.adapter.BaseAdapterTestUtils.validLivenessConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.LivenessState;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.LivenessConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.LivenessTestResult;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.LivenessTracker;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.SshLivenessTestFactory;
import com.elastisys.scale.commons.net.ssh.SshCommandResult;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Exercises the {@link NotifyingLivenessTracker}.
 * 
 * 
 * 
 */
public class TestNotifyingLivenessTracker {
	static Logger LOG = LoggerFactory
			.getLogger(TestNotifyingLivenessTracker.class);

	private final SshLivenessTestFactory livenessTestFactoryMock = mock(SshLivenessTestFactory.class);

	/**
	 * {@link EventBus} mock object used to capture events generated in the
	 * tests by the {@link NotifyingLivenessTracker}.
	 */
	private final EventBus eventBusMock = mock(EventBus.class);

	private final ExecutorService executorService = MoreExecutors
			.sameThreadExecutor();

	/** Object under test. */
	private NotifyingLivenessTracker livenessTracker;

	@Before
	public void onSetup() {
		FrozenTime.setFixed(UtcTime.parse("2014-04-16T12:00:00.000Z"));

		this.livenessTracker = new NotifyingLivenessTracker(
				this.livenessTestFactoryMock, this.eventBusMock,
				this.executorService);
	}

	@Test
	public void configure() throws CloudAdapterException {
		LivenessConfig livenessConfig = validLivenessConfig();
		this.livenessTracker.configure(livenessConfig);
		assertThat(this.livenessTracker.config(), is(validLivenessConfig()));
	}

	@Test
	public void reconfigure() throws CloudAdapterException {
		LivenessConfig oldConfig = validLivenessConfig();
		this.livenessTracker.configure(oldConfig);
		assertThat(this.livenessTracker.config(), is(oldConfig));

		LivenessConfig newConfig = validConfig2();
		this.livenessTracker.configure(newConfig);
		assertThat(this.livenessTracker.config(), is(newConfig));

	}

	@Test(expected = CloudAdapterException.class)
	public void configureWithInvalidConfig() throws CloudAdapterException {
		this.livenessTracker.configure(invalidSshKeyConfig());

	}

	@Test(expected = IllegalStateException.class)
	public void getLivenessBeforeBeingConfigured() {
		this.livenessTracker.getLiveness(machine("i-1", MachineState.RUNNING));
	}

	@Test(expected = IllegalStateException.class)
	public void checkBootLivenessBeforeBeingConfigured() {
		this.livenessTracker.checkBootLiveness(machine("i-1",
				MachineState.RUNNING));
	}

	@Test(expected = IllegalStateException.class)
	public void checkRuntimeLivenessBeforeBeingConfigured() {
		this.livenessTracker.checkRuntimeLiveness(machine("i-1",
				MachineState.RUNNING));
	}

	/**
	 * Verifies the behavior when running a boot-time liveness check that fails
	 * with an {@link Exception}.
	 */
	@Test
	public void runErroneousBootTimeLivenessCheck() throws Exception {
		// set up a liveness tracker with a factory that produces liveness test
		// that will fail with an error
		SocketException livenessTestError = new SocketException(
				"connection refused");
		SshLivenessTestFactory livenessTestFactory = new ErroneousSshLivenessTestFactory(
				livenessTestError);
		this.livenessTracker = new NotifyingLivenessTracker(
				livenessTestFactory, this.eventBusMock, this.executorService);
		this.livenessTracker.configure(validLivenessConfig());

		// start boot-time liveness test
		Machine machine = machine("i-1", MachineState.PENDING);
		Future<LivenessTestResult> promise = this.livenessTracker
				.checkBootLiveness(machine);
		// tracker should alert about machine state change: null -> BOOTING
		verify(this.eventBusMock).post(
				new LivenessStateChange(machine, null, LivenessState.BOOTING)
						.toAlert());

		// result should be UNHEALTHY
		LivenessTestResult livenessTestResult = promise.get();
		assertThat(livenessTestResult, is(new LivenessTestResult(machine,
				LivenessState.UNHEALTHY, livenessTestError)));
		// tracker should alert about machine state change: BOOTING -> UNHEALTHY
		verify(this.eventBusMock).post(
				new LivenessStateChange(machine, LivenessState.BOOTING,
						LivenessState.UNHEALTHY).toAlert().withTag(
						"commandError", livenessTestError.getMessage()));

		// liveness tracker should have marked machine unhealthy after erroneous
		// test
		assertEquals(this.livenessTracker.getLiveness(machine),
				LivenessState.UNHEALTHY);
	}

	/**
	 * Verifies the behavior when running a run-time liveness check that fails
	 * with an {@link Exception}.
	 */
	@Test
	public void runErroneousRunTimeLivenessCheck() throws Exception {
		// set up a liveness tracker with a factory that produces liveness test
		// that will fail with an error
		SocketException livenessTestError = new SocketException(
				"connection refused");
		SshLivenessTestFactory livenessTestFactory = new ErroneousSshLivenessTestFactory(
				livenessTestError);
		this.livenessTracker = new NotifyingLivenessTracker(
				livenessTestFactory, this.eventBusMock, this.executorService);
		this.livenessTracker.configure(validLivenessConfig());

		// start boot-time liveness test
		Machine machine = machine("i-1", MachineState.PENDING);
		Future<LivenessTestResult> promise = this.livenessTracker
				.checkRuntimeLiveness(machine);

		// result should be UNHEALTHY
		LivenessTestResult livenessTestResult = promise.get();
		assertThat(livenessTestResult, is(new LivenessTestResult(machine,
				LivenessState.UNHEALTHY, livenessTestError)));
		// tracker should alert about machine state change: UNKNOWN -> UNHEALTHY
		verify(this.eventBusMock).post(
				new LivenessStateChange(machine, LivenessState.UNKNOWN,
						LivenessState.UNHEALTHY).toAlert().withTag(
						"commandError", livenessTestError.getMessage()));

		// liveness tracker should have marked machine unhealthy after erroneous
		// test
		assertEquals(this.livenessTracker.getLiveness(machine),
				LivenessState.UNHEALTHY);
	}

	/**
	 * Verifies the behavior when running a boot-time liveness check that
	 * succeeds (with zero exit code).
	 */
	@Test
	public void runSuccessfulBootTimeLivenessCheck() throws Exception {
		// set up a liveness tracker with a factory that produces liveness test
		// that will succeed
		SshCommandResult commandResult = new SshCommandResult(0, "stdout", "");
		SshLivenessTestFactory livenessTestFactory = new FixedResultSshLivenessTestFactory(
				commandResult);
		this.livenessTracker = new NotifyingLivenessTracker(
				livenessTestFactory, this.eventBusMock, this.executorService);
		this.livenessTracker.configure(validLivenessConfig());

		// start boot-time liveness test
		Machine machine = machine("i-1", MachineState.PENDING);
		Future<LivenessTestResult> promise = this.livenessTracker
				.checkBootLiveness(machine);
		// tracker should alert about machine state change: null -> BOOTING
		verify(this.eventBusMock).post(
				new LivenessStateChange(machine, null, LivenessState.BOOTING)
						.toAlert());

		assertThat(promise.get(), is(new LivenessTestResult(machine,
				LivenessState.LIVE, commandResult)));
		// tracker should alert about machine state change: BOOTING -> LIVE
		verify(this.eventBusMock).post(
				new LivenessStateChange(machine, LivenessState.BOOTING,
						LivenessState.LIVE)
						.toAlert()
						.withTag("commandExitCode",
								"" + commandResult.getExitStatus())
						.withTag("commandStdout", commandResult.getStdout())
						.withTag("commandStderr", commandResult.getStderr()));

		// liveness tracker should have marked machine live after test
		assertEquals(this.livenessTracker.getLiveness(machine),
				LivenessState.LIVE);
	}

	/**
	 * Verifies the behavior when running a run-time liveness check that
	 * succeeds (with zero exit code).
	 */
	@Test
	public void runSuccessfulRunTimeLivenessCheck() throws Exception {
		// set up a liveness tracker with a factory that produces liveness test
		// that will succeed
		SshCommandResult commandResult = new SshCommandResult(0, "stdout", "");
		SshLivenessTestFactory livenessTestFactory = new FixedResultSshLivenessTestFactory(
				commandResult);
		this.livenessTracker = new NotifyingLivenessTracker(
				livenessTestFactory, this.eventBusMock, this.executorService);
		this.livenessTracker.configure(validLivenessConfig());

		// start run-time liveness test
		Machine machine = machine("i-1", MachineState.PENDING);
		Future<LivenessTestResult> promise = this.livenessTracker
				.checkRuntimeLiveness(machine);

		assertThat(promise.get(), is(new LivenessTestResult(machine,
				LivenessState.LIVE, commandResult)));
		// tracker should alert about machine state change: UNKNOWN -> LIVE
		verify(this.eventBusMock).post(
				new LivenessStateChange(machine, LivenessState.UNKNOWN,
						LivenessState.LIVE)
						.toAlert()
						.withTag("commandExitCode",
								"" + commandResult.getExitStatus())
						.withTag("commandStdout", commandResult.getStdout())
						.withTag("commandStderr", commandResult.getStderr()));

		// liveness tracker should have marked machine live after test
		assertEquals(this.livenessTracker.getLiveness(machine),
				LivenessState.LIVE);
	}

	/**
	 * Verifies the behavior when running a boot-time liveness check that fails
	 * (non-zero exit code).
	 */
	@Test
	public void runFailingBootTimeLivenessCheck() throws Exception {
		// set up a liveness tracker with a factory that produces liveness test
		// that will succeed
		SshCommandResult commandResult = new SshCommandResult(1, "", "stderr!");
		SshLivenessTestFactory livenessTestFactory = new FixedResultSshLivenessTestFactory(
				commandResult);
		this.livenessTracker = new NotifyingLivenessTracker(
				livenessTestFactory, this.eventBusMock, this.executorService);
		this.livenessTracker.configure(validLivenessConfig());

		// start boot-time liveness test
		Machine machine = machine("i-1", MachineState.PENDING);
		Future<LivenessTestResult> promise = this.livenessTracker
				.checkBootLiveness(machine);
		// tracker should alert about machine state change: null -> BOOTING
		verify(this.eventBusMock).post(
				new LivenessStateChange(machine, null, LivenessState.BOOTING)
						.toAlert());

		assertThat(promise.get(), is(new LivenessTestResult(machine,
				LivenessState.UNHEALTHY, commandResult)));
		// tracker should alert about machine state change: BOOTING -> UNHEALTHY
		verify(this.eventBusMock).post(
				new LivenessStateChange(machine, LivenessState.BOOTING,
						LivenessState.UNHEALTHY)
						.toAlert()
						.withTag("commandExitCode",
								"" + commandResult.getExitStatus())
						.withTag("commandStdout", commandResult.getStdout())
						.withTag("commandStderr", commandResult.getStderr()));

		// liveness tracker should have marked machine unhealthy after failing
		// test
		assertEquals(this.livenessTracker.getLiveness(machine),
				LivenessState.UNHEALTHY);
	}

	/**
	 * Verifies the behavior when running a run-time liveness check that fails
	 * (non-zero exit code).
	 */
	@Test
	public void runFailingRunTimeLivenessCheck() throws Exception {
		// set up a liveness tracker with a factory that produces liveness test
		// that will succeed
		SshCommandResult commandResult = new SshCommandResult(1, "", "stderr!");
		SshLivenessTestFactory livenessTestFactory = new FixedResultSshLivenessTestFactory(
				commandResult);
		this.livenessTracker = new NotifyingLivenessTracker(
				livenessTestFactory, this.eventBusMock, this.executorService);
		this.livenessTracker.configure(validLivenessConfig());

		// start boot-time liveness test
		Machine machine = machine("i-1", MachineState.PENDING);
		Future<LivenessTestResult> promise = this.livenessTracker
				.checkRuntimeLiveness(machine);

		assertThat(promise.get(), is(new LivenessTestResult(machine,
				LivenessState.UNHEALTHY, commandResult)));
		// tracker should alert about machine state change: UNKNOWN -> UNHEALTHY
		verify(this.eventBusMock).post(
				new LivenessStateChange(machine, LivenessState.UNKNOWN,
						LivenessState.UNHEALTHY)
						.toAlert()
						.withTag("commandExitCode",
								"" + commandResult.getExitStatus())
						.withTag("commandStdout", commandResult.getStdout())
						.withTag("commandStderr", commandResult.getStderr()));

		// liveness tracker should have marked machine unhealthy after failing
		// test
		assertEquals(this.livenessTracker.getLiveness(machine),
				LivenessState.UNHEALTHY);
	}

	/**
	 * Verifies that the latest observed {@link LivenessState} for a machine
	 * that (so far) is unknown to the {@link LivenessTracker} is
	 * {@link LivenessState#UNKNOWN}.
	 * 
	 * @throws CloudAdapterException
	 */
	@Test
	public void getLivenessForUnknownMachine() throws CloudAdapterException {
		this.livenessTracker.configure(validLivenessConfig());
		Machine unknownMachine = machine("i-1", MachineState.PENDING);
		assertThat(this.livenessTracker.getLiveness(unknownMachine),
				is(LivenessState.UNKNOWN));
	}

	private LivenessConfig invalidSshKeyConfig() {
		LivenessConfig livenessConfig = new LivenessConfig(
				22,
				"loginUser",
				"/my/non/existing/sshkey.pem",
				new BaseCloudAdapterConfig.BootTimeLivenessCheck(
						"service apache2 status | grep 'is running'", 20, 15),
				new BaseCloudAdapterConfig.RunTimeLivenessCheck(
						"service apache2 status | grep 'is running'", 60, 3, 10));
		return livenessConfig;
	}

}
