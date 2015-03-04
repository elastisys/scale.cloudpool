package com.elastisys.scale.cloudpool.splitter.poolcalculators;

import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.elastisys.scale.cloudpool.splitter.config.PrioritizedCloudPool;
import com.google.common.collect.ImmutableList;

/**
 * Exercises {@link StrictPoolSizeCalculationStrategy}.
 */
public class TestStrictPoolSizeCalculationStrategy {

	private final StrictPoolSizeCalculationStrategy calculator = StrictPoolSizeCalculationStrategy.INSTANCE;

	/**
	 * Nothing to be done for an empty set of back-end pools.
	 */
	@Test
	public void onEmptySetOfBackendPools() {
		List<PrioritizedCloudPool> backendPools = createBackendPools();
		Map<PrioritizedCloudPool, Integer> calculatedPoolSizes = this.calculator
				.calculatePoolSizes(backendPools, 0);
		assertTrue(calculatedPoolSizes.isEmpty());
	}

	/**
	 * Trivial mapping for a single back-end cloud pool.
	 */
	@Test
	public void onSingleBackendPool() {
		List<PrioritizedCloudPool> backendPools = createBackendPools(100);
		Map<PrioritizedCloudPool, Integer> calculatedPoolSizes = this.calculator
				.calculatePoolSizes(backendPools, 0);
		assertTotalSize(calculatedPoolSizes, 0);
		assertPoolSizes(backendPools, calculatedPoolSizes, 0);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				1);
		assertTotalSize(calculatedPoolSizes, 1);
		assertPoolSizes(backendPools, calculatedPoolSizes, 1);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				4);
		assertTotalSize(calculatedPoolSizes, 4);
		assertPoolSizes(backendPools, calculatedPoolSizes, 4);
	}

	/**
	 * Calculate pool sizes for two back-end cloud pools.
	 */
	@Test
	public void onTwoBackendPoolsSplit50_50() {
		// two pools with 50-50 split
		List<PrioritizedCloudPool> backendPools = createBackendPools(50, 50);
		Map<PrioritizedCloudPool, Integer> calculatedPoolSizes = this.calculator
				.calculatePoolSizes(backendPools, 0);
		assertTotalSize(calculatedPoolSizes, 0);
		assertPoolSizes(backendPools, calculatedPoolSizes, 0, 0);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				1);
		assertTotalSize(calculatedPoolSizes, 1);
		assertPoolSizes(backendPools, calculatedPoolSizes, 1, 0);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				2);
		assertTotalSize(calculatedPoolSizes, 2);
		assertPoolSizes(backendPools, calculatedPoolSizes, 1, 1);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				3);
		assertTotalSize(calculatedPoolSizes, 3);
		assertPoolSizes(backendPools, calculatedPoolSizes, 2, 1);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				4);
		assertTotalSize(calculatedPoolSizes, 4);
		assertPoolSizes(backendPools, calculatedPoolSizes, 2, 2);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				10);
		assertTotalSize(calculatedPoolSizes, 10);
		assertPoolSizes(backendPools, calculatedPoolSizes, 5, 5);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				100);
		assertTotalSize(calculatedPoolSizes, 100);
		assertPoolSizes(backendPools, calculatedPoolSizes, 50, 50);
	}

	/**
	 * Calculate pool sizes for two back-end cloud pools with 70-30
	 * distribution.
	 */
	@Test
	public void onTwoBackendPoolsSplit30_70() {
		// two backend pools with 30-70 split
		List<PrioritizedCloudPool> backendPools = createBackendPools(30, 70);
		Map<PrioritizedCloudPool, Integer> calculatedPoolSizes = this.calculator
				.calculatePoolSizes(backendPools, 0);
		assertTotalSize(calculatedPoolSizes, 0);
		assertPoolSizes(backendPools, calculatedPoolSizes, 0, 0);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				1);
		assertTotalSize(calculatedPoolSizes, 1);
		assertPoolSizes(backendPools, calculatedPoolSizes, 0, 1);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				2);
		assertTotalSize(calculatedPoolSizes, 2);
		assertPoolSizes(backendPools, calculatedPoolSizes, 0, 2);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				3);
		assertTotalSize(calculatedPoolSizes, 3);
		assertPoolSizes(backendPools, calculatedPoolSizes, 0, 3);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				4);
		assertTotalSize(calculatedPoolSizes, 4);
		assertPoolSizes(backendPools, calculatedPoolSizes, 1, 3);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				5);
		assertTotalSize(calculatedPoolSizes, 5);
		assertPoolSizes(backendPools, calculatedPoolSizes, 1, 4);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				6);
		assertTotalSize(calculatedPoolSizes, 6);
		assertPoolSizes(backendPools, calculatedPoolSizes, 1, 5);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				10);
		assertTotalSize(calculatedPoolSizes, 10);
		assertPoolSizes(backendPools, calculatedPoolSizes, 3, 7);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(backendPools,
				100);
		assertTotalSize(calculatedPoolSizes, 100);
		assertPoolSizes(backendPools, calculatedPoolSizes, 30, 70);
	}

	/**
	 * Check that the calculation strategy seems to work correctly for some
	 * trivial cases.
	 */
	@Test
	public void testTriviality() {
		List<PrioritizedCloudPool> pools = createBackendPools(10, 10, 20, 20,
				40);

		Map<PrioritizedCloudPool, Integer> calculatedPoolSizes = this.calculator
				.calculatePoolSizes(pools, 0);
		assertTotalSize(calculatedPoolSizes, 0);
		assertPoolSizes(pools, calculatedPoolSizes, 0, 0, 0, 0, 0);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(pools, 1);
		assertTotalSize(calculatedPoolSizes, 1);
		assertPoolSizes(pools, calculatedPoolSizes, 0, 0, 0, 0, 1);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(pools, 2);
		assertTotalSize(calculatedPoolSizes, 2);
		assertPoolSizes(pools, calculatedPoolSizes, 0, 0, 1, 0, 1);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(pools, 3);
		assertTotalSize(calculatedPoolSizes, 3);
		assertPoolSizes(pools, calculatedPoolSizes, 0, 0, 1, 0, 2);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(pools, 4);
		assertTotalSize(calculatedPoolSizes, 4);
		assertPoolSizes(pools, calculatedPoolSizes, 0, 0, 1, 1, 2);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(pools, 5);
		assertTotalSize(calculatedPoolSizes, 5);
		assertPoolSizes(pools, calculatedPoolSizes, 1, 0, 1, 1, 2);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(pools, 6);
		assertTotalSize(calculatedPoolSizes, 6);
		assertPoolSizes(pools, calculatedPoolSizes, 0, 0, 2, 1, 3);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(pools, 7);
		assertTotalSize(calculatedPoolSizes, 7);
		assertPoolSizes(pools, calculatedPoolSizes, 0, 0, 2, 2, 3);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(pools, 8);
		assertTotalSize(calculatedPoolSizes, 8);
		assertPoolSizes(pools, calculatedPoolSizes, 0, 0, 2, 2, 4);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(pools, 9);
		assertTotalSize(calculatedPoolSizes, 9);
		assertPoolSizes(pools, calculatedPoolSizes, 1, 0, 2, 2, 4);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(pools, 10);
		assertTotalSize(calculatedPoolSizes, 10);
		assertPoolSizes(pools, calculatedPoolSizes, 1, 1, 2, 2, 4);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(pools, 11);
		assertTotalSize(calculatedPoolSizes, 11);
		assertPoolSizes(pools, calculatedPoolSizes, 0, 0, 3, 3, 5);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(pools, 12);
		assertTotalSize(calculatedPoolSizes, 12);
		assertPoolSizes(pools, calculatedPoolSizes, 1, 0, 3, 3, 5);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(pools, 13);
		assertTotalSize(calculatedPoolSizes, 13);
		assertPoolSizes(pools, calculatedPoolSizes, 1, 0, 3, 3, 6);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(pools, 100);
		assertTotalSize(calculatedPoolSizes, 100);
		assertPoolSizes(pools, calculatedPoolSizes, 10, 10, 20, 20, 40);

	}

	/**
	 * Verifies that the calculated pool sizes for a list of back-end cloud
	 * pools match expectations.
	 *
	 * @param backendPools
	 *            The list of pools for which pool size has been calculated.
	 * @param calculatedPoolSizes
	 *            The calculated pool size for each pool.
	 * @param expectedPoolSizes
	 *            The expected pool sizes for each pool given in the same order
	 *            as {@code backendPools}.
	 */
	public static void assertPoolSizes(List<PrioritizedCloudPool> backendPools,
			Map<PrioritizedCloudPool, Integer> calculatedPoolSizes,
			int... expectedPoolSizes) {

		checkArgument(backendPools.size() == expectedPoolSizes.length,
				"Incorrect number of expected pool sizes specified!");

		for (int i = 0; i < backendPools.size(); i++) {
			assertThat("Expected " + expectedPoolSizes[i]
					+ " for backend pool " + backendPools.get(i),
					calculatedPoolSizes.get(backendPools.get(i)),
					is(expectedPoolSizes[i]));
		}
	}

	public static void assertTotalSize(
			Map<PrioritizedCloudPool, Integer> backendPoolSizes,
			final int correctSize) {
		int sum = 0;
		for (Integer poolSize : backendPoolSizes.values()) {
			sum += poolSize;
		}
		assertEquals(correctSize, sum);
	}

	public static List<PrioritizedCloudPool> createBackendPools(
			int... priorities) {
		List<PrioritizedCloudPool> backendPools = new ArrayList<PrioritizedCloudPool>(
				priorities.length);
		int index = 0;
		for (int priority : priorities) {
			index++;
			backendPools.add(new PrioritizedCloudPool(priority, "cloudPoolHost"
					+ index, 1234, null, null));
		}
		return ImmutableList.copyOf(backendPools);
	}
}
