package com.elastisys.scale.cloudadapters.api.restapi;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.restapi.PoolHandler;
import com.elastisys.scale.cloudadapers.api.restapi.types.PoolResizeRequest;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.JsonObject;

/**
 * Verifies that the {@link PoolHandler} processes resize requests in a serial
 * (one-by-one) manner to prevent concurrent modifications of the machine pool
 * from leading to excessive scale-ups/scale-downs.
 * 
 * 
 * 
 */
public class TestPoolHandlerConcurrency {

	/** The object under test. */
	private PoolHandler restEndpoint;
	/**
	 * Mock backing {@link CloudAdapter} that endpoint will dispatch incoming
	 * calls to.
	 */
	private ProcessTimeRegistratingCloudAdapter cloudAdapter = new ProcessTimeRegistratingCloudAdapter(
			10);

	@Before
	public void onSetup() {
		this.restEndpoint = new PoolHandler(this.cloudAdapter);
	}

	/**
	 * Verify that two concurrently submitted resize requests are serialized
	 * (processed one-by-one) by the {@link PoolHandler}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSerializationOfConcurrentInvocations() throws Exception {
		ExecutorService threadPool = Executors.newFixedThreadPool(2);

		// post two concurrent resize requests
		Future<Void> request1 = threadPool.submit(new PoolResizeRequester(
				this.restEndpoint, new PoolResizeRequest(3)));
		Future<Void> request2 = threadPool.submit(new PoolResizeRequester(
				this.restEndpoint, new PoolResizeRequest(3)));
		request1.get();
		request2.get();

		// verify that the server-side processing of the requests did not
		// overlap in time.
		assertThat(this.cloudAdapter.getProcessingTimes().size(), is(2));
		Interval request1Processing = this.cloudAdapter.getProcessingTimes()
				.get(0);
		Interval request2Processing = this.cloudAdapter.getProcessingTimes()
				.get(1);
		assertFalse("concurrent resize requests were not handled serialized",
				request1Processing.overlaps(request2Processing));

	}

	private static class ProcessTimeRegistratingCloudAdapter implements
			CloudAdapter {
		/**
		 * Tracks the processing time periods of all resize requests (from
		 * reception to completion).
		 */
		private final List<Interval> processingTimes = new ArrayList<>();
		/** Simulated processing time (in ms) per request. */
		private final long processingTimePerRequest;

		public ProcessTimeRegistratingCloudAdapter(long processingTimePerRequest) {
			this.processingTimePerRequest = processingTimePerRequest;
		}

		@Override
		public Optional<JsonObject> getConfigurationSchema() {
			return Optional.absent();
		}

		@Override
		public void configure(JsonObject configuration)
				throws IllegalArgumentException, CloudAdapterException {
		}

		@Override
		public Optional<JsonObject> getConfiguration() {
			return Optional.absent();
		}

		@Override
		public MachinePool getMachinePool() throws CloudAdapterException {
			return MachinePool.emptyPool(UtcTime.now());
		}

		@Override
		public void resizeMachinePool(int desiredCapacity) throws IllegalArgumentException,
				CloudAdapterException {
			DateTime startTime = UtcTime.now();
			try {
				// processing request ...
				Uninterruptibles.sleepUninterruptibly(
						this.processingTimePerRequest, TimeUnit.MILLISECONDS);
			} finally {
				DateTime endTime = UtcTime.now();
				this.processingTimes.add(new Interval(startTime, endTime));
			}
		}

		/**
		 * Returns the processing time periods of all handled resize requests
		 * (from reception to completion).
		 * 
		 * @return
		 */
		public List<Interval> getProcessingTimes() {
			return this.processingTimes;
		}
	}

	/**
	 * A task that makes a request to
	 * {@link PoolHandler#resizePool(PoolResizeRequest)}.
	 * 
	 * 
	 * 
	 */
	private static class PoolResizeRequester implements Callable<Void> {

		private final PoolHandler poolHandler;
		private final PoolResizeRequest resizeRequest;

		public PoolResizeRequester(PoolHandler poolHandler,
				PoolResizeRequest resizeRequest) {
			this.poolHandler = poolHandler;
			this.resizeRequest = resizeRequest;
		}

		@Override
		public Void call() throws Exception {
			this.poolHandler.resizePool(this.resizeRequest);
			return null;
		}
	}

}
