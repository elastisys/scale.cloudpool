package com.elastisys.scale.cloudadapters.splitter.poolcalculators;

import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.elastisys.scale.cloudadapters.splitter.config.PrioritizedCloudAdapter;
import com.google.common.collect.ImmutableList;

/**
 * Exercises {@link StrictPoolSizeCalculationStrategy}.
 *
 */
public class TestStrictPoolSizeCalculationStrategy {

	private final StrictPoolSizeCalculationStrategy calculator = StrictPoolSizeCalculationStrategy.INSTANCE;

	/**
	 * Nothing to be done for an empty set of back-end adapters.
	 */
	@Test
	public void onEmptySetOfAdapters() {
		List<PrioritizedCloudAdapter> adapters = createAdapters();
		Map<PrioritizedCloudAdapter, Integer> calculatedPoolSizes = this.calculator
				.calculatePoolSizes(adapters, 0);
		assertTrue(calculatedPoolSizes.isEmpty());
	}

	/**
	 * Trivial mapping for a single back-end cloud pool.
	 */
	@Test
	public void onSingleAdapters() {
		List<PrioritizedCloudAdapter> adapters = createAdapters(100);
		Map<PrioritizedCloudAdapter, Integer> calculatedPoolSizes = this.calculator
				.calculatePoolSizes(adapters, 0);
		assertTotalSize(calculatedPoolSizes, 0);
		assertPoolSizes(adapters, calculatedPoolSizes, 0);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 1);
		assertTotalSize(calculatedPoolSizes, 1);
		assertPoolSizes(adapters, calculatedPoolSizes, 1);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 4);
		assertTotalSize(calculatedPoolSizes, 4);
		assertPoolSizes(adapters, calculatedPoolSizes, 4);
	}

	/**
	 * Calculate pool sizes for two back-end cloud pools.
	 */
	@Test
	public void onTwoAdaptersSplit50_50() {
		// two adapters with 50-50 split
		List<PrioritizedCloudAdapter> adapters = createAdapters(50, 50);
		Map<PrioritizedCloudAdapter, Integer> calculatedPoolSizes = this.calculator
				.calculatePoolSizes(adapters, 0);
		assertTotalSize(calculatedPoolSizes, 0);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 1);
		assertTotalSize(calculatedPoolSizes, 1);
		assertPoolSizes(adapters, calculatedPoolSizes, 1, 0);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 2);
		assertTotalSize(calculatedPoolSizes, 2);
		assertPoolSizes(adapters, calculatedPoolSizes, 1, 1);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 3);
		assertTotalSize(calculatedPoolSizes, 3);
		assertPoolSizes(adapters, calculatedPoolSizes, 2, 1);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 4);
		assertTotalSize(calculatedPoolSizes, 4);
		assertPoolSizes(adapters, calculatedPoolSizes, 2, 2);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 10);
		assertTotalSize(calculatedPoolSizes, 10);
		assertPoolSizes(adapters, calculatedPoolSizes, 5, 5);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 100);
		assertTotalSize(calculatedPoolSizes, 100);
		assertPoolSizes(adapters, calculatedPoolSizes, 50, 50);
	}

	/**
	 * Calculate pool sizes for two back-end cloud pools with 70-30
	 * distribution.
	 */
	@Test
	public void onTwoAdaptersSplit30_70() {
		// two adapters with 30-70 split
		List<PrioritizedCloudAdapter> adapters = createAdapters(30, 70);
		Map<PrioritizedCloudAdapter, Integer> calculatedPoolSizes = this.calculator
				.calculatePoolSizes(adapters, 0);
		assertTotalSize(calculatedPoolSizes, 0);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 1);
		assertTotalSize(calculatedPoolSizes, 1);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 1);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 2);
		assertTotalSize(calculatedPoolSizes, 2);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 2);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 3);
		assertTotalSize(calculatedPoolSizes, 3);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 3);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 4);
		assertTotalSize(calculatedPoolSizes, 4);
		assertPoolSizes(adapters, calculatedPoolSizes, 1, 3);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 5);
		assertTotalSize(calculatedPoolSizes, 5);
		assertPoolSizes(adapters, calculatedPoolSizes, 1, 4);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 6);
		assertTotalSize(calculatedPoolSizes, 6);
		assertPoolSizes(adapters, calculatedPoolSizes, 1, 5);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 10);
		assertTotalSize(calculatedPoolSizes, 10);
		assertPoolSizes(adapters, calculatedPoolSizes, 3, 7);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 100);
		assertTotalSize(calculatedPoolSizes, 100);
		assertPoolSizes(adapters, calculatedPoolSizes, 30, 70);
	}

	/**
	 * Check that the calculation strategy seems to work correctly for some
	 * trivial cases.
	 */
	@Test
	public void testTriviality() {
		List<PrioritizedCloudAdapter> adapters = createAdapters(10, 10, 20, 20,
				40);

		Map<PrioritizedCloudAdapter, Integer> calculatedPoolSizes = this.calculator
				.calculatePoolSizes(adapters, 0);
		assertTotalSize(calculatedPoolSizes, 0);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 0, 0, 0);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 1);
		assertTotalSize(calculatedPoolSizes, 1);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 0, 0, 1);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 2);
		assertTotalSize(calculatedPoolSizes, 2);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 1, 0, 1);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 3);
		assertTotalSize(calculatedPoolSizes, 3);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 1, 0, 2);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 4);
		assertTotalSize(calculatedPoolSizes, 4);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 1, 1, 2);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 5);
		assertTotalSize(calculatedPoolSizes, 5);
		assertPoolSizes(adapters, calculatedPoolSizes, 1, 0, 1, 1, 2);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 6);
		assertTotalSize(calculatedPoolSizes, 6);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 2, 1, 3);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 7);
		assertTotalSize(calculatedPoolSizes, 7);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 2, 2, 3);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 8);
		assertTotalSize(calculatedPoolSizes, 8);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 2, 2, 4);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 9);
		assertTotalSize(calculatedPoolSizes, 9);
		assertPoolSizes(adapters, calculatedPoolSizes, 1, 0, 2, 2, 4);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 10);
		assertTotalSize(calculatedPoolSizes, 10);
		assertPoolSizes(adapters, calculatedPoolSizes, 1, 1, 2, 2, 4);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 11);
		assertTotalSize(calculatedPoolSizes, 11);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 3, 3, 5);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 12);
		assertTotalSize(calculatedPoolSizes, 12);
		assertPoolSizes(adapters, calculatedPoolSizes, 1, 0, 3, 3, 5);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 13);
		assertTotalSize(calculatedPoolSizes, 13);
		assertPoolSizes(adapters, calculatedPoolSizes, 1, 0, 3, 3, 6);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(adapters, 100);
		assertTotalSize(calculatedPoolSizes, 100);
		assertPoolSizes(adapters, calculatedPoolSizes, 10, 10, 20, 20, 40);

	}

	/**
	 * Verifies that the calculated pool sizes for a list of back-end cloud
	 * adapters match expectations.
	 *
	 * @param adapters
	 *            The list of adapters for which pool size has been calculated.
	 * @param calculatedPoolSizes
	 *            The calculated pool size for each adapter.
	 * @param expectedPoolSizes
	 *            The expected pool sizes for each adapter given in the same
	 *            order as {@code adapters}.
	 */
	public static void assertPoolSizes(List<PrioritizedCloudAdapter> adapters,
			Map<PrioritizedCloudAdapter, Integer> calculatedPoolSizes,
			int... expectedPoolSizes) {

		checkArgument(adapters.size() == expectedPoolSizes.length,
				"Incorrect number of expected pool sizes specified!");

		for (int i = 0; i < adapters.size(); i++) {
			assertThat("Expected " + expectedPoolSizes[i] + " for adapter "
					+ adapters.get(i),
					calculatedPoolSizes.get(adapters.get(i)),
					is(expectedPoolSizes[i]));
		}
	}

	public static void assertTotalSize(
			Map<PrioritizedCloudAdapter, Integer> adapterPoolSizes,
			final int correctSize) {
		int sum = 0;
		for (Integer poolSize : adapterPoolSizes.values()) {
			sum += poolSize;
		}
		assertEquals(correctSize, sum);
	}

	public static List<PrioritizedCloudAdapter> createAdapters(
			int... priorities) {
		List<PrioritizedCloudAdapter> adapters = new ArrayList<PrioritizedCloudAdapter>(
				priorities.length);
		int index = 0;
		for (int priority : priorities) {
			index++;
			adapters.add(new PrioritizedCloudAdapter(priority,
					"cloudAdapterHost" + index, 1234, null, null));
		}
		return ImmutableList.copyOf(adapters);
	}
}
