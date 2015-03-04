package com.elastisys.scale.cloudpool.splitter;

import static com.elastisys.scale.cloudpool.splitter.testutils.TestUtils.machine;
import static com.elastisys.scale.cloudpool.splitter.testutils.TestUtils.pool;
import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Callable;

import jersey.repackaged.com.google.common.collect.Lists;

import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.splitter.config.PoolSizeCalculator;
import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.elastisys.scale.cloudpool.splitter.config.SplitterConfig;
import com.elastisys.scale.cloudpool.splitter.requests.RequestFactory;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.util.concurrent.Callables;
import com.google.gson.JsonObject;

/**
 * Tests that exercise the {@link Splitter}.
 */
public class TestSplitterOperation {

	/** Mock request factory. */
	private RequestFactory requests = mock(RequestFactory.class);
	/** Object under test. */
	private Splitter splitter;

	@Before
	public void beforeTestMethod() {
		this.splitter = new Splitter(this.requests);
		FrozenTime.setFixed(UtcTime.now());
	}

	/**
	 * Verify that a get machine pool call gets dispatched to all child pools
	 * and is properly merged.
	 */
	@Test
	public void getEmptyMachinePool() {
		SplitterConfig config = config(50, 30, 20);
		PrioritizedCloudPool backendPool1 = config.getBackendPools().get(0);
		PrioritizedCloudPool backendPool2 = config.getBackendPools().get(1);
		PrioritizedCloudPool backendPool3 = config.getBackendPools().get(2);
		// set up mock pool
		MachinePool pool1 = pool();
		MachinePool pool2 = pool();
		MachinePool pool3 = pool();
		setupMockPools(config.getBackendPools(), pool1, pool2, pool3);

		// carry out calls
		this.splitter.configure(toJson(config));
		MachinePool pool = this.splitter.getMachinePool();
		MachinePool expectedPool = pool();
		assertThat(pool.getMachines(), is(expectedPool.getMachines()));

		// verify that request factory was called with expected arguments
		verify(this.requests, atLeast(1))
				.newGetMachinePoolRequest(backendPool1);
		verify(this.requests, atLeast(1))
				.newGetMachinePoolRequest(backendPool2);
		verify(this.requests, atLeast(1))
				.newGetMachinePoolRequest(backendPool3);
	}

	/**
	 * Verify that a get machine pool call gets dispatched to all child pools
	 * and is properly merged.
	 */
	@Test
	public void getNonEmptyMachinePool() {
		SplitterConfig config = config(50, 30, 20);
		PrioritizedCloudPool backendPool1 = config.getBackendPools().get(0);
		PrioritizedCloudPool backendPool2 = config.getBackendPools().get(1);
		PrioritizedCloudPool backendPool3 = config.getBackendPools().get(2);
		MachinePool pool1 = pool(machine(1), machine(2));
		MachinePool pool2 = pool(machine(3));
		MachinePool pool3 = pool(machine(4), machine(5));
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool1, pool2, pool3);

		// carry out calls
		this.splitter.configure(toJson(config));
		MachinePool pool = this.splitter.getMachinePool();
		MachinePool expectedPool = pool(machine(1), machine(2), machine(3),
				machine(4), machine(5));
		assertThat(pool.getMachines(), is(expectedPool.getMachines()));

