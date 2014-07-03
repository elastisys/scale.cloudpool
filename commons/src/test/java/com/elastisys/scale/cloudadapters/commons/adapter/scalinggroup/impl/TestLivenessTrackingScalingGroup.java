package com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.impl;

import static com.elastisys.scale.cloudadapters.commons.adapter.BaseAdapterTestUtils.machines;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.types.LivenessState;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseAdapterTestUtils;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.AlertSettings;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.LivenessConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleDownConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScalingGroupConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.LivenessTestResult;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.LivenessTracker;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroup;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.StartMachinesException;
import com.elastisys.scale.cloudadapters.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.ssh.SshCommandResult;

/**
 * Exercises the {@link LivenessTrackingScalingGroup}.
 * 
 * 
 * 
 */
public class TestLivenessTrackingScalingGroup {

	private static final int LIVENESS_CHECK_PERIOD = 60;

	private final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

	/**
	 * The mocked {@link ScalingGroup} being decorated by the
	 * {@link LivenessTrackingScalingGroup} under test.
	 */
	private final ScalingGroup scalingGroupMock = mock(ScalingGroup.class);
	/**
	 * The mocked {@link LivenessTracker} used by the
	 * {@link LivenessTrackingScalingGroup} under test.
	 */
	private final LivenessTracker livenessTrackerMock = mock(LivenessTracker.class);

	/** Object under test. */
	private LivenessTrackingScalingGroup livenessTrackingScalingGroup;

	@Before
	public void onSetup() {
		this.livenessTrackingScalingGroup = new LivenessTrackingScalingGroup(
				this.scalingGroupMock, this.livenessTrackerMock, this.executor,
				LIVENESS_CHECK_PERIOD);
	}

	/**
	 * Verify that the {@link LivenessTrackingScalingGroup} adds
	 * {@link LivenessState} to every {@link Machine} returned by the wrapped
	 * {@link ScalingGroup}.
	 * 
	 * @throws ScalingGroupException
	 */
	@Test
	public void verifyLivenessStateDecoration() throws ScalingGroupException {
		// machines in the scaling group (without liveness state)
		Machine machine1 = BaseAdapterTestUtils.machine("i-1",
				MachineState.PENDING);
		Machine machine2 = BaseAdapterTestUtils.machine("i-2",
				MachineState.RUNNING);
		Machine machine3 = BaseAdapterTestUtils.machine("i-3",
				MachineState.RUNNING);
		Machine inactive = BaseAdapterTestUtils.machine("i-4",
				MachineState.TERMINATED);

		// prepare mocks:
		// set up liveness state for different machines
		when(this.livenessTrackerMock.getLiveness(machine1)).thenReturn(
				LivenessState.BOOTING);
		when(this.livenessTrackerMock.getLiveness(machine2)).thenReturn(
				LivenessState.LIVE);
		when(this.livenessTrackerMock.getLiveness(machine3)).thenReturn(
				LivenessState.UNKNOWN);
		// last known liveness state of terminated machine
		when(this.livenessTrackerMock.getLiveness(inactive)).thenReturn(
				LivenessState.LIVE);
		// set up wrapped scaling group
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(machine1, machine2, machine3, inactive));

