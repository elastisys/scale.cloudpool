package com.elastisys.scale.cloudadapters.commons.adapter;

import static com.elastisys.scale.cloudadapers.api.types.MachineState.PENDING;
import static com.elastisys.scale.cloudadapers.api.types.MachineState.RUNNING;
import static com.elastisys.scale.cloudadapers.api.types.MachineState.TERMINATED;
import static com.elastisys.scale.cloudadapters.commons.adapter.BaseAdapterTestUtils.machine;
import static com.elastisys.scale.cloudadapters.commons.adapter.BaseAdapterTestUtils.machines;
import static com.elastisys.scale.cloudadapters.commons.adapter.IsAlert.isAlert;
import static com.elastisys.scale.cloudadapters.commons.adapter.IsBootAlert.isBootAlert;
import static com.elastisys.scale.cloudadapters.commons.adapter.IsResizeAlert.isResizeAlert;
import static com.elastisys.scale.cloudadapters.commons.adapter.alerts.AlertTopics.POOL_FETCH;
import static com.elastisys.scale.cloudadapters.commons.adapter.alerts.AlertTopics.RESIZE;
import static com.elastisys.scale.cloudadapters.commons.scaledown.VictimSelectionPolicy.NEWEST_INSTANCE;
import static com.elastisys.scale.cloudadapters.commons.scaledown.VictimSelectionPolicy.OLDEST_INSTANCE;
import static com.elastisys.scale.commons.net.smtp.alerter.AlertSeverity.ERROR;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.AlertSettings;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.MailServerSettings;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleDownConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScalingGroupConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroup;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.StartMachinesException;
import com.elastisys.scale.cloudadapters.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.smtp.ClientAuthentication;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonObject;

/**
 * Verifies proper operation of the {@link BaseCloudAdapter}.
 *
 * 
 *
 */
public class TestBaseCloudAdapterOperation {

	/** Mocked {@link EventBus} to capture events sent by the cloud adapter. */
	private final EventBus eventBusMock = mock(EventBus.class);
	/** Mocked scaling group for the simulated cloud. */
	private final ScalingGroup scalingGroupMock = mock(ScalingGroup.class);
	/** The object under test. */
	private BaseCloudAdapter cloudAdapter;

	@Before
	public void onSetup() {
		FrozenTime.setFixed(UtcTime.parse("2014-04-17T12:00:00.000Z"));
		this.cloudAdapter = new BaseCloudAdapter(this.scalingGroupMock,
				this.eventBusMock);
	}

	/**
	 * Verify that the initial pool size is correctly determined on an empty
	 * scaling group.
	 */
	@Test
	public void testPoolSizeInitializationOnEmptyPool()
			throws CloudAdapterException {
		// set up mocked responses
		when(this.scalingGroupMock.listMachines()).thenReturn(machines());

		// run test
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// make sure that the initial pool size is correctly set
		assertThat(this.cloudAdapter.desiredSize(), is(0));
	}

	/**
	 * Verify that the initial pool size is correctly determined on a non-empty
	 * scaling group.
	 */
	@Test
	public void testPoolSizeInitializationOnNonEmptyPool()
			throws CloudAdapterException {
		Machine requested = machine("i-1", MachineState.REQUESTED);
		Machine pending = machine("i-2", MachineState.PENDING);
		Machine active = machine("i-3", MachineState.RUNNING);
		Machine rejected = machine("i-4", MachineState.REJECTED);
		Machine terminated = machine("i-4", MachineState.TERMINATED);

		// set up mocked responses
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(requested, pending, active, rejected, terminated));

