package com.elastisys.scale.cloudpool.api.types;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Tests that exercise {@link PoolSizeSummary}.
 */
public class TestPoolSizeSummary {

	@Test
	public void basicSanity() {
		int desiredSize = 4;
		int allocated = 3;
		int active = 2;
		PoolSizeSummary summary = new PoolSizeSummary(desiredSize, allocated,
				active);
		assertThat(summary.getDesiredSize(), is(desiredSize));
		assertThat(summary.getAllocated(), is(allocated));
		assertThat(summary.getActive(), is(active));
	}

	@Test
	public void equality() {
		assertThat(new PoolSizeSummary(4, 3, 2),
				is(new PoolSizeSummary(4, 3, 2)));

		assertThat(new PoolSizeSummary(4, 3, 2), is(not(new PoolSizeSummary(3,
				3, 2))));
		assertThat(new PoolSizeSummary(4, 3, 2), is(not(new PoolSizeSummary(4,
				2, 2))));
		assertThat(new PoolSizeSummary(4, 3, 2), is(not(new PoolSizeSummary(4,
				3, 1))));
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

}
