package com.elastisys.scale.cloudadapters.splitter.cloudadapter.poolcalculators;

import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.PrioritizedRemoteCloudAdapter;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.client.config.PrioritizedRemoteCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.splitter.cloudadapter.testutils.MockPrioritizedRemoteCloudAdapter;
import com.google.common.collect.ImmutableList;

public class TestStrictPoolSizeCalculationStrategy {

	private final StrictPoolSizeCalculationStrategy calculator = StrictPoolSizeCalculationStrategy.INSTANCE;
	private final static Logger log = LoggerFactory
			.getLogger(TestStrictPoolSizeCalculationStrategy.class);

	/**
	 * Check that the calculation strategy seems to work correctly for some
	 * trivial cases.
	 */
	@Test
	public void testTriviality() {
		ImmutableList<PrioritizedRemoteCloudAdapter> adapters = createAdapters(
				10, 10, 20, 20, 40);
		Map<PrioritizedRemoteCloudAdapter, MachinePool> initialMachinePools = mapAdaptersToPools(
				adapters, 0, 0, 0, 0, 0);

		Map<PrioritizedRemoteCloudAdapter, Long> calculatedPoolSizes;

		calculatedPoolSizes = this.calculator.calculatePoolSizes(
				initialMachinePools, adapters, 0);
		assertTotalSize(calculatedPoolSizes, 0);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 0, 0, 0);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(
				initialMachinePools, adapters, 1);
		assertTotalSize(calculatedPoolSizes, 1);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 0, 0, 1);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(
				initialMachinePools, adapters, 2);
		assertTotalSize(calculatedPoolSizes, 2);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 1, 0, 1);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(
				initialMachinePools, adapters, 3);
		assertTotalSize(calculatedPoolSizes, 3);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 1, 0, 2);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(
				initialMachinePools, adapters, 4);
		assertTotalSize(calculatedPoolSizes, 4);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 1, 1, 2);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(
				initialMachinePools, adapters, 5);
		assertTotalSize(calculatedPoolSizes, 5);
		assertPoolSizes(adapters, calculatedPoolSizes, 1, 0, 1, 1, 2);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(
				initialMachinePools, adapters, 6);
		assertTotalSize(calculatedPoolSizes, 6);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 2, 1, 3);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(
				initialMachinePools, adapters, 7);
		assertTotalSize(calculatedPoolSizes, 7);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 2, 2, 3);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(
				initialMachinePools, adapters, 8);
		assertTotalSize(calculatedPoolSizes, 8);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 2, 2, 4);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(
				initialMachinePools, adapters, 9);
		assertTotalSize(calculatedPoolSizes, 9);
		assertPoolSizes(adapters, calculatedPoolSizes, 1, 0, 2, 2, 4);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(
				initialMachinePools, adapters, 10);
		assertTotalSize(calculatedPoolSizes, 10);
		assertPoolSizes(adapters, calculatedPoolSizes, 1, 1, 2, 2, 4);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(
				initialMachinePools, adapters, 11);
		assertTotalSize(calculatedPoolSizes, 11);
		assertPoolSizes(adapters, calculatedPoolSizes, 0, 0, 3, 3, 5);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(
				initialMachinePools, adapters, 12);
		assertTotalSize(calculatedPoolSizes, 12);
		assertPoolSizes(adapters, calculatedPoolSizes, 1, 0, 3, 3, 5);

		calculatedPoolSizes = this.calculator.calculatePoolSizes(
				initialMachinePools, adapters, 13);
		assertTotalSize(calculatedPoolSizes, 13);
		assertPoolSizes(adapters, calculatedPoolSizes, 1, 0, 3, 3, 6);
	}

	public static void assertPoolSizes(
			ImmutableList<PrioritizedRemoteCloudAdapter> adapters,
			Map<PrioritizedRemoteCloudAdapter, Long> adaptersAndPoolSizes,
			long... expectedPoolSizes) {

		checkArgument(adapters.size() == expectedPoolSizes.length,
				"Incorrect number of expected pool sizes specified!");

		for (int i = 0; i < adapters.size(); i++) {
			assertThat(
					"Expected " + expectedPoolSizes[i] + " for adapter "
							+ adapters.get(i) + " in "
							+ adaptersAndPoolSizes.toString(),
							adaptersAndPoolSizes.get(adapters.get(i)),
							is(expectedPoolSizes[i]));
		}
	}

	public static void assertTotalSize(
			Map<PrioritizedRemoteCloudAdapter, Long> adaptersAndPoolSizes,
			final int correctSize) {
		int sum = 0;

		for (Long poolSize : adaptersAndPoolSizes.values()) {
			sum += poolSize;
		}

		assertEquals(correctSize, sum);
	}

	public static ImmutableList<PrioritizedRemoteCloudAdapter> createAdapters(
			long... priorities) {
		List<PrioritizedRemoteCloudAdapter> adapters = new ArrayList<PrioritizedRemoteCloudAdapter>(
				priorities.length);
		for (long priority : priorities) {
			adapters.add(createMockPrioritizedRemoteCloudAdapter((int) priority));
		}
		return ImmutableList.copyOf(adapters);
	}

	private static MockPrioritizedRemoteCloudAdapter createMockPrioritizedRemoteCloudAdapter(
			int priority, MachinePool machinePool) {
		PrioritizedRemoteCloudAdapterConfig config = new PrioritizedRemoteCloudAdapterConfig(
				"localhost", 1337, priority, null, null);
		return new MockPrioritizedRemoteCloudAdapter(config, machinePool);
	}

	private static MockPrioritizedRemoteCloudAdapter createMockPrioritizedRemoteCloudAdapter(
			int priority) {
		return createMockPrioritizedRemoteCloudAdapter(priority,
				new MachinePool(Collections.<Machine> emptyList(),
						new DateTime()));
	}

	public static Map<PrioritizedRemoteCloudAdapter, MachinePool> mapAdaptersToPools(
			ImmutableList<PrioritizedRemoteCloudAdapter> adapters,
			long... poolSizes) {
		checkArgument(adapters.size() == poolSizes.length,
				"Incorrect number of pool sizes supplied!");

		Map<PrioritizedRemoteCloudAdapter, MachinePool> adaptersAndPools = new HashMap<PrioritizedRemoteCloudAdapter, MachinePool>();

		int i = 0;
		for (PrioritizedRemoteCloudAdapter adapter : adapters) {

			((MockPrioritizedRemoteCloudAdapter) adapter)
			.setMachinePool(MockPrioritizedRemoteCloudAdapter
					.generateMachinePool(poolSizes[i++]));
		}

		return adaptersAndPools;
	}

	public static Map<PrioritizedRemoteCloudAdapter, MachinePool> createAdaptersAndPools(
			int[] priorities, long[] poolSizes) {
		checkArgument(priorities.length == poolSizes.length,
				"Incomplete mapping of priorities and pool sizes");

		Map<PrioritizedRemoteCloudAdapter, MachinePool> adaptersAndPools = new HashMap<PrioritizedRemoteCloudAdapter, MachinePool>();
		for (int i = 0; i < poolSizes.length; i++) {
			final MachinePool machinePool = MockPrioritizedRemoteCloudAdapter
					.generateMachinePool(poolSizes[i]);
			adaptersAndPools.put(
					createMockPrioritizedRemoteCloudAdapter(priorities[i],
							machinePool), machinePool);
		}
		return adaptersAndPools;
	}

}
