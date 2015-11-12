package com.elastisys.scale.cloudpool.commons.basepool;

import static com.elastisys.scale.cloudpool.api.types.MachineState.PENDING;
import static com.elastisys.scale.cloudpool.api.types.MachineState.RUNNING;
import static com.elastisys.scale.cloudpool.api.types.MachineState.TERMINATED;
import static com.elastisys.scale.cloudpool.api.types.MachineState.TERMINATING;
import static com.elastisys.scale.cloudpool.api.types.ServiceState.OUT_OF_SERVICE;
import static com.elastisys.scale.cloudpool.api.types.ServiceState.UNKNOWN;
import static com.elastisys.scale.cloudpool.commons.basepool.BasePoolTestUtils.machine;
import static com.elastisys.scale.cloudpool.commons.basepool.BasePoolTestUtils.machines;
import static com.elastisys.scale.cloudpool.commons.basepool.IsAlert.isAlert;
import static com.elastisys.scale.cloudpool.commons.basepool.IsAttachAlert.isAttachAlert;
import static com.elastisys.scale.cloudpool.commons.basepool.IsDetachAlert.isDetachAlert;
import static com.elastisys.scale.cloudpool.commons.basepool.IsSetMembershipStatusAlert.isMembershipStatusAlert;
import static com.elastisys.scale.cloudpool.commons.basepool.IsSetServiceStateAlert.isSetServiceStateAlert;
import static com.elastisys.scale.cloudpool.commons.basepool.IsStartAlert.isStartAlert;
import static com.elastisys.scale.cloudpool.commons.basepool.IsTerminationAlert.isTerminationAlert;
import static com.elastisys.scale.cloudpool.commons.basepool.alerts.AlertTopics.RESIZE;
import static com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy.NEWEST_INSTANCE;
import static com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy.OLDEST_INSTANCE;
import static com.elastisys.scale.commons.net.alerter.AlertSeverity.ERROR;
import static com.elastisys.scale.commons.net.alerter.AlertSeverity.WARN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.NotConfiguredException;
import com.elastisys.scale.cloudpool.api.NotStartedException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.CloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.PoolFetchConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.RetriesConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleInConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.TimeInterval;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import com.google.gson.JsonObject;

/**
 * Verifies proper operation of the {@link BaseCloudPool}.
 */
public class TestBaseCloudPoolOperation {
	private static final Logger LOG = LoggerFactory
			.getLogger(TestBaseCloudPoolOperation.class);

	private static final File STATE_STORAGE_DIR = new File("target/state");
	private static final StateStorage STATE_STORAGE = StateStorage
			.builder(STATE_STORAGE_DIR).build();

	/** Mocked {@link EventBus} to capture events sent by the cloud pool. */
	private final EventBus eventBusMock = mock(EventBus.class);
	/** Mocked cloud driver for the simulated cloud. */
	private final CloudPoolDriver driverMock = mock(CloudPoolDriver.class);
	/** The object under test. */
	private BaseCloudPool cloudPool;

	@Before
	public void onSetup() throws IOException {
		FileUtils.deleteRecursively(STATE_STORAGE_DIR);
		FrozenTime.setFixed(UtcTime.parse("2014-04-17T12:00:00.000Z"));
		this.cloudPool = new BaseCloudPool(STATE_STORAGE, this.driverMock,
				this.eventBusMock);
	}

	/**
	 * Configuring a stopped {@link CloudPool} should not change its stopped
	 * state.
	 */
	@Test
	public void configureStoppedCloudPool() {
		assertThat(this.cloudPool.getStatus().isStarted(), is(false));
		Optional<JsonObject> absent = Optional.absent();
		assertThat(this.cloudPool.getConfiguration(), is(absent));

		JsonObject config = poolConfig(OLDEST_INSTANCE, 0);
		this.cloudPool.configure(config);
		assertThat(this.cloudPool.getStatus().isStarted(), is(false));
		assertThat(this.cloudPool.getConfiguration().get(), is(config));
	}

	/**
	 * Configuring a started {@link CloudPool} should re-configure and re-start
	 * the {@link CloudPool}.
	 */
	@Test
	public void reconfigureStartedCloudPool() {
		JsonObject config = poolConfig(OLDEST_INSTANCE, 0);
		this.cloudPool.configure(config);
		this.cloudPool.start();
		assertThat(this.cloudPool.getStatus().isStarted(), is(true));
		assertThat(this.cloudPool.getConfiguration().get(), is(config));

		JsonObject newConfig = poolConfig(NEWEST_INSTANCE, 1800);
		// configure should re-start the cloudpool
		this.cloudPool.configure(newConfig);
		assertThat(this.cloudPool.getStatus().isStarted(), is(true));
		assertThat(this.cloudPool.getConfiguration().get(), is(newConfig));
		assertThat(newConfig, is(not(config)));
	}

	@Test
	public void startCloudPool() {
		JsonObject config = poolConfig(OLDEST_INSTANCE, 0);
		this.cloudPool.configure(config);
		assertThat(this.cloudPool.getStatus().isConfigured(), is(true));
		assertThat(this.cloudPool.getStatus().isStarted(), is(false));

		this.cloudPool.start();
		assertThat(this.cloudPool.getStatus().isStarted(), is(true));

		// starting an already started pool is a no-op
		this.cloudPool.start();
		assertThat(this.cloudPool.getStatus().isStarted(), is(true));
	}

	/**
	 * Should not be possible to start a {@link CloudPool} before being
	 * configured.
	 */
	@Test(expected = NotConfiguredException.class)
	public void startBeforeConfigured() {
		this.cloudPool.start();
	}

	@Test
	public void stopCloudPool() {
		JsonObject config = poolConfig(OLDEST_INSTANCE, 0);
		this.cloudPool.configure(config);
		this.cloudPool.start();
		assertThat(this.cloudPool.getStatus().isStarted(), is(true));

		this.cloudPool.stop();
		assertThat(this.cloudPool.getStatus().isStarted(), is(false));

		// stopping an already stopped pool is a no-op
		this.cloudPool.stop();
		assertThat(this.cloudPool.getStatus().isStarted(), is(false));
	}