		// run test
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// make sure that the initial pool size is correctly determined
		// (should only count machines in an active state)
		assertThat(this.cloudAdapter.desiredSize(), is(3));
	}

	/**
	 * Verifies the behavior of {@link CloudAdapter#getMachinePool()} on a
	 * successful request to retrieve the {@link ScalingGroup} members.
	 */
	@Test
	public void getMachinePool() throws CloudAdapterException {
		Machine pending = machine("i-1", MachineState.PENDING);
		Machine running = machine("i-2", MachineState.RUNNING);
		Machine requested = machine("i-3", MachineState.REQUESTED);
		Machine rejected = machine("i-4", MachineState.REJECTED);
		Machine terminated = machine("i-5", MachineState.TERMINATED);
		// set up mocked responses
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(pending, running, requested, rejected, terminated));

		// run test
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		MachinePool machinePool = this.cloudAdapter.getMachinePool();
		assertThat(machinePool.getTimestamp(), is(UtcTime.now()));
		assertThat(machinePool.getMachines().size(), is(5));
		assertThat(machinePool.getMachines().get(0), is(pending));
		assertThat(machinePool.getMachines().get(1), is(running));
		assertThat(machinePool.getMachines().get(2), is(requested));
		assertThat(machinePool.getMachines().get(3), is(rejected));
		assertThat(machinePool.getMachines().get(4), is(terminated));
	}

	/**
	 * Verifies the behavior of {@link CloudAdapter#getMachinePool()} on an
	 * unsuccessful request to retrieve the {@link ScalingGroup} members.
	 */
	@Test
	public void getMachinePoolOnError() throws CloudAdapterException {
		// set up mocked responses
		Throwable fault = new ScalingGroupException(
				"could not retrieve members");
		// first invocation (when cloudadapter initializes desiredSize): return
		// an empty list
		// second invocation (when getMachinePool is called): raise error
		when(this.scalingGroupMock.listMachines()).thenReturn(machines())
				.thenThrow(fault);

		// run test
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		try {
			this.cloudAdapter.getMachinePool();
			fail("getMachinePool expected to fail when listMachines fail");
		} catch (CloudAdapterException e) {
			// expected
			assertThat(e.getCause(), is(fault));
		}

		// verify event posted on event bus
		verify(this.eventBusMock).post(
				argThat(isAlert(POOL_FETCH.name(), ERROR)));
	}

	@Test
	public void singleMachineScaleUpOfMachinePool()
			throws CloudAdapterException {
		// set up initial pool
		Machine booting = machine("i-1", MachineState.PENDING);
		Machine active = machine("i-2", MachineState.RUNNING);
		Machine requested = machine("i-3", MachineState.REQUESTED);
		Machine terminated = machine("i-4", MachineState.TERMINATED);
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active, requested, terminated));
		// when asked to start a machine, it will succeed
		Machine newMachine = machine("i-5", MachineState.PENDING);
		when(this.scalingGroupMock.startMachines(1, scaleUpConfig()))
				.thenReturn(machines(newMachine));

		// run test that requests one additional machine to be started
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// effective size: 3 => ask for 4 machines
		assertThat(this.cloudAdapter.desiredSize(), is(3));
		this.cloudAdapter.resizeMachinePool(4);
		assertThat(this.cloudAdapter.desiredSize(), is(4));

		// verify that scaling group was asked to start one additional machine
		verify(this.scalingGroupMock).startMachines(1, scaleUpConfig());

		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isResizeAlert(3, 4)));
		verify(this.eventBusMock).post(argThat(isBootAlert(newMachine)));
	}

	@Test
	public void multiMachineScaleUpOfMachinePool() throws CloudAdapterException {
		// set up initial pool
		Machine booting = machine("i-1", MachineState.PENDING);
		Machine active1 = machine("i-2", MachineState.RUNNING);
		Machine active2 = machine("i-3", MachineState.RUNNING);
		Machine terminated = machine("i-4", MachineState.TERMINATED);
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active1, active2, terminated));
		// when asked to start two more machines, it will succeed
		Machine newMachine1 = machine("i-5", MachineState.PENDING);
		Machine newMachine2 = machine("i-6", MachineState.PENDING);
		when(this.scalingGroupMock.startMachines(2, scaleUpConfig()))
				.thenReturn(machines(newMachine1, newMachine2));

		// run test that requests two additional machines to be started
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// effective size: 3 => ask for 5 machines
		assertThat(this.cloudAdapter.desiredSize(), is(3));
		this.cloudAdapter.resizeMachinePool(5);
		assertThat(this.cloudAdapter.desiredSize(), is(5));

		// verify that scaling group was asked to start two additional machines
		verify(this.scalingGroupMock).startMachines(2, scaleUpConfig());

		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isResizeAlert(3, 5)));
		verify(this.eventBusMock).post(argThat(isBootAlert(newMachine1)));
		verify(this.eventBusMock).post(argThat(isBootAlert(newMachine2)));
	}

	/**
	 * Single machine scale-up when the {@link ScalingGroup} only requests an
	 * instance that isn't immediately satisfied by the underlying
	 * infrastructure. This could, for example, be the case for a spot instance
	 * {@link ScalingGroup}. Such a machine will have a state of
	 * {@link MachineState#REQUESTED}, and no boot-time test should be performed
	 * in that case (since there is no IP address to connect to).
	 */
	@Test
	public void scaleUpWhenMachineOnlyGetsRequested() throws Exception {
		// set up initial pool
		Machine booting = machine("i-1", MachineState.PENDING);
		Machine active1 = machine("i-2", MachineState.RUNNING);
		Machine active2 = machine("i-3", MachineState.RUNNING);
		Machine terminated = machine("i-4", MachineState.TERMINATED);
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active1, active2, terminated));
		// when asked to start two more machines, scaling group will succeed but
		// return a machine in REQUESTED state
		Machine requestedMachine = machine("sir-5", MachineState.REQUESTED);
		when(this.scalingGroupMock.startMachines(1, scaleUpConfig()))
				.thenReturn(machines(requestedMachine));

		// run test that requests two additional machines to be started
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// effective size: 3 => ask for 4 machines
		assertThat(this.cloudAdapter.desiredSize(), is(3));
		this.cloudAdapter.resizeMachinePool(4);
		assertThat(this.cloudAdapter.desiredSize(), is(4));

		// verify that scaling group was asked to start two additional machines
		verify(this.scalingGroupMock).startMachines(1, scaleUpConfig());

		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isResizeAlert(3, 4)));
		// verify that *no* boot alert was sent for machine in REQUESTED state
		verify(this.eventBusMock, never()).post(
				argThat(isBootAlert(requestedMachine)));
	}

	/**
	 * Verify cloud adapter behavior when {@link ScalingGroup#startMachines}
	 * fails completely without starting any new machine(s).
	 */
	@Test
	public void completelyFailedScaleUpOfMachinePool()
			throws CloudAdapterException {
		// set up initial pool
		Machine booting = machine("i-1", MachineState.PENDING);
		Machine active1 = machine("i-2", MachineState.RUNNING);
		Machine active2 = machine("i-3", MachineState.RUNNING);
		Machine terminated = machine("i-4", MachineState.TERMINATED);
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active1, active2, terminated));
		// when asked to start a machine, an error will be raised
		Throwable fault = new StartMachinesException(2, machines(),
				new Exception("failed to add machines"));
		when(this.scalingGroupMock.startMachines(2, scaleUpConfig()))
				.thenThrow(fault);

		// run test that requests two additional machines to be started
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// effective size: 3 => ask for 5 machines
		assertThat(this.cloudAdapter.desiredSize(), is(3));
		try {
			this.cloudAdapter.resizeMachinePool(5);
			fail("cloud adapter expected to fail when startMachines fail");
		} catch (CloudAdapterException e) {
			// expected
			assertThat(e.getCause(), is(fault));
		}
		assertThat(this.cloudAdapter.desiredSize(), is(5));

		// verify that scaling group was asked to start two additional machines
		verify(this.scalingGroupMock).startMachines(2, scaleUpConfig());

		// verify that an error event was posted on event bus
		verify(this.eventBusMock).post(argThat(isAlert(RESIZE.name(), ERROR)));
	}

	/**
	 * Verify cloud adapter behavior when {@link ScalingGroup#startMachines}
	 * fails part-way in to the operation, after it has started a subset of the
	 * requested machines.
	 *
	 * @throws CloudAdapterException
	 */
	@Test
	public void partiallyFailedScaleUpOfMachinePool()
			throws CloudAdapterException {
		// set up initial pool
		Machine booting = machine("i-1", MachineState.PENDING);
		Machine active1 = machine("i-2", MachineState.RUNNING);
		Machine active2 = machine("i-3", MachineState.RUNNING);
		Machine terminated = machine("i-4", MachineState.TERMINATED);
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active1, active2, terminated));
		// when asked to start two machines, only one is started before an error
		// occurs
		Machine newMachine = machine("i-5", MachineState.PENDING);
		Throwable partialFault = new StartMachinesException(2,
				machines(newMachine), new Exception(
						"failed to start second machine"));
		when(this.scalingGroupMock.startMachines(2, scaleUpConfig()))
				.thenThrow(partialFault);

		// run test that requests two additional machines to be started
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// effective size: 3 => ask for 5 machines
		assertThat(this.cloudAdapter.desiredSize(), is(3));
		try {
			this.cloudAdapter.resizeMachinePool(5);
			fail("cloud adapter expected to fail when startMachines fail");
		} catch (CloudAdapterException e) {
			// expected
			assertThat(e.getCause(), is(partialFault));
		}
		assertThat(this.cloudAdapter.desiredSize(), is(5));

		// verify that scaling group was asked to start two additional machines
		verify(this.scalingGroupMock).startMachines(2, scaleUpConfig());

		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isResizeAlert(3, 4)));
		verify(this.eventBusMock).post(argThat(isBootAlert(newMachine)));
		// verify that an error event was posted on event bus
		verify(this.eventBusMock).post(argThat(isAlert(RESIZE.name(), ERROR)));
	}

	/**
	 * Verify cloud adapter behavior when scaling down machine pool by a single
	 * machine instance.
	 */
	@Test
	public void singleMachineScaleDownOfMachinePool()
			throws CloudAdapterException {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active1, active2, terminated));

		// run test that requests one machine to be terminated
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// effective size: 3 => ask for 2 machines
		assertThat(this.cloudAdapter.desiredSize(), is(3));
		this.cloudAdapter.resizeMachinePool(2);

		// verify that scaling group was asked to terminate the oldest active
		// machine
		verify(this.scalingGroupMock).terminateMachine("i-3");
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isResizeAlert(3, 2)));

		assertThat(this.cloudAdapter.desiredSize(), is(2));
	}

	/**
	 * Verify that cloud adapter enforces the chosen
	 * {@link VictimSelectionPolicy}.
	 */
	@Test
	public void vicitmSelectionPolicyEnforcementOnscaleDown()
			throws CloudAdapterException {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active1, active2, terminated));

		// run test that requests one machine to be terminated
		this.cloudAdapter.configure(adapterConfig(NEWEST_INSTANCE, 0));
		// effective size: 3 => ask for 2 machines
		assertThat(this.cloudAdapter.desiredSize(), is(3));
		this.cloudAdapter.resizeMachinePool(2);

		// verify that scaling group was asked to terminate the _newest_ active
		// machine
		verify(this.scalingGroupMock).terminateMachine("i-1");
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isResizeAlert(3, 2)));

		assertThat(this.cloudAdapter.desiredSize(), is(2));
	}

	/**
	 * Verify cloud adapter behvaior when scaling down the machine pool with
	 * several machines.
	 */
	@Test
	public void multiMachineScaleDownOfMachinePool()
			throws CloudAdapterException {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active1, active2, terminated));

		// run test that requests two machines to be terminated
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// effective size: 3 => ask for 1 machines
		assertThat(this.cloudAdapter.desiredSize(), is(3));
		this.cloudAdapter.resizeMachinePool(1);

		// verify that scaling group was asked to terminate the two oldest
		// active machines
		verify(this.scalingGroupMock).terminateMachine("i-3");
		verify(this.scalingGroupMock).terminateMachine("i-2");
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isResizeAlert(3, 1)));

		assertThat(this.cloudAdapter.desiredSize(), is(1));
	}

	/**
	 * Verify cloud adapter behavior when scaling down the machine pool fails.
	 */
	@Test
	public void failedSingleMachineScaleDownOfMachinePool()
			throws CloudAdapterException {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active1, active2, terminated));
		// when asked to terminate a machine, the scaling group will fail
		Exception fault = new ScalingGroupException("terminate failed");
		doThrow(fault).when(this.scalingGroupMock)
				.terminateMachine(anyString());

		// run test that requests one machine to be terminated
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// effective size: 3 => ask for 2 machines
		assertThat(this.cloudAdapter.desiredSize(), is(3));
		this.cloudAdapter.resizeMachinePool(2);

		// verify that scaling group was asked to terminate the oldest active
		// machine
		verify(this.scalingGroupMock).terminateMachine("i-3");
		// verify error events posted on event bus
		verify(this.eventBusMock).post(argThat(isAlert(RESIZE.name(), ERROR)));

		assertThat(this.cloudAdapter.desiredSize(), is(2));
	}

	/**
	 * Verify cloud adapter behavior when scaling down the machine pool with
	 * several instances fails.
	 */
	@Test
	public void failedMultiMachineScaleDownOfMachinePool()
			throws CloudAdapterException {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active1, active2, terminated));
		// when asked to terminate a machine, the scaling group will fail
		Exception fault = new ScalingGroupException("terminate failed");
		doThrow(fault).when(this.scalingGroupMock)
				.terminateMachine(anyString());

		// run test that requests two machines to be terminated
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// effective size: 3 => ask for 1 machines
		assertThat(this.cloudAdapter.desiredSize(), is(3));
		this.cloudAdapter.resizeMachinePool(1);

		// verify that scaling group was asked to terminate the two oldest
		// active machines
		verify(this.scalingGroupMock).terminateMachine("i-3");
		verify(this.scalingGroupMock).terminateMachine("i-2");
		// verify error events posted on event bus
		verify(this.eventBusMock, atLeast(2)).post(
				argThat(isAlert(RESIZE.name(), ERROR)));

		assertThat(this.cloudAdapter.desiredSize(), is(1));
	}

	/**
	 * Verify cloud adapter behavior when scaling down the machine pool fails
	 * for some instances but not others.
	 */
	@Test
	public void partiallyFailedScaleDownOfMachinePool()
			throws CloudAdapterException {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active1, active2, terminated));
		// when asked to terminate i-3, the scaling group will fail
		Exception fault = new ScalingGroupException("terminate failed");
		doThrow(fault).when(this.scalingGroupMock).terminateMachine("i-3");

		// run test that requests two machines to be terminated
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// effective size: 3 => ask for 1 machines
		assertThat(this.cloudAdapter.desiredSize(), is(3));
		this.cloudAdapter.resizeMachinePool(1);

		// verify that scaling group was asked to terminate the two oldest
		// active machines
		verify(this.scalingGroupMock).terminateMachine("i-3");
		verify(this.scalingGroupMock).terminateMachine("i-2");
		// verify one error event due to failure to terminate i-3
		verify(this.eventBusMock, atMost(1)).post(
				argThat(isAlert(RESIZE.name(), ERROR)));
		// verify that termination of i-2 succeeded
		verify(this.eventBusMock).post(argThat(isResizeAlert(3, 2)));

		assertThat(this.cloudAdapter.desiredSize(), is(1));
	}

	/**
	 * Test correct behavior when cloud adapter is configured to do late release
	 * of machine instances, such that the termination is delayed to be closer
	 * to the end of the billing hour.
	 */
	@Test
	public void lateTermination() throws CloudAdapterException {
		DateTime now = UtcTime.parse("2014-04-22T12:00:00.000Z");
		DateTime launchTime = UtcTime.parse("2014-04-22T11:55:00.000Z");
		FrozenTime.setFixed(now);

		// set up initial pool
		Machine active1 = machine("i-1", RUNNING, launchTime);
		when(this.scalingGroupMock.listMachines())
				.thenReturn(machines(active1));

		// instances are to be released five minutes before next instance hour
		int instanceHourMargin = 300;
		JsonObject config = adapterConfig(OLDEST_INSTANCE, instanceHourMargin);
		this.cloudAdapter.configure(config);
		// run test that requests one machine to be terminated
		// effective size: 1 => ask for 0 machines
		assertThat(this.cloudAdapter.desiredSize(), is(1));
		this.cloudAdapter.resizeMachinePool(0);

		// verify that scaling group was not asked (yet) to terminate and that
		// no resize event has been posted
		verify(this.scalingGroupMock, never()).terminateMachine("i-1");
		verifyZeroInteractions(this.eventBusMock);

		// forward time to just prior to when termination should be ordered
		FrozenTime.setFixed(UtcTime.parse("2014-04-22T12:49:59.000Z"));
		this.cloudAdapter.updateMachinePool();
		// ... termination still shouldn't have been ordered
		verify(this.scalingGroupMock, never()).terminateMachine("i-1");
		verifyZeroInteractions(this.eventBusMock);

		// forward time to when termination is due
		FrozenTime.setFixed(UtcTime.parse("2014-04-22T12:50:50.000Z"));
		this.cloudAdapter.updateMachinePool();
		// ... now termination should have been ordered
		verify(this.scalingGroupMock).terminateMachine("i-1");
		verify(this.eventBusMock).post(argThat(isResizeAlert(1, 0)));

		assertThat(this.cloudAdapter.desiredSize(), is(0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullScalingGroup() {
		new BaseCloudAdapter(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullEventBus() {
		new BaseCloudAdapter(this.scalingGroupMock, null);
	}

	/**
	 * Run a pool update iteration when pool size is {@code desiredSize} and
	 * make no scaling action is taken.
	 */
	@Test
	public void doPoolUpdateWhenPoolIsProperlySized() throws Exception {
		// set up initial pool of size 2
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active1, terminated));
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// desiredSize should have been determined to 2
		assertThat(this.cloudAdapter.desiredSize(), is(2));

		// run pool update (to 2) => no changes are expected
		this.cloudAdapter.updateMachinePool();
		// verify that no resize alerts were sent
		verifyNoMoreInteractions(this.eventBusMock);
	}

	/**
	 * Run a pool update iteration when pool size is smaller than
	 * {@code desiredSize} (for example, due to some external event causing a
	 * machine to go down) and make sure pool is scaled up.
	 */
	@Test
	public void doPoolUpdateWhenPoolIsTooSmall() throws Exception {
		// set up initial scaling group of size 3
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active1, active2, terminated));
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// desiredSize should have been determined to 3
		assertThat(this.cloudAdapter.desiredSize(), is(3));

		// an external event causes a machine in the pool to be terminated
		// => scaling group now of size 2
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active1, terminated));
		// when asked to start a machine, it will succeed
		Machine newMachine = machine("i-5", MachineState.PENDING);
		when(this.scalingGroupMock.startMachines(1, scaleUpConfig()))
				.thenReturn(machines(newMachine));

		// run pool update (to 3) => scale-out expected
		this.cloudAdapter.updateMachinePool();

		// verify that scaling group was asked to start one additional machine
		verify(this.scalingGroupMock).startMachines(1, scaleUpConfig());
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isResizeAlert(2, 3)));
		verify(this.eventBusMock).post(argThat(isBootAlert(newMachine)));
	}

	/**
	 * Run a pool update iteration when pool size is bigger than
	 * {@code desiredSize} (for example, due to some external event causing a
	 * machine to come up) and make sure pool is scaled down.
	 */
	@Test
	public void doPoolUpdateWhenPoolIsTooBig() throws Exception {
		// set up initial scaling group of size 2
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active2, terminated));
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// desiredSize should have been determined to 2
		assertThat(this.cloudAdapter.desiredSize(), is(2));

		// an external event causes a machine in the pool to be started
		// => scaling group now of size 3
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		when(this.scalingGroupMock.listMachines()).thenReturn(
				machines(booting, active1, active2, terminated));

		// run pool update (to 2) => scale-in expected
		this.cloudAdapter.updateMachinePool();

		// verify that scaling group was asked to terminate the oldest active
		// machine
		verify(this.scalingGroupMock).terminateMachine("i-3");
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isResizeAlert(3, 2)));
		assertThat(this.cloudAdapter.desiredSize(), is(2));
	}

	/**
	 * Run a pool update iteration when {@code desiredSize} hasn't yet been
	 * determined (or set) and make sure that it is determined.
	 */
	@Test
	public void doPoolUpdateWhenDesiredSizeIsUnset() throws Exception {
		// determining initial pool size on startup should fail
		when(this.scalingGroupMock.listMachines()).thenThrow(
				new ScalingGroupException("cloud provider API outage"));
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// desiredSize should be undetermined
		assertThat(this.cloudAdapter.desiredSize(), is(nullValue()));

		// make sure desiredSize is determined when pool update is run
		reset(this.scalingGroupMock);
		when(this.scalingGroupMock.listMachines()).thenReturn(machines());
		// run pool update
		this.cloudAdapter.updateMachinePool();

		// verify that desiredSize was determined
		assertThat(this.cloudAdapter.desiredSize(), is(0));
	}

	/**
	 * Run a pool update iteration when {@code desiredSize} hasn't yet been
	 * determined (or set) and the attempt to determine it fails. Make sure this
	 * doesn't cause the iteration to fail.
	 */
	@Test
	public void doPoolUpdateWhenDesiredSizeIsUnsetAndItCantBeDetermined()
			throws Exception {
		// determining initial pool size on startup should fail
		when(this.scalingGroupMock.listMachines()).thenThrow(
				new ScalingGroupException("cloud provider API outage"));
		this.cloudAdapter.configure(adapterConfig(OLDEST_INSTANCE, 0));
		// desiredSize should be undetermined
		assertThat(this.cloudAdapter.desiredSize(), is(nullValue()));

		// run pool update
		this.cloudAdapter.updateMachinePool();

		// verify that desiredSize is still undetermined
		assertThat(this.cloudAdapter.desiredSize(), is(nullValue()));
	}

	/**
	 * Creates a {@link BaseCloudAdapterConfig} with a given scale-down victim
	 * selection strategy and instance hour margin.
	 *
	 * @param victimSelectionPolicy
	 * @param instanceHourMargin
	 *            Instance hour margin (in seconds). Zero means immediate
	 *            termination.
	 * @return
	 */
	private JsonObject adapterConfig(
			VictimSelectionPolicy victimSelectionPolicy, int instanceHourMargin) {
		ScalingGroupConfig scalingGroupConfig = scalingGroupConfig();
		ScaleUpConfig scaleUpConfig = scaleUpConfig();
		ScaleDownConfig scaleDownConfig = new ScaleDownConfig(
				victimSelectionPolicy, instanceHourMargin);
		BaseCloudAdapterConfig adapterConfig = new BaseCloudAdapterConfig(
				scalingGroupConfig, scaleUpConfig, scaleDownConfig,
				BaseAdapterTestUtils.validLivenessConfig(), null, 120);

		return JsonUtils.toJson(adapterConfig).getAsJsonObject();
	}

	private ScalingGroupConfig scalingGroupConfig() {
		return new ScalingGroupConfig("MyScalingGroup",
				cloudCredentialsConfig());
	}

	private JsonObject cloudCredentialsConfig() {
		return JsonUtils.parseJsonString("{\"userName\": \"johndoe\", "
				+ "\"region\": \"us-east-1\"}");
	}

	private ScaleUpConfig scaleUpConfig() {
		ScaleUpConfig scaleUpConfig = new ScaleUpConfig("size", "image",
				"keyPair", Arrays.asList("web"),
				Arrays.asList("apt-get install apache2"));
		return scaleUpConfig;
	}

	private AlertSettings alertConfig() {
		return new AlertSettings("subject",
				Arrays.asList("recipient@dest.com"), "sender@source.com",
				"ERROR|FATAL", mailServer());
	}

	private MailServerSettings mailServer() {
		return new MailServerSettings("smtpHost", 587, smtpAuth(), true);
	}

	private ClientAuthentication smtpAuth() {
		return new ClientAuthentication("userName", "password");
	}

}
