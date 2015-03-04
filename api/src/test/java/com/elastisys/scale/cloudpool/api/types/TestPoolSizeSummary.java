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
		int outOfService = 2;
		PoolSizeSummary summary = new PoolSizeSummary(desiredSize, allocated,
				outOfService);
		assertThat(summary.getDesiredSize(), is(desiredSize));
		assertThat(summary.getAllocated(), is(allocated));
		assertThat(summary.getOutOfService(), is(outOfService));
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
	public void outOfServiceMachinesCannotOutnumberAllocated() {
		int desiredSize = 4;
		int allocated = 3;
		int outOfService = 4;
		new PoolSizeSummary(desiredSize, allocated, outOfService);
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
	public void negativeOutOfService() {
		new PoolSizeSummary(2, 1, -1);
	}

}