	/**
	 * Any saved machine pool cache should be restored on restart and should be
	 * relied upon if machine pool cannot be immediately refreshed.
	 */
	@Test
	public void recoverMachinePoolCache() throws IOException {
		// save a cached machine pool to disk that will be restored when
		// starting the cloudpool
		MachinePool cachedPool = new MachinePool(
				machines(machine("i-1"), machine("i-2")), FrozenTime.now());
		File cacheFile = STATE_STORAGE.getCachedMachinePoolFile();
		save(cachedPool, cacheFile);

		// the attempt to refresh the machine pool cache on start-up should fail
		when(this.driverMock.listMachines())
				.thenThrow(new RuntimeException("api outage"));

		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// make sure that the cached machine pool was recovered and is used
		// (since we could not refresh the cache)
		assertThat(this.cloudPool.getMachinePool(), is(cachedPool));
	}

	/**
	 * Verify that the initial pool size is correctly determined on an empty
	 * cloud pool.
	 */
	@Test
	public void testPoolSizeInitializationOnEmptyPool()
			throws CloudPoolException {
		// set up mocked responses
		when(this.driverMock.listMachines()).thenReturn(machines());

		// run test
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// make sure that the initial pool size is correctly set
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(0));
	}

	/**
	 * Verify that the initial pool size is correctly determined on a non-empty
	 * cloud pool.
	 */
	@Test
	public void testPoolSizeInitializationOnNonEmptyPool()
			throws CloudPoolException {
		Machine requested = machine("i-1", MachineState.REQUESTED);
		Machine pending = machine("i-2", MachineState.PENDING);
		Machine active = machine("i-3", MachineState.RUNNING);
		Machine rejected = machine("i-4", MachineState.REJECTED);
		Machine terminated = machine("i-4", MachineState.TERMINATED);

		// set up mocked responses
		when(this.driverMock.listMachines()).thenReturn(
				machines(requested, pending, active, rejected, terminated));

		// run test
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// make sure that the initial pool size is correctly determined
		// (should only count machines in an active state)
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(3));
	}

	/**
	 * Verifies the behavior of {@link CloudPool#getMachinePool()} on a
	 * successful request to retrieve the {@link CloudPoolDriver} members.
	 */
	@Test
	public void getMachinePool() throws CloudPoolException {
		Machine pending = machine("i-1", MachineState.PENDING);
		Machine running = machine("i-2", MachineState.RUNNING);
		Machine requested = machine("i-3", MachineState.REQUESTED);
		Machine rejected = machine("i-4", MachineState.REJECTED);
		Machine terminated = machine("i-5", MachineState.TERMINATED);
		// set up mocked responses
		when(this.driverMock.listMachines()).thenReturn(
				machines(pending, running, requested, rejected, terminated));

		// run test
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		MachinePool machinePool = this.cloudPool.getMachinePool();
		assertThat(machinePool.getTimestamp(), is(UtcTime.now()));
		assertThat(machinePool.getMachines().size(), is(5));
		assertThat(machinePool.getMachines().get(0), is(pending));
		assertThat(machinePool.getMachines().get(1), is(running));
		assertThat(machinePool.getMachines().get(2), is(requested));
		assertThat(machinePool.getMachines().get(3), is(rejected));
		assertThat(machinePool.getMachines().get(4), is(terminated));
	}

	@Test
	public void singleMachineScaleUpOfMachinePool() throws CloudPoolException {
		// set up initial pool
		Machine booting = machine("i-1", MachineState.PENDING);
		Machine active = machine("i-2", MachineState.RUNNING);
		Machine requested = machine("i-3", MachineState.REQUESTED);
		Machine terminated = machine("i-4", MachineState.TERMINATED);
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active, requested, terminated));
		// when asked to start a machine, it will succeed
		Machine newMachine = machine("i-5", MachineState.PENDING);
		when(this.driverMock.startMachines(1, scaleOutConfig()))
				.thenReturn(machines(newMachine));

		// run test that requests one additional machine to be started
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
		// effective size: 3 => ask for 4 machines
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(3));
		this.cloudPool.setDesiredSize(4);
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(4));

		// force a pool resize run
		this.cloudPool.updateMachinePool();

		// verify that cloud driver was asked to start one additional machine
		verify(this.driverMock).startMachines(1, scaleOutConfig());

		// verify event posted on event bus
		verify(this.eventBusMock)
				.post(argThat(IsStartAlert.isStartAlert("i-5")));
	}

	@Test
	public void multiMachineScaleUpOfMachinePool() throws CloudPoolException {
		// set up initial pool
		Machine booting = machine("i-1", MachineState.PENDING);
		Machine active1 = machine("i-2", MachineState.RUNNING);
		Machine active2 = machine("i-3", MachineState.RUNNING);
		Machine terminated = machine("i-4", MachineState.TERMINATED);
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active1, active2, terminated));
		// when asked to start two more machines, it will succeed
		Machine newMachine1 = machine("i-5", MachineState.PENDING);
		Machine newMachine2 = machine("i-6", MachineState.PENDING);
		when(this.driverMock.startMachines(2, scaleOutConfig()))
				.thenReturn(machines(newMachine1, newMachine2));

		// run test that requests two additional machines to be started
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// effective size: 3 => ask for 5 machines
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(3));
		this.cloudPool.setDesiredSize(5);
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(5));

		// force a pool resize run
		this.cloudPool.updateMachinePool();

		// verify that cloud driver was asked to start two additional machines
		verify(this.driverMock).startMachines(2, scaleOutConfig());

		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isStartAlert("i-5", "i-6")));

	}

	/**
	 * Single machine scale-up when the {@link CloudPoolDriver} only requests an
	 * instance that isn't immediately satisfied by the underlying
	 * infrastructure. This could, for example, be the case for a spot instance
	 * {@link CloudPoolDriver}. Such a machine will have a state of
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
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active1, active2, terminated));
		// when asked to start two more machines, cloud pool will succeed but
		// return a machine in REQUESTED state
		Machine requestedMachine = machine("sir-5", MachineState.REQUESTED);
		when(this.driverMock.startMachines(1, scaleOutConfig()))
				.thenReturn(machines(requestedMachine));

		// run test that requests two additional machines to be started
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// effective size: 3 => ask for 4 machines
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(3));
		this.cloudPool.setDesiredSize(4);
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(4));

		// force a pool resize run
		this.cloudPool.updateMachinePool();

		// verify that cloud driver was asked to start two additional machines
		verify(this.driverMock).startMachines(1, scaleOutConfig());

		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isStartAlert("sir-5")));
	}

	/**
	 * Verify cloud pool behavior when {@link CloudPoolDriver#startMachines}
	 * fails completely without starting any new machine(s).
	 */
	@Test
	public void completelyFailedScaleUpOfMachinePool()
			throws CloudPoolException {
		// set up initial pool
		Machine booting = machine("i-1", MachineState.PENDING);
		Machine active1 = machine("i-2", MachineState.RUNNING);
		Machine active2 = machine("i-3", MachineState.RUNNING);
		Machine terminated = machine("i-4", MachineState.TERMINATED);
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active1, active2, terminated));
		// when asked to start a machine, an error will be raised
		Throwable fault = new StartMachinesException(2, machines(),
				new Exception("failed to add machines"));
		when(this.driverMock.startMachines(2, scaleOutConfig()))
				.thenThrow(fault);

		// run test that requests two additional machines to be started
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// effective size: 3 => ask for 5 machines
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(3));
		this.cloudPool.setDesiredSize(5);
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(5));

		try {
			// force a pool resize run
			this.cloudPool.updateMachinePool();
			fail("cloud pool expected to fail when startMachines fail");
		} catch (CloudPoolException e) {
			// expected
			assertThat(e.getCause(), is(fault));
		}

		// verify that cloud driver was asked to start two additional machines
		verify(this.driverMock).startMachines(2, scaleOutConfig());

		// verify that an error event was posted on event bus
		verify(this.eventBusMock).post(argThat(isAlert(RESIZE.name(), WARN)));
	}

	/**
	 * Verify cloud pool behavior when {@link CloudPoolDriver#startMachines}
	 * fails part-way in to the operation, after it has started a subset of the
	 * requested machines.
	 *
	 * @throws CloudPoolException
	 */
	@Test
	public void partiallyFailedScaleUpOfMachinePool()
			throws CloudPoolException {
		// set up initial pool
		Machine booting = machine("i-1", MachineState.PENDING);
		Machine active1 = machine("i-2", MachineState.RUNNING);
		Machine active2 = machine("i-3", MachineState.RUNNING);
		Machine terminated = machine("i-4", MachineState.TERMINATED);
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active1, active2, terminated));
		// when asked to start two machines, only one is started before an error
		// occurs
		Machine newMachine = machine("i-5", MachineState.PENDING);
		Throwable partialFault = new StartMachinesException(2,
				machines(newMachine),
				new Exception("failed to start second machine"));
		when(this.driverMock.startMachines(2, scaleOutConfig()))
				.thenThrow(partialFault);

		// run test that requests two additional machines to be started
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// effective size: 3 => ask for 5 machines
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(3));
		try {
			this.cloudPool.setDesiredSize(5);
			// force a pool resize run
			this.cloudPool.updateMachinePool();
			fail("cloud pool expected to fail when startMachines fail");
		} catch (CloudPoolException e) {
			// expected
			assertThat(e.getCause(), is(partialFault));
		}
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(5));

		// verify that cloud driver was asked to start two additional machines
		verify(this.driverMock).startMachines(2, scaleOutConfig());

		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isStartAlert("i-5")));

		// verify that an error event was posted on event bus
		verify(this.eventBusMock).post(argThat(isAlert(RESIZE.name(), WARN)));
	}

	/**
	 * Verify cloud pool behavior when scaling down machine pool by a single
	 * machine instance.
	 */
	@Test
	public void singleMachineScaleDownOfMachinePool()
			throws CloudPoolException {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active1, active2, terminated));

		// run test that requests one machine to be terminated
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// effective size: 3 => ask for 2 machines
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(3));
		this.cloudPool.setDesiredSize(2);

		// force a pool resize run
		this.cloudPool.updateMachinePool();

		// verify that cloud driver was asked to terminate the oldest active
		// machine
		verify(this.driverMock).terminateMachine("i-3");
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isTerminationAlert("i-3")));

		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(2));
	}

	/**
	 * Verify that cloud pool enforces the chosen {@link VictimSelectionPolicy}.
	 */
	@Test
	public void victimSelectionPolicyEnforcementOnscaleDown()
			throws CloudPoolException {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active1, active2, terminated));

		// run test that requests one machine to be terminated
		this.cloudPool.configure(poolConfig(NEWEST_INSTANCE, 0));
		this.cloudPool.start();

		// effective size: 3 => ask for 2 machines
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(3));
		this.cloudPool.setDesiredSize(2);

		// force a pool resize run
		this.cloudPool.updateMachinePool();

		// verify that cloud driver was asked to terminate the _newest_ active
		// machine
		verify(this.driverMock).terminateMachine("i-1");
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isTerminationAlert("i-1")));

		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(2));
	}

	/**
	 * Verify cloud pool behavior when scaling down the machine pool with
	 * several machines.
	 */
	@Test
	public void multiMachineScaleDownOfMachinePool() throws CloudPoolException {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active1, active2, terminated));

		// run test that requests two machines to be terminated
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// effective size: 3 => ask for 1 machines
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(3));
		this.cloudPool.setDesiredSize(1);

		// force a pool resize run
		this.cloudPool.updateMachinePool();

		// verify that cloud driver was asked to terminate the two oldest
		// active machines
		verify(this.driverMock).terminateMachine("i-3");
		verify(this.driverMock).terminateMachine("i-2");
		// verify event posted on event bus
		verify(this.eventBusMock)
				.post(argThat(isTerminationAlert("i-2", "i-3")));

		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(1));
	}

	/**
	 * Verify cloud pool behavior when scaling down the machine pool fails.
	 */
	@Test
	public void failedSingleMachineScaleDownOfMachinePool()
			throws CloudPoolException {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active1, active2, terminated));
		// when asked to terminate a machine, the cloud pool will fail
		Exception fault = new CloudPoolDriverException("terminate failed");
		doThrow(fault).when(this.driverMock).terminateMachine(anyString());

		// run test that requests one machine to be terminated
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// effective size: 3 => ask for 2 machines
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(3));
		this.cloudPool.setDesiredSize(2);

		// force a pool resize run
		this.cloudPool.updateMachinePool();

		// verify that cloud driver was asked to terminate the oldest active
		// machine
		verify(this.driverMock).terminateMachine("i-3");
		// verify error events posted on event bus
		verify(this.eventBusMock).post(argThat(isAlert(RESIZE.name(), WARN)));

		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(2));
	}

	/**
	 * Verify cloud pool behavior when scaling down the machine pool with
	 * several instances fails.
	 */
	@Test
	public void failedMultiMachineScaleDownOfMachinePool()
			throws CloudPoolException {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active1, active2, terminated));
		// when asked to terminate a machine, the cloud pool will fail
		Exception fault = new CloudPoolDriverException("terminate failed");
		doThrow(fault).when(this.driverMock).terminateMachine(anyString());

		// run test that requests two machines to be terminated
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// effective size: 3 => ask for 1 machines
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(3));
		this.cloudPool.setDesiredSize(1);

		// force a pool resize run
		this.cloudPool.updateMachinePool();

		// verify that cloud driver was asked to terminate the two oldest
		// active machines
		verify(this.driverMock).terminateMachine("i-3");
		verify(this.driverMock).terminateMachine("i-2");
		// verify error events posted on event bus
		verify(this.eventBusMock, atLeast(2))
				.post(argThat(isAlert(RESIZE.name(), WARN)));

		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(1));
	}

	/**
	 * Verify cloud pool behavior when scaling down the machine pool fails for
	 * some instances but not others.
	 */
	@Test
	public void partiallyFailedScaleDownOfMachinePool()
			throws CloudPoolException {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active1, active2, terminated));
		// when asked to terminate i-3, the cloud pool will fail
		Exception fault = new CloudPoolDriverException("terminate failed");
		doThrow(fault).when(this.driverMock).terminateMachine("i-3");

		// run test that requests two machines to be terminated
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// effective size: 3 => ask for 1 machines
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(3));
		this.cloudPool.setDesiredSize(1);

		// force a pool resize run
		this.cloudPool.updateMachinePool();

		// verify that cloud driver was asked to terminate the two oldest
		// active machines
		verify(this.driverMock).terminateMachine("i-3");
		verify(this.driverMock).terminateMachine("i-2");
		// verify one error event due to failure to terminate i-3
		verify(this.eventBusMock, atMost(1))
				.post(argThat(isAlert(RESIZE.name(), ERROR)));
		// verify that termination of i-2 succeeded
		verify(this.eventBusMock).post(argThat(isTerminationAlert("i-2")));

		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(1));
	}

	/**
	 * Test correct behavior when cloud pool is configured to do late release of
	 * machine instances, such that the termination is delayed to be closer to
	 * the end of the billing hour.
	 */
	@Test
	public void lateTermination() throws CloudPoolException {
		DateTime now = UtcTime.parse("2014-04-22T12:00:00.000Z");
		DateTime launchTime = UtcTime.parse("2014-04-22T11:55:00.000Z");
		FrozenTime.setFixed(now);

		// set up initial pool
		Machine active1 = machine("i-1", RUNNING, launchTime);
		when(this.driverMock.listMachines()).thenReturn(machines(active1));

		// instances are to be released five minutes before next instance hour
		int instanceHourMargin = 300;
		JsonObject config = poolConfig(OLDEST_INSTANCE, instanceHourMargin);
		this.cloudPool.configure(config);
		this.cloudPool.start();

		// run test that requests one machine to be terminated
		// effective size: 1 => ask for 0 machines
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(1));
		this.cloudPool.setDesiredSize(0);

		// force a pool resize run
		this.cloudPool.updateMachinePool();

		// verify that cloud driver was not asked (yet) to terminate and that
		// no resize event has been posted
		verify(this.driverMock, never()).terminateMachine("i-1");
		verifyZeroInteractions(this.eventBusMock);

		// forward time to just prior to when termination should be ordered
		FrozenTime.setFixed(UtcTime.parse("2014-04-22T12:49:59.000Z"));
		this.cloudPool.updateMachinePool();
		// ... termination still shouldn't have been ordered
		verify(this.driverMock, never()).terminateMachine("i-1");
		verifyZeroInteractions(this.eventBusMock);

		// forward time to when termination is due
		FrozenTime.setFixed(UtcTime.parse("2014-04-22T12:50:50.000Z"));
		this.cloudPool.updateMachinePool();
		// ... now termination should have been ordered
		verify(this.driverMock).terminateMachine("i-1");
		// verify event posted on event bus
		verify(this.eventBusMock)
				.post(argThat(IsTerminationAlert.isTerminationAlert("i-1")));

		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullStateStorage() {
		new BaseCloudPool(null, this.driverMock);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullCloudDriver() {
		new BaseCloudPool(STATE_STORAGE, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullEventBus() {
		new BaseCloudPool(STATE_STORAGE, this.driverMock, null);
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
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active1, terminated));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// desiredSize should have been determined to 2
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(2));

		// run pool update (to 2) => no changes are expected
		this.cloudPool.updateMachinePool();
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
		// set up initial cloud pool of size 3
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active1, active2, terminated));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// desiredSize should have been determined to 3
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(3));

		// an external event causes a machine in the pool to be terminated
		// => cloud pool now of size 2
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active1, terminated));
		// when asked to start a machine, it will succeed
		Machine newMachine = machine("i-5", MachineState.PENDING);
		when(this.driverMock.startMachines(1, scaleOutConfig()))
				.thenReturn(machines(newMachine));

		// run pool update (to 3) => scale-out expected
		this.cloudPool.updateMachinePool();

		// verify that cloud driver was asked to start one additional machine
		verify(this.driverMock).startMachines(1, scaleOutConfig());
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isStartAlert("i-5")));

	}

	/**
	 * Run a pool update iteration when pool size is bigger than
	 * {@code desiredSize} (for example, due to some external event causing a
	 * machine to come up) and make sure pool is scaled down.
	 */
	@Test
	public void doPoolUpdateWhenPoolIsTooBig() throws Exception {
		// set up initial cloud pool of size 2
		DateTime now = UtcTime.now();
		Machine booting = machine("i-1", PENDING, now.minus(1));
		Machine active2 = machine("i-3", RUNNING, now.minus(3));
		Machine terminated = machine("i-4", TERMINATED, now.minus(4));
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active2, terminated));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// desiredSize should have been determined to 2
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(2));

		// an external event causes a machine in the pool to be started
		// => cloud pool now of size 3
		Machine active1 = machine("i-2", RUNNING, now.minus(2));
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active1, active2, terminated));

		// run pool update (to 2) => scale-in expected
		this.cloudPool.updateMachinePool();

		// verify that cloud driver was asked to terminate the oldest active
		// machine
		verify(this.driverMock).terminateMachine("i-3");
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isTerminationAlert("i-3")));

		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(2));
	}

	/**
	 * Run a pool update iteration when {@code desiredSize} hasn't yet been
	 * determined (or set) and make sure that it is determined.
	 */
	@Test
	public void doPoolUpdateWhenDesiredSizeIsUnset() throws Exception {
		// determining initial pool size on startup should fail
		when(this.driverMock.listMachines()).thenThrow(
				new CloudPoolDriverException("cloud provider API outage"));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// desiredSize should be undetermined
		assertThat(desiredSize(this.cloudPool), is(nullValue()));

		// make sure desiredSize is determined when pool update is run
		reset(this.driverMock);
		when(this.driverMock.listMachines()).thenReturn(machines());
		// run pool update
		this.cloudPool.updateMachinePool();

		// verify that desiredSize was determined
		assertThat(desiredSize(this.cloudPool), is(0));
	}

	/**
	 * It should not be possible to complete a resize iteration when the cloud
	 * pool cannot be reached.
	 */
	@Test
	public void doPoolUpdateWhenCloudPoolIsUnreachable() throws Exception {
		// determining initial pool size on startup should fail
		when(this.driverMock.listMachines()).thenThrow(
				new CloudPoolDriverException("cloud provider API outage"));
		JsonObject config = poolConfig(OLDEST_INSTANCE, 0);
		this.cloudPool.configure(config);
		this.cloudPool.start();
		// desiredSize should be undetermined
		assertThat(desiredSize(this.cloudPool), is(nullValue()));

		// run pool update
		try {
			this.cloudPool.updateMachinePool();
			fail("should fail");
		} catch (CloudPoolException e) {
			// expected
		}
	}

	/**
	 * Shrink the pool when there are non-evictable ({@link MembershipStatus})
	 * machines in the pool and ensure that the non-evictable machines are never
	 * considered for termination.
	 */
	@Test
	public void scaleInWithOutOfServiceMachinesInPool() throws Exception {
		// set up initial cloud pool of size 2
		DateTime now = UtcTime.now();
		Machine booting = machine("i-3", PENDING, now.minus(1));
		Machine active2 = machine("i-2", RUNNING, now.minus(3));
		// oldest instance is protected from termination
		Machine outOfService = machine("i-1", RUNNING,
				MembershipStatus.awaitingService(), now.minus(4));
		when(this.driverMock.listMachines())
				.thenReturn(machines(booting, active2, outOfService));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
		// desiredSize should be 2 (out-of-service instances don't count)
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(2));

		// order scale-in
		this.cloudPool.setDesiredSize(1);
		// run pool update => scale-in expected
		this.cloudPool.updateMachinePool();

		// verify that the out-of-service machine, despite being the oldest,
		// was not selected for termination
		verify(this.driverMock).terminateMachine("i-2");
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isTerminationAlert("i-2")));

		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(1));
	}

	/**
	 * Verifies that a replacement instance is called in when a machine is
	 * marked with a {@link MembershipStatus} that is inactive.
	 */
	@Test
	public void launchReplacementForInactiveMachines() {
		// set up initial cloud pool of size 2
		DateTime now = UtcTime.now();
		Machine running1 = machine("i-1", RUNNING, now.minus(1));
		Machine running2 = machine("i-2", RUNNING, now.minus(3));
		when(this.driverMock.listMachines())
				.thenReturn(machines(running1, running2));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
		// desiredSize should have been determined to 2
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(2));

		// one of the machines is set to inactive => group size 1.
		running1 = machine("i-1", RUNNING, MembershipStatus.awaitingService(),
				now.minus(1));
		when(this.driverMock.listMachines())
				.thenReturn(machines(running1, running2));

		// when asked to start a machine, it will succeed
		Machine newMachine = machine("i-3", MachineState.PENDING);
		when(this.driverMock.startMachines(1, scaleOutConfig()))
				.thenReturn(machines(newMachine));

		// run pool update => expected to start a replacement instance for i-1
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(2));
		this.cloudPool.updateMachinePool();

		// verify that cloud driver was asked to start one additional machine
		verify(this.driverMock).startMachines(1, scaleOutConfig());
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isStartAlert("i-3")));
	}

	@Test
	public void getPoolSizeOnEmptyPool() {
		// initial pool size, and desiredSize, is 0
		when(this.driverMock.listMachines()).thenReturn(machines());
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
		int desired = this.cloudPool.getPoolSize().getDesiredSize();
		assertThat(this.cloudPool.getPoolSize(),
				is(new PoolSizeSummary(UtcTime.now(), desired, 0, 0)));
	}

	/**
	 * All active machines are considered part of the pool.
	 */
	@Test
	public void getPoolSizeWithActiveMachines() {
		DateTime now = UtcTime.now();
		Machine running1 = machine("i-1", RUNNING,
				MembershipStatus.defaultStatus(), now.minus(1));
		Machine running2 = machine("i-2", RUNNING,
				MembershipStatus.defaultStatus(), now.minus(3));
		Machine pending1 = machine("i-3", PENDING,
				MembershipStatus.defaultStatus(), now.minus(3));
		Machine pending2 = machine("i-4", PENDING,
				MembershipStatus.defaultStatus(), now.minus(3));

		// cloud pool with active instances

		when(this.driverMock.listMachines())
				.thenReturn(machines(running1, running2, pending1, pending2));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
		int desired = this.cloudPool.getPoolSize().getDesiredSize();
		assertThat(this.cloudPool.getPoolSize(),
				is(new PoolSizeSummary(UtcTime.now(), desired, 4, 4)));
	}

	/**
	 * Terminal machine states are to be excluded from the active pool size.
	 */
	@Test
	public void getPoolSizeWithActiveAndTerminalMachines() {
		DateTime now = UtcTime.now();
		Machine running1 = machine("i-1", RUNNING,
				MembershipStatus.defaultStatus(), now.minus(1));
		Machine running2 = machine("i-2", RUNNING,
				MembershipStatus.defaultStatus(), now.minus(3));
		Machine pending1 = machine("i-3", PENDING,
				MembershipStatus.defaultStatus(), now.minus(3));
		Machine pending2 = machine("i-4", PENDING,
				MembershipStatus.defaultStatus(), now.minus(3));
		Machine terminating = machine("i-8", TERMINATING,
				MembershipStatus.defaultStatus(), now.minus(3));
		Machine terminated = machine("i-9", TERMINATED,
				MembershipStatus.defaultStatus(), now.minus(3));

		// cloud pool with instances in active and terminal states
		when(this.driverMock.listMachines()).thenReturn(machines(running1,
				running2, pending1, pending2, terminating, terminated));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
		int desired = this.cloudPool.getPoolSize().getDesiredSize();
		assertThat(this.cloudPool.getPoolSize(),
				is(new PoolSizeSummary(UtcTime.now(), desired, 4, 4)));
	}

	/**
	 * Machines with a {@link MembershipStatus} that is not active should not be
	 * regarded part of the active machine pool.
	 */
	@Test
	public void getPoolSizeWithOnlyInactiveMachines() {
		DateTime now = UtcTime.now();
		Machine outOfService1 = machine("i-6", RUNNING,
				MembershipStatus.awaitingService(), now.minus(3));
		Machine outOfService2 = machine("i-7", RUNNING,
				MembershipStatus.awaitingService(), now.minus(3));

		// cloud pool with only inactive instances
		when(this.driverMock.listMachines())
				.thenReturn(machines(outOfService1, outOfService2));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
		int desired = this.cloudPool.getPoolSize().getDesiredSize();
		assertThat(this.cloudPool.getPoolSize(),
				is(new PoolSizeSummary(UtcTime.now(), desired, 2, 0)));
	}

	/**
	 * Get pool size with machine pool members in a mix of states.
	 */
	@Test
	public void getPoolSizeWithActiveTerminalAndInactiveMachines() {
		DateTime now = UtcTime.now();
		Machine running1 = machine("i-1", RUNNING,
				MembershipStatus.defaultStatus(), now.minus(1));
		Machine running2 = machine("i-2", RUNNING,
				MembershipStatus.defaultStatus(), now.minus(3));
		Machine pending1 = machine("i-3", PENDING,
				MembershipStatus.defaultStatus(), now.minus(3));
		Machine pending2 = machine("i-4", PENDING,
				MembershipStatus.defaultStatus(), now.minus(3));
		Machine outOfService1 = machine("i-6", RUNNING,
				MembershipStatus.awaitingService(), now.minus(3));
		Machine outOfService2 = machine("i-7", RUNNING,
				MembershipStatus.awaitingService(), now.minus(3));
		Machine terminating = machine("i-8", TERMINATING,
				MembershipStatus.defaultStatus(), now.minus(3));
		Machine terminated = machine("i-9", TERMINATED,
				MembershipStatus.defaultStatus(), now.minus(3));

		// cloud pool with mix active, terminal and out-of-service instances
		when(this.driverMock.listMachines())
				.thenReturn(machines(running1, running2, pending1, pending2,
						terminating, terminated, outOfService1, outOfService2));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
		int desired = this.cloudPool.getPoolSize().getDesiredSize();
		assertThat(this.cloudPool.getPoolSize(),
				is(new PoolSizeSummary(UtcTime.now(), desired, 6, 4)));
	}

	/**
	 * Verifies proper behavior when a machine instance in the group is
	 * terminated via {@link CloudPool#terminateMachine(String, boolean)} and a
	 * replacement instance is desired (as marked by setting
	 * {@code decrementDesiredSize} to {@code false}).
	 */
	@Test
	public void terminateMachineWithReplacement() {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine running1 = machine("i-1", RUNNING,
				MembershipStatus.defaultStatus(), now.minus(1));
		when(this.driverMock.listMachines()).thenReturn(machines(running1));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(1));
		assertThat(this.cloudPool.getPoolSize(),
				is(new PoolSizeSummary(UtcTime.now(), 1, 1, 1)));

		// prepare cloud driver to be asked to terminate i-1
		doNothing().when(this.driverMock).terminateMachine("i-1");

		boolean decrementDesiredSize = false;
		this.cloudPool.terminateMachine("i-1", decrementDesiredSize);

		// verify that call was dispatched through and that a replacement is to
		// be called in (that is, desiredSize still set to 1)
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(1));
		verify(this.driverMock).terminateMachine("i-1");
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isTerminationAlert("i-1")));
	}

	/**
	 * Verifies proper behavior when a machine instance in the group is
	 * terminated via {@link CloudPool#terminateMachine(String, boolean)} and no
	 * replacement instance is desired (as marked by setting
	 * {@code decrementDesiredSize} to {@code true}).
	 */
	@Test
	public void terminateMachineWithoutReplacement() {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine running1 = machine("i-1", RUNNING,
				MembershipStatus.defaultStatus(), now.minus(1));
		when(this.driverMock.listMachines()).thenReturn(machines(running1));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(1));
		assertThat(this.cloudPool.getPoolSize(),
				is(new PoolSizeSummary(UtcTime.now(), 1, 1, 1)));

		// prepare cloud driver to be asked to terminate i-1
		doNothing().when(this.driverMock).terminateMachine("i-1");

		boolean decementDesiredSize = true;
		this.cloudPool.terminateMachine("i-1", decementDesiredSize);

		// verify that call was dispatched through and that a replacement won't
		// be called in (that is, desiredSize is decremented)
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(0));
		verify(this.driverMock).terminateMachine("i-1");
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isTerminationAlert("i-1")));
	}

	/**
	 * Tests the scenario when desired size has been set to zero, but not yet
	 * realized, and a terminate is called where we don't want a replacement. We
	 * want to make sure that desiredSize isn't decremented to -1.
	 */
	@Test
	public void terminateWithoutReplacementAfterDesiredSizeSetToZero() {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine running1 = machine("i-1", RUNNING,
				MembershipStatus.defaultStatus(), now.minus(1));
		when(this.driverMock.listMachines()).thenReturn(machines(running1));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(1));

		// desired size gets set to 0
		this.cloudPool.setDesiredSize(0);
		// ... but before the pool has been updated to the new desired size a
		// terminate is requested
		doNothing().when(this.driverMock).terminateMachine("i-1");
		boolean decrementDesiredSize = true;
		this.cloudPool.terminateMachine("i-1", decrementDesiredSize);
		// verify that desired size is left at 0 and isn't decremented to -1!
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(0));
		verify(this.driverMock).terminateMachine("i-1");
	}

	/**
	 * Tests the scenario when desired size has been set to zero, but not yet
	 * realized, and a detach is called where we don't want a replacement. We
	 * want to make sure that desiredSize isn't decremented to -1.
	 */
	@Test
	public void detachWithoutReplacementAfterDesiredSizeSetToZero() {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine running1 = machine("i-1", RUNNING,
				MembershipStatus.defaultStatus(), now.minus(1));
		when(this.driverMock.listMachines()).thenReturn(machines(running1));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(1));

		// desired size gets set to 0
		this.cloudPool.setDesiredSize(0);
		// ... but before the new desired size has been applied a terminate is
		// requested
		doNothing().when(this.driverMock).detachMachine("i-1");
		boolean decrementDesiredSize = true;
		this.cloudPool.detachMachine("i-1", decrementDesiredSize);
		// verify that desired size gets set to 0 and isn't decremented to -1!
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(0));
		verify(this.driverMock).detachMachine("i-1");
	}

	/**
	 * Verifies that {@link CloudPool#setServiceState(String, ServiceState)} is
	 * dispatched through to the {@link CloudPoolDriver}.
	 */
	@Test
	public void setServiceState() {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine running1 = machine("i-1", RUNNING,
				MembershipStatus.defaultStatus(), now.minus(1));
		when(this.driverMock.listMachines()).thenReturn(machines(running1));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// prepare cloud driver to be asked to set state
		doNothing().when(this.driverMock).setServiceState("i-1",
				OUT_OF_SERVICE);

		this.cloudPool.setServiceState("i-1", OUT_OF_SERVICE);

		// verify that call was dispatched through
		verify(this.driverMock).setServiceState("i-1", OUT_OF_SERVICE);

		// verify event posted on event bus
		verify(this.eventBusMock)
				.post(argThat(isSetServiceStateAlert("i-1", OUT_OF_SERVICE)));
	}

	/**
	 * Verifies that
	 * {@link CloudPool#setMembershipStatus(String, MembershipStatus)} is
	 * dispatched through to the {@link CloudPoolDriver}.
	 */
	@Test
	public void setMembershipStatus() {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine running1 = machine("i-1", RUNNING,
				MembershipStatus.defaultStatus(), now.minus(1));
		when(this.driverMock.listMachines()).thenReturn(machines(running1));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// prepare cloud driver to be asked to set state
		doNothing().when(this.driverMock).setMembershipStatus("i-1",
				MembershipStatus.blessed());

		this.cloudPool.setMembershipStatus("i-1", MembershipStatus.blessed());

		// verify that call was dispatched through
		verify(this.driverMock).setMembershipStatus("i-1",
				MembershipStatus.blessed());

		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(
				isMembershipStatusAlert("i-1", MembershipStatus.blessed())));
	}

	/**
	 * Verifies proper behavior when attaching a machine instance to the pool.
	 * The desired size should be incremented.
	 */
	@Test
	public void attachMachine() {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine running1 = machine("i-1", RUNNING,
				MembershipStatus.defaultStatus(), now.minus(1));
		when(this.driverMock.listMachines()).thenReturn(machines(running1));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(1));

		// prepare cloud driver to be asked to attach i-1
		doNothing().when(this.driverMock).attachMachine("i-1");

		this.cloudPool.attachMachine("i-1");

		// verify that call was dispatched through and that a desiredSize was
		// incremented
		verify(this.driverMock).attachMachine("i-1");
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(2));
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isAttachAlert("i-1")));
	}

	/**
	 * Verifies proper behavior when a machine instance in the group is detached
	 * via {@link CloudPool#detachMachine(String, boolean)} and a replacement
	 * instance is desired (marked by setting {@code decrementDesiredSize} to
	 * {@code false}).
	 */
	@Test
	public void detachMachineWithReplacement() {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine running1 = machine("i-1", RUNNING,
				MembershipStatus.defaultStatus(), now.minus(1));
		when(this.driverMock.listMachines()).thenReturn(machines(running1));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(1));
		assertThat(this.cloudPool.getPoolSize(),
				is(new PoolSizeSummary(UtcTime.now(), 1, 1, 1)));

		// prepare cloud driver to be asked to detach i-1
		doNothing().when(this.driverMock).detachMachine("i-1");

		boolean decrementDesiredSize = false;
		this.cloudPool.detachMachine("i-1", decrementDesiredSize);

		// verify that call was dispatched through and that a replacement is to
		// be called in (that is, desiredSize still set to 1)
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(1));
		verify(this.driverMock).detachMachine("i-1");
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isDetachAlert("i-1")));
	}

	/**
	 * Verifies proper behavior when a machine instance in the group is detached
	 * via {@link CloudPool#detachMachine(String, boolean)} and no replacement
	 * instance is desired (marked by setting {@code decrementDesiredSize} to
	 * {@code true}).
	 */
	@Test
	public void detachMachineWithoutReplacement() {
		// set up initial pool
		DateTime now = UtcTime.now();
		Machine running1 = machine("i-1", RUNNING,
				MembershipStatus.defaultStatus(), now.minus(1));
		when(this.driverMock.listMachines()).thenReturn(machines(running1));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(1));
		assertThat(this.cloudPool.getPoolSize(),
				is(new PoolSizeSummary(UtcTime.now(), 1, 1, 1)));

		// prepare cloud driver to be asked to detach i-1
		doNothing().when(this.driverMock).detachMachine("i-1");

		boolean decrementDesiredSize = true;
		this.cloudPool.detachMachine("i-1", decrementDesiredSize);

		// verify that call was dispatched through and that a replacement won't
		// be called in (that is, desiredSize is decremented)
		assertThat(this.cloudPool.getPoolSize().getDesiredSize(), is(0));
		verify(this.driverMock).detachMachine("i-1");
		// verify event posted on event bus
		verify(this.eventBusMock).post(argThat(isDetachAlert("i-1")));
	}

	/**
	 * Verify that a termination alert gets sent even though the pool members
	 * cannot be retrieved (for example, due to temporary limited cloud provider
	 * API accessability).
	 */
	@Test
	public void sendTerminationAlertOnFailureToFetchMachinePool()
			throws CloudPoolException {
		// set up initial pool
		Machine running1 = machine("i-1", RUNNING);
		when(this.driverMock.listMachines()).thenReturn(machines(running1));
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();

		// next listing of machines will fail
		doThrow(new RuntimeException("api outage")).when(this.driverMock)
				.listMachines();

		// terminate and verify that alert gets sent despite failing to retrieve
		// pool members
		this.cloudPool.terminateMachine("i-1", true);
		verify(this.eventBusMock).post(argThat(isTerminationAlert("i-1")));
	}

	@Test(expected = NotStartedException.class)
	public void callGetMachinePoolBeforeStarted() {
		this.cloudPool.getMachinePool();
	}

	@Test(expected = NotStartedException.class)
	public void callSetDesiredSizeBeforeStarted() {
		this.cloudPool.setDesiredSize(0);
	}

	@Test(expected = NotStartedException.class)
	public void callGetPoolSizeBeforeStarted() {
		this.cloudPool.getPoolSize();
	}

	@Test(expected = NotStartedException.class)
	public void callSetServiceStateBeforeStarted() {
		this.cloudPool.setServiceState("i-1", UNKNOWN);
	}

	@Test(expected = NotStartedException.class)
	public void callTerminateMachineBeforeStarted() {
		this.cloudPool.terminateMachine("i-1", false);
	}

	@Test(expected = NotStartedException.class)
	public void callDetachMachineBeforeStarted() {
		this.cloudPool.detachMachine("i-1", false);
	}

	@Test(expected = NotStartedException.class)
	public void callAttachMachineBeforeStarted() {
		this.cloudPool.attachMachine("i-1");
	}

	/**
	 *
	 * It should not be possible to call {@code attachMachine} if the cloud pool
	 * cannot be reached (or its desiredSize cannot be determined).
	 */
	@Test(expected = CloudPoolException.class)
	public void callAttachMachineBeforePoolReachable() {
		setUpUnreachablePool();

		this.cloudPool.attachMachine("i-1");
	}

	/**
	 * It should not be possible to call {@code detachMachine} if the cloud pool
	 * cannot be reached (or its desiredSize cannot be determined).
	 */
	@Test(expected = CloudPoolException.class)
	public void callDetachMachineBeforePoolReachable() {
		setUpUnreachablePool();

		this.cloudPool.detachMachine("i-1", true);
	}

	/**
	 * It should not be possible to call {@code terminateMachine} if the cloud
	 * pool cannot be reached (or its desiredSize cannot be determined).
	 */
	@Test(expected = CloudPoolException.class)
	public void callTerminateMachineBeforePoolReachable() {
		setUpUnreachablePool();

		this.cloudPool.terminateMachine("i-1", true);
	}

	/**
	 * Set up a {@link BaseCloudPool} whose {@link CloudPoolDriver} cannot reach
	 * its cloud API.
	 */
	private void setUpUnreachablePool() {
		doThrow(new RuntimeException("api outage")).when(this.driverMock)
				.listMachines();
		this.cloudPool.configure(poolConfig(OLDEST_INSTANCE, 0));
		this.cloudPool.start();
	}

	/**
	 * Creates a {@link BaseCloudPoolConfig} with a given scale-down victim
	 * selection strategy and instance hour margin.
	 *
	 * @param victimSelectionPolicy
	 * @param instanceHourMargin
	 *            Instance hour margin (in seconds). Zero means immediate
	 *            termination.
	 * @return
	 */
	private JsonObject poolConfig(VictimSelectionPolicy victimSelectionPolicy,
			int instanceHourMargin) {
		CloudPoolConfig scalingGroupConfig = scalingGroupConfig();
		ScaleOutConfig scaleUpConfig = scaleOutConfig();
		ScaleInConfig scaleDownConfig = new ScaleInConfig(victimSelectionPolicy,
				instanceHourMargin);
		PoolFetchConfig poolFetchConfig = new PoolFetchConfig(
				new RetriesConfig(3, new TimeInterval(0L, TimeUnit.SECONDS)),
				new TimeInterval(20L, TimeUnit.SECONDS),
				new TimeInterval(5L, TimeUnit.MINUTES));
		BaseCloudPoolConfig poolConfig = new BaseCloudPoolConfig(
				scalingGroupConfig, scaleUpConfig, scaleDownConfig, null,
				poolFetchConfig, null);

		return JsonUtils.toJson(poolConfig).getAsJsonObject();
	}

	private CloudPoolConfig scalingGroupConfig() {
		return new CloudPoolConfig("MyScalingGroup", cloudCredentialsConfig());
	}

	private JsonObject cloudCredentialsConfig() {
		return JsonUtils.parseJsonString(
				"{\"userName\": \"johndoe\", " + "\"region\": \"us-east-1\"}")
				.getAsJsonObject();
	}

	private ScaleOutConfig scaleOutConfig() {
		String encodedUserData = Base64Utils.toBase64("#!/bin/bash",
				"sudo apt-get update -qy", "sudo apt-get install -qy apache2");
		ScaleOutConfig scaleUpConfig = new ScaleOutConfig("size", "image",
				"keyPair", Arrays.asList("web"), encodedUserData);
		return scaleUpConfig;
	}

	private void save(MachinePool pool, File destination) throws IOException {
		Files.createParentDirs(destination);
		Files.write(JsonUtils.toPrettyString(JsonUtils.toJson(pool)),
				destination, Charsets.UTF_8);
	}

	/**
	 * Returns the current desired size of a {@link BaseCloudPool} or
	 * <code>null</code> if it could not be determined.
	 *
	 * @param cloudPool
	 * @return
	 */
	private Integer desiredSize(BaseCloudPool cloudPool) {
		try {
			return cloudPool.getPoolSize().getDesiredSize();
		} catch (CloudPoolException e) {
			LOG.warn("no desired size could be retrieved: {}", e.getMessage());
			return null;
		}
	}
}
