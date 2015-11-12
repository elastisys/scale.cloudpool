package com.elastisys.scale.cloudpool.api.types;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Tests that exercise {@link PoolSizeSummary}.
 */
public class TestPoolSizeSummary {

	@Before
	public void beforeTestMethod() {
		FrozenTime.setFixed(UtcTime.parse("2015-11-17T12:00:00.000Z"));
	}

	@Test
	public void basicSanity() {
		DateTime timestamp = UtcTime.parse("2015-01-01T12:00:00.000Z");
		int desiredSize = 4;
		int allocated = 3;
		int active = 2;
		PoolSizeSummary summary = new PoolSizeSummary(timestamp, desiredSize,
				allocated, active);
		assertThat(summary.getTimestamp(), is(timestamp));
		assertThat(summary.getDesiredSize(), is(desiredSize));
		assertThat(summary.getAllocated(), is(allocated));
		assertThat(summary.getActive(), is(active));

		// with default timestamp
		summary = new PoolSizeSummary(desiredSize, allocated, active);
		assertThat(summary.getTimestamp(), is(UtcTime.now()));
		assertThat(summary.getDesiredSize(), is(desiredSize));
		assertThat(summary.getAllocated(), is(allocated));
		assertThat(summary.getActive(), is(active));
	}

	@Test
	public void equality() {
		assertThat(new PoolSizeSummary(4, 3, 2),
				is(new PoolSizeSummary(4, 3, 2)));

		assertThat(new PoolSizeSummary(4, 3, 2),
				is(not(new PoolSizeSummary(3, 3, 2))));
		assertThat(new PoolSizeSummary(4, 3, 2),
				is(not(new PoolSizeSummary(4, 2, 2))));
		assertThat(new PoolSizeSummary(4, 3, 2),
				is(not(new PoolSizeSummary(4, 3, 1))));
	}

	@Test(expected = IllegalArgumentException.class)
	public void activeMachinesCannotOutnumberAllocated() {
		int desiredSize = 4;
		int allocated = 3;
		int active = 4;
		new PoolSizeSummary(desiredSize, allocated, active);
	}

	@Test(expected = IllegalArgumentException.class)
	public void negativeDesiredSize() {
		new PoolSizeSummary(-1, 1, 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void negativeAllocated() {
		new PoolSizeSummary(2, -1, 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void negativeActive() {
		new PoolSizeSummary(2, 1, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullTimestamp() {
		new PoolSizeSummary(null, 2, 1, 1);
	}

}