		// make call and verify that machines were properly decorated with
		// liveness state
		List<Machine> decoratedMachines = this.livenessTrackingScalingGroup
				.listMachines();
		assertThat(decoratedMachines.size(), is(4));
		assertThat(decoratedMachines.get(0).getId(), is("i-1"));
		assertThat(decoratedMachines.get(0).getLiveness(),
				is(LivenessState.BOOTING));
		assertThat(decoratedMachines.get(1).getId(), is("i-2"));
		assertThat(decoratedMachines.get(1).getLiveness(),
				is(LivenessState.LIVE));
		assertThat(decoratedMachines.get(2).getId(), is("i-3"));
		assertThat(decoratedMachines.get(2).getLiveness(),
				is(LivenessState.UNKNOWN));
		// no liveness state is set unless machine is in an active state
		assertThat(decoratedMachines.get(3).getId(), is("i-4"));
		assertThat(decoratedMachines.get(3).getLiveness(),
				is(LivenessState.UNKNOWN));

	}

	@Test
	public void verifyThatBootLivenessChecksAreInitiatedOnStartMachines()
			throws ScalingGroupException {
		Machine started1 = BaseAdapterTestUtils.machine("i-1",
				MachineState.PENDING);
		Machine started2 = BaseAdapterTestUtils.machine("i-2",
				MachineState.PENDING);

		int desiredMachines = 2;
		ScaleUpConfig scaleUpConfig = new ScaleUpConfig("size", "image",
				"keyPair", Arrays.asList("web"),
				Arrays.asList("apt-get update"));
		when(
				this.scalingGroupMock.startMachines(desiredMachines,
						scaleUpConfig))
				.thenReturn(machines(started1, started2));

		List<Machine> decoratedMachines = this.livenessTrackingScalingGroup
				.startMachines(desiredMachines, scaleUpConfig);
		assertThat(decoratedMachines.size(), is(desiredMachines));

		// verify that boot liveness checks were initiated for each started
		// machine
		verify(this.livenessTrackerMock).checkBootLiveness(started1);
		verify(this.livenessTrackerMock).checkBootLiveness(started2);
	}

	/**
	 * Verify that boot-time liveness checks were started for all machines that
	 * could be started for the case when the startMachines invocation only
	 * succeeded partially (not all requested machines could be provisioned).
	 */
	@Test
	public void verifyBehaviorWhenDelegateStartMachinesOnlySucceededPartially()
			throws ScalingGroupException {
		Machine started1 = BaseAdapterTestUtils.machine("i-1",
				MachineState.PENDING);
		Machine started2 = BaseAdapterTestUtils.machine("i-2",
				MachineState.PENDING);

		// the scaling group will only succeed on starting two of the requested
		// machines before throwing an exception
		int desiredMachines = 3;
		ScaleUpConfig launchConfig = new ScaleUpConfig("size", "image",
				"keyPair", Arrays.asList("web"),
				Arrays.asList("apt-get update"));
		StartMachinesException startMachinesFault = new StartMachinesException(
				desiredMachines, machines(started1, started2), new Exception(
						"could not get 3 machines"));
		when(this.scalingGroupMock.startMachines(desiredMachines, launchConfig))
				.thenThrow(startMachinesFault);

		try {
			this.livenessTrackingScalingGroup.startMachines(desiredMachines,
					launchConfig);
			fail("scaling group expected to re-throw error");
		} catch (StartMachinesException e) {
			assertThat(e, is(startMachinesFault));
		}
		// verify that boot liveness checks were initiated for each started
		// machine
		verify(this.livenessTrackerMock).checkBootLiveness(started1);
		verify(this.livenessTrackerMock).checkBootLiveness(started2);
	}

	/**
	 * Verify that inactive and booting machines in the scaling group are
	 * excluded from runtime liveness checks.
	 */
	@Test
	public void testThatBootingMachinesAreExcludedFromRuntimeLivenessCheck()
			throws ScalingGroupException {
		// machines in the scaling group (without liveness state)
		Machine booting = BaseAdapterTestUtils.machine("i-1",
				MachineState.PENDING);
		Machine active1 = BaseAdapterTestUtils.machine("i-2",
				MachineState.RUNNING);
		Machine active2 = BaseAdapterTestUtils.machine("i-3",
				MachineState.RUNNING);
		Machine terminated = BaseAdapterTestUtils.machine("i-4",
				MachineState.TERMINATED);

		// prepare mocks:
		// set up liveness state for different machines
		when(this.livenessTrackerMock.getLiveness(booting)).thenReturn(
				LivenessState.BOOTING);
		when(this.livenessTrackerMock.getLiveness(active1)).thenReturn(
				LivenessState.LIVE);
		when(this.livenessTrackerMock.getLiveness(active2)).thenReturn(
				LivenessState.UNKNOWN);
		// last known liveness state of terminated machine
		when(this.livenessTrackerMock.getLiveness(terminated)).thenReturn(
				LivenessState.LIVE);
		// set up wrapped scaling group
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active1, active2, terminated));

		// set up liveness test results for the active machines
		when(this.livenessTrackerMock.checkRuntimeLiveness(active1))
				.thenReturn(success(active1));
		when(this.livenessTrackerMock.checkRuntimeLiveness(active2))
				.thenReturn(success(active2));

		this.livenessTrackingScalingGroup.runLivenessChecks();
		// verify that only active machines not in the process of being booted
		// were runtime liveness checked
		verify(this.livenessTrackerMock).checkRuntimeLiveness(active1);
		verify(this.livenessTrackerMock).checkRuntimeLiveness(active2);
		// in particular: verify that neither booting nor terminated machine
		// were liveness-checked
		verify(this.livenessTrackerMock, never()).checkRuntimeLiveness(booting);
		verify(this.livenessTrackerMock, never()).checkRuntimeLiveness(
				terminated);
	}

	private FixedLivenessTestFuture success(Machine active1) {
		return new FixedLivenessTestFuture(new LivenessTestResult(active1,
				LivenessState.LIVE, new SshCommandResult(0, "OK!", "")));
	}

	/**
	 * Verify that the
	 * {@link ScalingGroup#configure(com.google.gson.JsonObject, String)} call
	 * is delegated through to the delegatee.
	 * 
	 * @throws ScalingGroupException
	 */
	@Test
	public void verifyDelegationOnConfigure() throws ScalingGroupException {
		this.livenessTrackingScalingGroup.configure(baseCloudadapterConfig());

		// delegate should have been called
		verify(this.scalingGroupMock).configure(baseCloudadapterConfig());

		// repeating execution of runtime liveness checks should be scheduled
		verify(this.executor).scheduleWithFixedDelay(any(Runnable.class),
				anyInt(), anyInt(), any(TimeUnit.class));
	}

	/**
	 * Verify that {@link ScalingGroup#listMachines()} calls are delegated
	 * through to the delegatee.
	 */
	@Test
	public void verifyDelegationOnListMachines() throws ScalingGroupException {
		configure(this.livenessTrackingScalingGroup);
		this.livenessTrackingScalingGroup.listMachines();

		// delegate should have been called
		verify(this.scalingGroupMock).listMachines();
	}

	/**
	 * Verify that {@link ScalingGroup#startMachines} calls are delegated
	 * through to the delegatee.
	 */
	@Test
	public void verifyDelegationOnStartMachines() throws ScalingGroupException {
		configure(this.livenessTrackingScalingGroup);
		ScaleUpConfig scaleUpConfig = new ScaleUpConfig("size", "image",
				"keyPair", Arrays.asList("web"),
				Arrays.asList("apt-get update"));
		this.livenessTrackingScalingGroup.startMachines(2, scaleUpConfig);

		// delegate should have been called
		verify(this.scalingGroupMock).startMachines(2, scaleUpConfig);
	}

	/**
	 * Verify that {@link ScalingGroup#terminateMachine} calls are delegated
	 * through to the delegatee.
	 */
	@Test
	public void verifyDelegationOnTerminateMachine()
			throws ScalingGroupException {
		configure(this.livenessTrackingScalingGroup);
		this.livenessTrackingScalingGroup.terminateMachine("i-1");

		// delegate should have been called
		verify(this.scalingGroupMock).terminateMachine("i-1");
	}

	/**
	 * Verify that {@link ScalingGroup#getScalingGroupName()} calls are delegated
	 * through to the delegatee.
	 */
	@Test
	public void verifyDelegationOnGetScalingGroup()
			throws ScalingGroupException {
		configure(this.livenessTrackingScalingGroup);
		this.livenessTrackingScalingGroup.getScalingGroupName();

		// delegate should have been called
		verify(this.scalingGroupMock).getScalingGroupName();
	}

	private void configure(ScalingGroup scalingGroup)
			throws ScalingGroupException {
		scalingGroup.configure(baseCloudadapterConfig());
	}

	/**
	 * Creates a sample {@link BaseCloudAdapterConfig}.
	 * 
	 * @return
	 */
	private BaseCloudAdapterConfig baseCloudadapterConfig() {
		ScalingGroupConfig scalingGroupConfig = new BaseCloudAdapterConfig.ScalingGroupConfig(
				"MyScalingGroup",
				JsonUtils.parseJsonString("{\"user\": \"johndoe\"}"));
		ScaleUpConfig scaleUpConfig = new ScaleUpConfig("size", "image",
				"keyPair", Arrays.asList("web"),
				Arrays.asList("apt-get install apache2"));
		ScaleDownConfig scaleDownConfig = new ScaleDownConfig(
				VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, 300);

		LivenessConfig liveness = null;
		AlertSettings alerts = null;
		return new BaseCloudAdapterConfig(scalingGroupConfig, scaleUpConfig,
				scaleDownConfig, liveness, alerts, 60);

	}
}
