package com.elastisys.scale.cloudpool.commons.basepool.poolfetcher.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.PoolIdentifiers;
import com.elastisys.scale.cloudpool.commons.basepool.config.RetriesConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriver;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Stopwatch;

/**
 * Exercise the {@link RetryingPoolFetcher}.
 */
public class TestRetryingPoolFetcher {

	private final CloudPoolDriver mockDriver = mock(CloudPoolDriver.class);

	private static final RetriesConfig RETRIES_CONFIG = new RetriesConfig(3,
			new TimeInterval(10L, TimeUnit.MILLISECONDS));
	/** Object under test. */
	private RetryingPoolFetcher fetcher;

	@Before
	public void beforeTestMethod() {
		FrozenTime.setFixed(UtcTime.parse("2015-11-16T12:00:00.000Z"));

		this.fetcher = new RetryingPoolFetcher(this.mockDriver, RETRIES_CONFIG);
	}

	/**
	 * No retries are to be performed on success.
	 */
	@Test
	public void getOnImmediateSuccess() {

		when(this.mockDriver.listMachines()).thenReturn(machines("i-1", "i-2"));

		MachinePool pool = this.fetcher.get();
		assertThat(pool.getTimestamp(), is(UtcTime.now()));
		assertThat(pool.getMachines(), is(machines("i-1", "i-2")));

		// verify that cloud pool driver was called one single time
		verify(this.mockDriver, times(1)).listMachines();
	}

	/**
	 * Verify that retries are only made until success and that back-off delay
	 * is introduced between attempts.
	 */
	@Test
	public void getOnEventualSuccess() {
		// fail two times and succeed on third attempt
		when(this.mockDriver.listMachines())
				.thenThrow(new CloudPoolDriverException("api outage"))
				.thenThrow(new CloudPoolDriverException("api outage"))
				.thenReturn(machines("i-1", "i-2"));

		Stopwatch stopwatch = Stopwatch.createStarted();
		MachinePool pool = this.fetcher.get();
		assertThat(pool.getTimestamp(), is(UtcTime.now()));
		assertThat(pool.getMachines(), is(machines("i-1", "i-2")));
		stopwatch.stop();

		// verify that cloud pool driver was called repeatedly
		verify(this.mockDriver, times(3)).listMachines();
		// verify that some back-off delay was introduced
		assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) >= RETRIES_CONFIG
				.getInitialBackoffDelay().getTime() * 2);
	}

	/**
	 * After all retries have been exhausted, fail.
	 */
	@Test
	public void failAfterExhaustingRetries() {
		// never succeeds
		when(this.mockDriver.listMachines())
				.thenThrow(new CloudPoolDriverException("api outage"));

		Stopwatch stopwatch = Stopwatch.createStarted();
		try {
			this.fetcher.get();
			fail("should not succeed");
		} catch (CloudPoolException e) {
			// expected
		}
		stopwatch.stop();

		// verify that cloud pool driver was called for the maximum number of
		// retries
		verify(this.mockDriver, times(1 + RETRIES_CONFIG.getMaxRetries()))
				.listMachines();
		// verify that some back-off delay was introduced
		assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) >= RETRIES_CONFIG
				.getInitialBackoffDelay().getTime() * 3);
	}

	/**
	 * It should be possible to configure the {@link RetryingPoolFetcher} to not
	 * retry failed attempts.
	 */
	@Test
	public void withNoRetries() {
		RetriesConfig noRetries = new RetriesConfig(0,
				new TimeInterval(0L, TimeUnit.MILLISECONDS));
		this.fetcher = new RetryingPoolFetcher(this.mockDriver, noRetries);

		// never succeeds
		when(this.mockDriver.listMachines())
				.thenThrow(new CloudPoolDriverException("api outage"));

		try {
			this.fetcher.get();
			fail("should not succeed");
		} catch (CloudPoolException e) {
			// expected
		}

		// verify that driver was called only once (no retry)
		verify(this.mockDriver, times(1)).listMachines();
	}

	private List<Machine> machines(String... machineIds) {
		List<Machine> machines = new ArrayList<>();
		for (String id : machineIds) {
			machines.add(Machine.builder().id(id).machineSize("m1.medium")
					.machineState(MachineState.RUNNING)
					.cloudProvider(PoolIdentifiers.AWS_EC2).build());
		}
		return machines;
	}

}