		// verify that request factory was called with expected arguments
		verify(this.requests, atLeast(1))
				.newGetMachinePoolRequest(backendPool1);
		verify(this.requests, atLeast(1))
				.newGetMachinePoolRequest(backendPool2);
		verify(this.requests, atLeast(1))
				.newGetMachinePoolRequest(backendPool3);
	}

	/**
	 * Attempting to get machine pool call when a sub-pool fails to respond
	 * properly should result in an error.
	 */
	@Test(expected = CloudPoolException.class)
	public void getMachinePoolOnHttpError() {
		SplitterConfig config = config(50, 30, 20);
		PrioritizedCloudPool backendPool2 = config.getBackendPools().get(1);

		MachinePool pool1 = pool(machine(1), machine(2));
		MachinePool pool2 = pool(machine(3));
		MachinePool pool3 = pool(machine(4), machine(5));
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool1, pool2, pool3);
		// call to pool 2 will fail
		when(this.requests.newGetMachinePoolRequest(backendPool2)).thenReturn(
				failingPoolRequest(500, "internal server error"));

		this.splitter.configure(toJson(config));
		this.splitter.getMachinePool();
	}

	/**
	 * Verify that desired size gets determined on startup after configuration
	 * set.
	 */
	@Test
	public void determineInitialDesiredSize() {
		SplitterConfig config = config(50, 30, 20);
		MachinePool pool1 = pool(machine(1), machine(2));
		MachinePool pool2 = pool(machine(3));
		MachinePool pool3 = pool(machine(4), machine(5));
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool1, pool2, pool3);

		// carry out calls
		assertThat(this.splitter.getDesiredSize(), is(nullValue()));
		this.splitter.configure(toJson(config));
		assertThat(this.splitter.getDesiredSize(), is(5));
	}

	/**
	 * Verify that a failure to determine the initial desired size on startup
	 * isn't critical (it will be determined later when all pools can be
	 * reached).
	 */
	@Test
	public void testFailureToDetermineInitialDesiredSize() {
		SplitterConfig config = config(100);
		PrioritizedCloudPool pool1 = config.getBackendPools().get(0);
		when(this.requests.newGetMachinePoolRequest(pool1)).thenReturn(
				failingPoolRequest(500, "internal server error"));

		// carry out calls
		assertThat(this.splitter.getDesiredSize(), is(nullValue()));
		this.splitter.configure(toJson(config));
		assertThat(this.splitter.getDesiredSize(), is(nullValue()));
	}

	/**
	 * Verify proper dispatch of getPoolSize to child pools.
	 */
	@Test
	public void getPoolSize() {
		SplitterConfig config = config(50, 30, 20);
		MachinePool pool1 = pool(machine(1), machine(2));
		MachinePool pool2 = pool(machine(3));
		MachinePool pool3 = pool(machine(4), machine(5));
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool1, pool2, pool3);
		this.splitter.configure(toJson(config));
		assertThat(this.splitter.getPoolSize(),
				is(new PoolSizeSummary(5, 5, 0)));
	}

	/**
	 * Attempting to get pool size call when a sub-pool fails to respond
	 * properly should result in an error.
	 */
	@Test(expected = CloudPoolException.class)
	public void getPoolSizeOnError() {
		SplitterConfig config = config(50, 30, 20);
		MachinePool pool1 = pool(machine(1), machine(2));
		MachinePool pool2 = pool(machine(3));
		MachinePool pool3 = pool(machine(4), machine(5));
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool1, pool2, pool3);
		// call to get pool 3 will fail
		PrioritizedCloudPool backendPool3 = config.getBackendPools().get(2);
		when(this.requests.newGetMachinePoolRequest(backendPool3)).thenReturn(
				failingPoolRequest(500, "internal server error"));

		this.splitter.configure(toJson(config));
		this.splitter.getPoolSize();
	}

	/**
	 * Verify that a set desired size call is properly divided and dispatched to
	 * all child pools.
	 */
	@Test
	public void setDesiredSize() {
		SplitterConfig config = config(50, 30, 20);
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool(), pool(), pool());
		PrioritizedCloudPool pool1 = config.getBackendPools().get(0);
		PrioritizedCloudPool pool2 = config.getBackendPools().get(1);
		PrioritizedCloudPool pool3 = config.getBackendPools().get(2);

		// prepare request factory for calls to produce tasks
		when(this.requests.newSetDesiredSizeRequest(pool1, 3)).thenReturn(
				succesfulRequest());
		when(this.requests.newSetDesiredSizeRequest(pool2, 2)).thenReturn(
				succesfulRequest());
		when(this.requests.newSetDesiredSizeRequest(pool3, 0)).thenReturn(
				succesfulRequest());

		// carry out calls
		this.splitter.configure(toJson(config));
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(0));
		this.splitter.setDesiredSize(5);
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(5));

		// verify that request factory was called with expected arguments
		verify(this.requests).newSetDesiredSizeRequest(pool1, 3);
		verify(this.requests).newSetDesiredSizeRequest(pool2, 2);
		verify(this.requests).newSetDesiredSizeRequest(pool3, 0);
	}

	/**
	 * Attempting to set desired size should fail when a sub-pool fails to
	 * respond properly.
	 */
	@Test
	public void setDesiredSizeOnError() {
		SplitterConfig config = config(50, 30, 20);
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool(), pool(), pool());
		PrioritizedCloudPool pool1 = config.getBackendPools().get(0);
		PrioritizedCloudPool pool2 = config.getBackendPools().get(1);
		PrioritizedCloudPool pool3 = config.getBackendPools().get(2);

		// prepare request factory for calls to produce tasks
		when(this.requests.newSetDesiredSizeRequest(pool1, 3)).thenReturn(
				failingRequest(500, "internal error"));
		when(this.requests.newSetDesiredSizeRequest(pool2, 2)).thenReturn(
				succesfulRequest());
		when(this.requests.newSetDesiredSizeRequest(pool3, 0)).thenReturn(
				succesfulRequest());

		// carry out calls
		this.splitter.configure(toJson(config));
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(0));
		try {
			this.splitter.setDesiredSize(5);
			fail("should not succeed");
		} catch (CloudPoolException e) {
			// expected to fail
		}
		// total desired size should still have been set
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(5));
	}

	/**
	 * Verify behavior when a member machine is to be terminated from one of the
	 * child pools, and a replacement instance is desired.
	 */
	@Test
	public void terminateMachineWithReplacement() {
		SplitterConfig config = config(50, 30, 20);
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool(machine(1)),
				pool(machine(2)), pool(machine(3)));
		PrioritizedCloudPool pool1 = config.getBackendPools().get(0);
		PrioritizedCloudPool pool2 = config.getBackendPools().get(1);
		PrioritizedCloudPool pool3 = config.getBackendPools().get(2);

		// i-3 lives in pool3's pool
		when(this.requests.newTerminateMachineRequest(pool1, "i-3", false))
				.thenReturn(failingRequest(404, "not found"));
		when(this.requests.newTerminateMachineRequest(pool2, "i-3", false))
				.thenReturn(failingRequest(404, "not found"));
		when(this.requests.newTerminateMachineRequest(pool3, "i-3", false))
				.thenReturn(succesfulRequest());

		// carry out calls
		this.splitter.configure(toJson(config));
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(3));
		boolean decrementDesiredSize = false;
		this.splitter.terminateMachine("i-3", decrementDesiredSize);
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(3));

		// verify that request factory was called with expected arguments
		verify(this.requests).newTerminateMachineRequest(pool1, "i-3", false);
		verify(this.requests).newTerminateMachineRequest(pool2, "i-3", false);
		verify(this.requests).newTerminateMachineRequest(pool3, "i-3", false);
	}

	/**
	 * Verify behavior when a member machine is to be terminated from one of the
	 * child pools, and no replacement instance is desired.
	 */
	@Test
	public void terminateMachineWithoutReplacement() {
		SplitterConfig config = config(50, 30, 20);
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool(machine(1)),
				pool(machine(2)), pool(machine(3)));
		PrioritizedCloudPool pool1 = config.getBackendPools().get(0);
		PrioritizedCloudPool pool2 = config.getBackendPools().get(1);
		PrioritizedCloudPool pool3 = config.getBackendPools().get(2);

		// i-3 lives in pool3's pool
		when(this.requests.newTerminateMachineRequest(pool1, "i-3", true))
				.thenReturn(failingRequest(404, "not found"));
		when(this.requests.newTerminateMachineRequest(pool2, "i-3", true))
				.thenReturn(failingRequest(404, "not found"));
		when(this.requests.newTerminateMachineRequest(pool3, "i-3", true))
				.thenReturn(succesfulRequest());

		// carry out calls
		this.splitter.configure(toJson(config));
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(3));
		boolean decrementDesiredSize = true;
		this.splitter.terminateMachine("i-3", decrementDesiredSize);
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(2));

		// verify that request factory was called with expected arguments
		verify(this.requests).newTerminateMachineRequest(pool1, "i-3", true);
		verify(this.requests).newTerminateMachineRequest(pool2, "i-3", true);
		verify(this.requests).newTerminateMachineRequest(pool3, "i-3", true);
	}

	/**
	 * Verify behavior when a member machine is to be terminated but none of the
	 * child pools recognizes it.
	 */
	@Test
	public void terminateNonExistingMachine() {
		SplitterConfig config = config(50, 30, 20);
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool(machine(1)),
				pool(machine(2)), pool(machine(3)));
		PrioritizedCloudPool pool1 = config.getBackendPools().get(0);
		PrioritizedCloudPool pool2 = config.getBackendPools().get(1);
		PrioritizedCloudPool pool3 = config.getBackendPools().get(2);

		// all pools respond with 404 - Not Found
		when(this.requests.newTerminateMachineRequest(pool1, "i-3", true))
				.thenReturn(failingRequest(404, "not found"));
		when(this.requests.newTerminateMachineRequest(pool2, "i-3", true))
				.thenReturn(failingRequest(404, "not found"));
		when(this.requests.newTerminateMachineRequest(pool3, "i-3", true))
				.thenReturn(failingRequest(404, "not found"));

		// carry out calls
		this.splitter.configure(toJson(config));
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(3));
		boolean decrementDesiredSize = true;
		try {
			this.splitter.terminateMachine("i-3", decrementDesiredSize);
			fail("should not be successful");
		} catch (Exception e) {
			// expected
		}
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(3));

		// verify that request factory was called with expected arguments
		verify(this.requests).newTerminateMachineRequest(pool1, "i-3", true);
		verify(this.requests).newTerminateMachineRequest(pool2, "i-3", true);
		verify(this.requests).newTerminateMachineRequest(pool3, "i-3", true);
	}

	/**
	 * Verify behavior when a member machine is to be detached from one of the
	 * child pools, and a replacement instance is desired.
	 */
	@Test
	public void detachMachineWithReplacement() {
		SplitterConfig config = config(50, 30, 20);
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool(machine(1)),
				pool(machine(2)), pool(machine(3)));
		PrioritizedCloudPool pool1 = config.getBackendPools().get(0);
		PrioritizedCloudPool pool2 = config.getBackendPools().get(1);
		PrioritizedCloudPool pool3 = config.getBackendPools().get(2);

		// i-3 lives in pool3's pool
		when(this.requests.newDetachMachineRequest(pool1, "i-3", false))
				.thenReturn(failingRequest(404, "not found"));
		when(this.requests.newDetachMachineRequest(pool2, "i-3", false))
				.thenReturn(failingRequest(404, "not found"));
		when(this.requests.newDetachMachineRequest(pool3, "i-3", false))
				.thenReturn(succesfulRequest());

		// carry out calls
		this.splitter.configure(toJson(config));
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(3));
		boolean decrementDesiredSize = false;
		this.splitter.detachMachine("i-3", decrementDesiredSize);
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(3));

		// verify that request factory was called with expected arguments
		verify(this.requests).newDetachMachineRequest(pool1, "i-3", false);
		verify(this.requests).newDetachMachineRequest(pool2, "i-3", false);
		verify(this.requests).newDetachMachineRequest(pool3, "i-3", false);
	}

	/**
	 * Verify behavior when a member machine is to be detached from one of the
	 * child pools, and no replacement instance is desired.
	 */
	@Test
	public void detachMachineWithoutReplacement() {
		SplitterConfig config = config(50, 30, 20);
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool(machine(1)),
				pool(machine(2)), pool(machine(3)));
		PrioritizedCloudPool pool1 = config.getBackendPools().get(0);
		PrioritizedCloudPool pool2 = config.getBackendPools().get(1);
		PrioritizedCloudPool pool3 = config.getBackendPools().get(2);

		// i-3 lives in pool3's pool
		when(this.requests.newDetachMachineRequest(pool1, "i-3", true))
				.thenReturn(failingRequest(404, "not found"));
		when(this.requests.newDetachMachineRequest(pool2, "i-3", true))
				.thenReturn(failingRequest(404, "not found"));
		when(this.requests.newDetachMachineRequest(pool3, "i-3", true))
				.thenReturn(succesfulRequest());

		// carry out calls
		this.splitter.configure(toJson(config));
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(3));
		boolean decrementDesiredSize = true;
		this.splitter.detachMachine("i-3", decrementDesiredSize);
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(2));

		// verify that request factory was called with expected arguments
		verify(this.requests).newDetachMachineRequest(pool1, "i-3", true);
		verify(this.requests).newDetachMachineRequest(pool2, "i-3", true);
		verify(this.requests).newDetachMachineRequest(pool3, "i-3", true);
	}

	/**
	 * Verify behavior when a member machine is to be detached but none of the
	 * child pools recognizes it.
	 */
	@Test
	public void detachNonExistingMachine() {
		SplitterConfig config = config(50, 30, 20);
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool(machine(1)),
				pool(machine(2)), pool(machine(3)));
		PrioritizedCloudPool pool1 = config.getBackendPools().get(0);
		PrioritizedCloudPool pool2 = config.getBackendPools().get(1);
		PrioritizedCloudPool pool3 = config.getBackendPools().get(2);

		// all pools respond with 404 - Not Found
		when(this.requests.newDetachMachineRequest(pool1, "i-3", true))
				.thenReturn(failingRequest(404, "not found"));
		when(this.requests.newDetachMachineRequest(pool2, "i-3", true))
				.thenReturn(failingRequest(404, "not found"));
		when(this.requests.newDetachMachineRequest(pool3, "i-3", true))
				.thenReturn(failingRequest(404, "not found"));

		// carry out calls
		this.splitter.configure(toJson(config));
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(3));
		boolean decrementDesiredSize = true;
		try {
			this.splitter.detachMachine("i-3", decrementDesiredSize);
			fail("should not be successful");
		} catch (Exception e) {
			// expected
		}
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(3));

		// verify that request factory was called with expected arguments
		verify(this.requests).newDetachMachineRequest(pool1, "i-3", true);
		verify(this.requests).newDetachMachineRequest(pool2, "i-3", true);
		verify(this.requests).newDetachMachineRequest(pool3, "i-3", true);
	}

	/**
	 * Verify behavior when a machine is to be attached to one of the child
	 * pools.
	 */
	@Test
	public void attachMachine() {
		SplitterConfig config = config(50, 30, 20);
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool(machine(1)),
				pool(machine(2)), pool(machine(3)));
		PrioritizedCloudPool pool1 = config.getBackendPools().get(0);
		PrioritizedCloudPool pool2 = config.getBackendPools().get(1);
		PrioritizedCloudPool pool3 = config.getBackendPools().get(2);

		// pool2 will accept i-3
		when(this.requests.newAttachMachineRequest(pool1, "i-3")).thenReturn(
				failingRequest(404, "not found"));
		when(this.requests.newAttachMachineRequest(pool2, "i-3")).thenReturn(
				succesfulRequest());
		when(this.requests.newAttachMachineRequest(pool3, "i-3")).thenReturn(
				failingRequest(404, "not found"));

		// carry out calls
		this.splitter.configure(toJson(config));
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(3));
		this.splitter.attachMachine("i-3");
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(4));

		// verify that request factory was called with expected arguments
		verify(this.requests).newAttachMachineRequest(pool1, "i-3");
		verify(this.requests).newAttachMachineRequest(pool2, "i-3");
		verify(this.requests).newAttachMachineRequest(pool3, "i-3");
	}

	/**
	 * Attaching a machine that no cloud pool can attach should raise an error.
	 */
	@Test
	public void attachNonExistingMachine() {
		SplitterConfig config = config(50, 30, 20);
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool(machine(1)),
				pool(machine(2)), pool(machine(3)));
		PrioritizedCloudPool pool1 = config.getBackendPools().get(0);
		PrioritizedCloudPool pool2 = config.getBackendPools().get(1);
		PrioritizedCloudPool pool3 = config.getBackendPools().get(2);

		// no pool will accept i-3
		when(this.requests.newAttachMachineRequest(pool1, "i-3")).thenReturn(
				failingRequest(404, "not found"));
		when(this.requests.newAttachMachineRequest(pool2, "i-3")).thenReturn(
				failingRequest(404, "not found"));
		when(this.requests.newAttachMachineRequest(pool3, "i-3")).thenReturn(
				failingRequest(404, "not found"));

		// carry out calls
		this.splitter.configure(toJson(config));
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(3));
		try {
			this.splitter.attachMachine("i-3");
			fail("should not succeed");
		} catch (CloudPoolException e) {
			// expected
		}
		assertThat(this.splitter.getPoolSize().getDesiredSize(), is(3));

		// verify that request factory was called with expected arguments
		verify(this.requests).newAttachMachineRequest(pool1, "i-3");
		verify(this.requests).newAttachMachineRequest(pool2, "i-3");
		verify(this.requests).newAttachMachineRequest(pool3, "i-3");
	}

	/**
	 * Test setting service state on a member instance of one of the child
	 * pools.
	 */
	@Test
	public void setServiceState() {
		SplitterConfig config = config(50, 30, 20);
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool(machine(1)),
				pool(machine(2)), pool(machine(3)));
		PrioritizedCloudPool pool1 = config.getBackendPools().get(0);
		PrioritizedCloudPool pool2 = config.getBackendPools().get(1);
		PrioritizedCloudPool pool3 = config.getBackendPools().get(2);

		// pool2 will accept a new state for i-3
		when(
				this.requests.newSetServiceStateRequest(pool1, "i-3",
						ServiceState.IN_SERVICE)).thenReturn(
				failingRequest(404, "not found"));
		when(
				this.requests.newSetServiceStateRequest(pool2, "i-3",
						ServiceState.IN_SERVICE))
				.thenReturn(succesfulRequest());
		when(
				this.requests.newSetServiceStateRequest(pool3, "i-3",
						ServiceState.IN_SERVICE)).thenReturn(
				failingRequest(404, "not found"));

		// carry out calls
		this.splitter.configure(toJson(config));
		this.splitter.setServiceState("i-3", ServiceState.IN_SERVICE);

		// verify that request factory was called with expected arguments
		verify(this.requests).newSetServiceStateRequest(pool1, "i-3",
				ServiceState.IN_SERVICE);
		verify(this.requests).newSetServiceStateRequest(pool2, "i-3",
				ServiceState.IN_SERVICE);
		verify(this.requests).newSetServiceStateRequest(pool3, "i-3",
				ServiceState.IN_SERVICE);
	}

	/**
	 * Verify that an error is raised on an attempt to set service state for a
	 * machine that isn't recognized as a member in any of the child pools.
	 */
	@Test(expected = CloudPoolException.class)
	public void setServiceStateOnNonExistingMember() {
		SplitterConfig config = config(50, 30, 20);
		// set up mock pool
		setupMockPools(config.getBackendPools(), pool(machine(1)),
				pool(machine(2)), pool(machine(3)));
		PrioritizedCloudPool pool1 = config.getBackendPools().get(0);
		PrioritizedCloudPool pool2 = config.getBackendPools().get(1);
		PrioritizedCloudPool pool3 = config.getBackendPools().get(2);

		// no pool will accept a new state for i-X
		when(
				this.requests.newSetServiceStateRequest(pool1, "i-X",
						ServiceState.IN_SERVICE)).thenReturn(
				failingRequest(404, "not found"));
		when(
				this.requests.newSetServiceStateRequest(pool2, "i-X",
						ServiceState.IN_SERVICE)).thenReturn(
				failingRequest(404, "not found"));
		when(
				this.requests.newSetServiceStateRequest(pool3, "i-X",
						ServiceState.IN_SERVICE)).thenReturn(
				failingRequest(404, "not found"));

		// carry out calls
		this.splitter.configure(toJson(config));
		this.splitter.setServiceState("i-X", ServiceState.IN_SERVICE);
	}

	/**
	 * Verify that re-configuring (stopping and then re-starting) the
	 * {@link Splitter} leaves it in a useful state.
	 */
	@Test
	public void testReconfigure() {
		SplitterConfig config = config(50, 50);
		// set up mock pool
		MachinePool pool1 = pool();
		MachinePool pool2 = pool();
		setupMockPools(config.getBackendPools(), pool1, pool2);

		// configure and use splitter
		this.splitter.configure(toJson(config));
		assertThat(this.splitter.getConfiguration().get(), is(toJson(config)));
		assertThat(this.splitter.getMachinePool(), is(pool()));

		// re-configure and make sure it is still useable
		SplitterConfig newConfig = config(60, 40);
		this.splitter.configure(toJson(newConfig));
		assertThat(this.splitter.getConfiguration().get(),
				is(toJson(newConfig)));
		assertThat(this.splitter.getMachinePool(), is(pool()));
	}

	@Test(expected = IllegalStateException.class)
	public void getMachinePoolBeforeConfigured() {
		this.splitter.getMachinePool();
	}

	@Test(expected = IllegalStateException.class)
	public void getPoolSizeBeforeConfigured() {
		this.splitter.getPoolSize();
	}

	@Test(expected = IllegalStateException.class)
	public void setDesiredSizeBeforeConfigured() {
		this.splitter.setDesiredSize(0);
	}

	@Test(expected = IllegalStateException.class)
	public void terminateMachineBeforeConfigured() {
		this.splitter.terminateMachine("i-1", false);
	}

	@Test(expected = IllegalStateException.class)
	public void setServiceStateBeforeConfigured() {
		this.splitter.setServiceState("i-1", ServiceState.OUT_OF_SERVICE);
	}

	@Test(expected = IllegalStateException.class)
	public void attachMachineBeforeConfigured() {
		this.splitter.attachMachine("i-1");
	}

	@Test(expected = IllegalStateException.class)
	public void detachMachineBeforeConfigured() {
		this.splitter.detachMachine("i-1", false);
	}

	/**
	 * Set up mock machine pools for the simulated back-end cloud pools for the
	 * {@link Splitter} under test. The {@link Splitter}'s request factory is
	 * set up to produce {@code GetMachinePool} tasks that fetch the specified
	 * machine pools.
	 *
	 * @param backendPool
	 *            A list of remote cloud pools.
	 * @param pools
	 *            Machine pools
	 */
	private void setupMockPools(List<PrioritizedCloudPool> backendPools,
			MachinePool... pools) {
		checkArgument(backendPools.size() == pools.length,
				"number of pools and number of machine pools do not match");

		for (int i = 0; i < backendPools.size(); i++) {
			when(this.requests.newGetMachinePoolRequest(backendPools.get(i)))
					.thenReturn(succesfulPoolRequest(pools[i]));
		}
	}

	private JsonObject toJson(Object object) {
		return JsonUtils.toJson(object).getAsJsonObject();
	}

	private SplitterConfig config(int... poolPriorities) {
		List<PrioritizedCloudPool> pools = Lists.newArrayList();
		int index = 0;
		for (int priority : poolPriorities) {
			index++;
			pools.add(new PrioritizedCloudPool(priority, "poolHost" + index,
					index, null, null));
		}
		return new SplitterConfig(PoolSizeCalculator.STRICT, pools, 30);
	}

	private Callable<MachinePool> succesfulPoolRequest(MachinePool pool) {
		return Callables.returning(pool);
	}

	private Callable<MachinePool> failingPoolRequest(final int statusCode,
			final String message) {
		return new Callable<MachinePool>() {
			@Override
			public MachinePool call() throws Exception {
				throw new HttpResponseException(statusCode, message);
			}
		};
	}

	private Callable<Void> succesfulRequest() {
		return Callables.returning(null);
	}

	private Callable<Void> failingRequest(final int statusCode,
			final String message) {
		return new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				throw new HttpResponseException(statusCode, message);
			}
		};
	}

}
