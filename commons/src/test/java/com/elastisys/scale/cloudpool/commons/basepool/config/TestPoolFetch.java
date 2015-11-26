package com.elastisys.scale.cloudpool.commons.basepool.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Exercises {@link PoolFetchConfig}.
 */
public class TestPoolFetch {

	@Test
	public void basicSanity() {
		RetriesConfig retries = new RetriesConfig(5,
				new TimeInterval(2L, TimeUnit.SECONDS));
		TimeInterval refreshInterval = new TimeInterval(30L, TimeUnit.SECONDS);
		TimeInterval reachabilityTimeout = new TimeInterval(10L,
				TimeUnit.MINUTES);
		PoolFetchConfig config = new PoolFetchConfig(retries, refreshInterval,
				reachabilityTimeout);

		config.validate();

		assertThat(config.getRetries(), is(
				new RetriesConfig(5, new TimeInterval(2L, TimeUnit.SECONDS))));
		assertThat(config.getRefreshInterval(),
				is(new TimeInterval(30L, TimeUnit.SECONDS)));
		assertThat(config.getReachabilityTimeout(),
				is(new TimeInterval(10L, TimeUnit.MINUTES)));
	}

	/**
	 * Retries is required.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void missingRetries() {
		TimeInterval refreshInterval = new TimeInterval(30L, TimeUnit.SECONDS);
		TimeInterval reachabilityTimeout = new TimeInterval(10L,
				TimeUnit.MINUTES);
		new PoolFetchConfig(null, refreshInterval, reachabilityTimeout)
				.validate();
	}

	/**
	 * Refresh interval is required.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void missingRefreshInterval() {
		RetriesConfig retries = new RetriesConfig(5,
				new TimeInterval(2L, TimeUnit.SECONDS));
		TimeInterval reachabilityTimeout = new TimeInterval(10L,
				TimeUnit.MINUTES);
		new PoolFetchConfig(retries, null, reachabilityTimeout).validate();
	}

	/**
	 * Reachability timeout is required.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void missingReachabilityTimeout() {
		RetriesConfig retries = new RetriesConfig(5,
				new TimeInterval(2L, TimeUnit.SECONDS));
		TimeInterval refreshInterval = new TimeInterval(30L, TimeUnit.SECONDS);
		new PoolFetchConfig(retries, refreshInterval, null).validate();
	}

	/**
	 * reachabilityTimeout must be longer than refreshInterval
	 */
	@Test(expected = IllegalArgumentException.class)
	public void reachabilityTimeoutShorterThanRefreshInterval() {
		RetriesConfig retries = new RetriesConfig(5,
				new TimeInterval(2L, TimeUnit.SECONDS));

		TimeInterval refreshInterval = new TimeInterval(30L, TimeUnit.SECONDS);
		// reachability timout shorter than refresh interval
		TimeInterval reachabilityTimeout = new TimeInterval(20L,
				TimeUnit.SECONDS);

		new PoolFetchConfig(retries, refreshInterval, reachabilityTimeout)
				.validate();
	}
}
