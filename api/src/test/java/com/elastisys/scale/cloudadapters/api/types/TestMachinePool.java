package com.elastisys.scale.cloudadapters.api.types;

import static com.elastisys.scale.cloudadapters.api.types.TestUtils.ips;
import static com.elastisys.scale.cloudadapters.api.types.TestUtils.machine;
import static com.elastisys.scale.cloudadapters.api.types.TestUtils.machineNoIp;
import static com.elastisys.scale.cloudadapters.api.types.TestUtils.pool;
import static com.elastisys.scale.commons.json.JsonUtils.parseJsonString;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;

import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachinePool;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapers.api.types.ServiceState;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link MachinePool} class.
 *
 *
 *
 */
public class TestMachinePool {

	/** Resource path containing Json representation of a machine pool. */
	private final static String emptyPoolJson = "json/empty-pool.json";
	/** Resource path containing Json representation of a machine pool. */
	private final static String singleMachinePoolJson = "json/pool1.json";
	/** Resource path containing Json representation of a machine pool. */
	private final static String multiMachinePoolJson = "json/pool2.json";

	@Test
	public void testGetTimestamp() {
		DateTime now = UtcTime.now();
		MachinePool pool = MachinePool.emptyPool(now);
		assertThat(pool.getTimestamp(), is(now));
	}

	/**
	 * Exercise {@link MachinePool#getMachines()}
	 */
	@Test
	public void testGetMachines() {
		DateTime now = UtcTime.now();

		// on empty pool
		MachinePool pool = MachinePool.emptyPool(now);
		assertThat(pool.getMachines().size(), is(0));

		// on a pool with a mix of machines in different states
		Machine requested1 = new Machine("i-1", MachineState.REQUESTED,
				ServiceState.UNKNOWN, null, null, null);
		Machine requested2 = new Machine("i-2", MachineState.REQUESTED,
				ServiceState.UNKNOWN, null, null, null);
		Machine pending1 = new Machine("i-3", MachineState.PENDING,
				ServiceState.BOOTING, now, null, null);
		Machine running1 = new Machine("i-4", MachineState.RUNNING,
				ServiceState.IN_SERVICE, now, asList("1.2.3.4"), null);
		Machine running2 = new Machine("i-5", MachineState.RUNNING,
				ServiceState.UNHEALTHY, now, asList("1.2.3.4"), null);
		Machine running3 = new Machine("i-5.1", MachineState.RUNNING,
				ServiceState.OUT_OF_SERVICE, now, asList("1.2.3.5"), null);
		Machine terminating = new Machine("i-6", MachineState.TERMINATING,
				ServiceState.UNKNOWN, now, null, null);
		Machine terminated1 = new Machine("i-7", MachineState.TERMINATED,
				ServiceState.UNKNOWN, now, null, null);
		Machine terminated2 = new Machine("i-8", MachineState.TERMINATED,
				ServiceState.UNKNOWN, now, null, null);
		pool = new MachinePool(Arrays.asList(requested1, requested2, pending1,
				running1, running2, running3, terminating, terminated1,
				terminated2), now);
		// verify machines
		assertThat(pool.getMachines().size(), is(9));
		assertThat(
				pool.getMachines(),
				is(asList(requested1, requested2, pending1, running1, running2,
						running3, terminating, terminated1, terminated2)));
	}

	/**
	 * Exercise {@link MachinePool#getActiveMachines()}
	 */
	@Test
	public void testGetActiveMachines() {
		DateTime now = UtcTime.now();

		// on empty pool
		MachinePool pool = MachinePool.emptyPool(now);
		assertThat(pool.getActiveMachines().size(), is(0));

		// on a pool with a mix of machines in different states
		Machine requested1 = new Machine("i-1", MachineState.REQUESTED,
				ServiceState.UNKNOWN, null, null, null);
		Machine requested2 = new Machine("i-2", MachineState.REQUESTED,
				ServiceState.UNKNOWN, null, null, null);
		Machine pending1 = new Machine("i-3", MachineState.PENDING,
				ServiceState.BOOTING, now, null, null);
		Machine running1 = new Machine("i-4", MachineState.RUNNING,
				ServiceState.IN_SERVICE, now, asList("1.2.3.4"), null);
		Machine running2 = new Machine("i-5", MachineState.RUNNING,
				ServiceState.UNHEALTHY, now, asList("1.2.3.4"), null);
		Machine running3 = new Machine("i-5.1", MachineState.RUNNING,
				ServiceState.OUT_OF_SERVICE, now, asList("1.2.3.5"), null);
		Machine terminating = new Machine("i-6", MachineState.TERMINATING,
				ServiceState.UNKNOWN, now, null, null);
		Machine terminated1 = new Machine("i-7", MachineState.TERMINATED,
				ServiceState.UNKNOWN, now, null, null);
		Machine terminated2 = new Machine("i-8", MachineState.TERMINATED,
				ServiceState.UNKNOWN, now, null, null);
		pool = new MachinePool(Arrays.asList(requested1, requested2, pending1,
				running1, running2, running3, terminating, terminated1,
				terminated2), now);
		// verify active machines: 1 pending + 2 running that aren't
		// OUT_OF_SERVICE
		assertThat(pool.getActiveMachines().size(), is(3));
		assertThat(pool.getActiveMachines(),
				is(asList(pending1, running1, running2)));
	}

	/**
	 * Exercise {@link MachinePool#getAllocatedMachines()}
	 */
	@Test
	public void testGetAllocatedMachines() {
		DateTime now = UtcTime.now();

		// on empty pool
		MachinePool pool = MachinePool.emptyPool(now);
		assertThat(pool.getAllocatedMachines().size(), is(0));

		// on a pool with a mix of machines in different states
		Machine requested1 = new Machine("i-1", MachineState.REQUESTED,
				ServiceState.UNKNOWN, null, null, null);
		Machine requested2 = new Machine("i-2", MachineState.REQUESTED,
				ServiceState.UNKNOWN, null, null, null);
		Machine pending1 = new Machine("i-3", MachineState.PENDING,
				ServiceState.BOOTING, now, null, null);
		Machine running1 = new Machine("i-4", MachineState.RUNNING,
				ServiceState.IN_SERVICE, now, asList("1.2.3.4"), null);
		Machine running2 = new Machine("i-5", MachineState.RUNNING,
				ServiceState.UNHEALTHY, now, asList("1.2.3.4"), null);
		Machine running3 = new Machine("i-5.1", MachineState.RUNNING,
				ServiceState.OUT_OF_SERVICE, now, asList("1.2.3.5"), null);
		Machine terminating = new Machine("i-6", MachineState.TERMINATING,
				ServiceState.UNKNOWN, now, null, null);
		Machine terminated1 = new Machine("i-7", MachineState.TERMINATED,
				ServiceState.UNKNOWN, now, null, null);
		Machine terminated2 = new Machine("i-8", MachineState.TERMINATED,
				ServiceState.UNKNOWN, now, null, null);
		pool = new MachinePool(Arrays.asList(requested1, requested2, pending1,
				running1, running2, running3, terminating, terminated1,
				terminated2), now);
		// verify allocated machines: 2 requested + 1 pending + 3 running
		assertThat(pool.getAllocatedMachines().size(), is(6));
		assertThat(
				pool.getAllocatedMachines(),
				is(asList(requested1, requested2, pending1, running1, running2,
						running3)));
	}

	/**
	 * Exercise {@link MachinePool#getEffectiveMachines()}
	 */
	@Test
	public void testGetEffectiveMachines() {
		DateTime now = UtcTime.now();

		// on empty pool
		MachinePool pool = MachinePool.emptyPool(now);
		assertThat(pool.getEffectiveMachines().size(), is(0));

		// on a pool with a mix of machines in different states
		Machine requested1 = new Machine("i-1", MachineState.REQUESTED,
				ServiceState.UNKNOWN, null, null, null);
		Machine requested2 = new Machine("i-2", MachineState.REQUESTED,
				ServiceState.UNKNOWN, null, null, null);
		Machine pending1 = new Machine("i-3", MachineState.PENDING,
				ServiceState.BOOTING, now, null, null);
		Machine running1 = new Machine("i-4", MachineState.RUNNING,
				ServiceState.IN_SERVICE, now, asList("1.2.3.4"), null);
		Machine running2 = new Machine("i-5", MachineState.RUNNING,
				ServiceState.UNHEALTHY, now, asList("1.2.3.4"), null);
		Machine running3 = new Machine("i-5.1", MachineState.RUNNING,
				ServiceState.OUT_OF_SERVICE, now, asList("1.2.3.5"), null);
		Machine terminating = new Machine("i-6", MachineState.TERMINATING,
				ServiceState.UNKNOWN, now, null, null);
		Machine terminated1 = new Machine("i-7", MachineState.TERMINATED,
				ServiceState.UNKNOWN, now, null, null);
		Machine terminated2 = new Machine("i-8", MachineState.TERMINATED,
				ServiceState.UNKNOWN, now, null, null);
		pool = new MachinePool(Arrays.asList(requested1, requested2, pending1,
				running1, running2, running3, terminating, terminated1,
				terminated2), now);
		// verify effective machines: 2 requested + 1 pending + 2 running that
		// aren't OUT_OF_SERVICE
		assertThat(pool.getEffectiveMachines().size(), is(5));
		assertThat(
				pool.getEffectiveMachines(),
				is(asList(requested1, requested2, pending1, running1, running2)));
	}

	/**
	 * Exercise {@link MachinePool#getOutOfServiceMachines()}
	 */
	@Test
	public void testGetOutOfServiceMachines() {
		DateTime now = UtcTime.now();

		// on empty pool
		MachinePool pool = MachinePool.emptyPool(now);
		assertThat(pool.getOutOfServiceMachines().size(), is(0));

		// on a pool with a mix of machines in different states
		Machine requested1 = new Machine("i-1", MachineState.REQUESTED,
				ServiceState.UNKNOWN, null, null, null);
		Machine requested2 = new Machine("i-2", MachineState.REQUESTED,
				ServiceState.UNKNOWN, null, null, null);
		Machine pending1 = new Machine("i-3", MachineState.PENDING,
				ServiceState.BOOTING, now, null, null);
		Machine running1 = new Machine("i-4", MachineState.RUNNING,
				ServiceState.IN_SERVICE, now, asList("1.2.3.4"), null);
		Machine running2 = new Machine("i-5", MachineState.RUNNING,
				ServiceState.UNHEALTHY, now, asList("1.2.3.4"), null);
		Machine running3 = new Machine("i-5.1", MachineState.RUNNING,
				ServiceState.OUT_OF_SERVICE, now, asList("1.2.3.5"), null);
		Machine terminating = new Machine("i-6", MachineState.TERMINATING,
				ServiceState.UNKNOWN, now, null, null);
		Machine terminated1 = new Machine("i-7", MachineState.TERMINATED,
				ServiceState.UNKNOWN, now, null, null);
		Machine terminated2 = new Machine("i-8", MachineState.TERMINATED,
				ServiceState.UNKNOWN, now, null, null);
		pool = new MachinePool(Arrays.asList(requested1, requested2, pending1,
				running1, running2, running3, terminating, terminated1,
				terminated2), now);
		// verify out-of-service machines: 1 running
		assertThat(pool.getOutOfServiceMachines().size(), is(1));
		assertThat(pool.getOutOfServiceMachines(), is(asList(running3)));
	}

	/**
	 * Test equality comparisons.
	 */
	@Test
	public void testEquality() throws IOException {
		MachinePool empty1 = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"));
		MachinePool empty1Clone = pool(DateTime
				.parse("2014-01-13T12:00:00.000Z"));
		// different timestamp
		MachinePool empty2 = pool(UtcTime.parse("2014-01-14T15:00:00.000Z"));

		// single-member pool
		MachinePool singlePool = pool(
				UtcTime.parse("2014-01-13T12:00:00.000Z"),
				machineNoIp("m1", MachineState.PENDING,
						UtcTime.parse("2014-01-13T11:00:00.000Z")));
		MachinePool singlePoolClone = pool(
				UtcTime.parse("2014-01-13T12:00:00.000Z"),
				machineNoIp("m1", MachineState.PENDING,
						UtcTime.parse("2014-01-13T11:00:00.000Z")));

		// multi-member pool
		MachinePool multiPool = pool(
				UtcTime.parse("2014-01-13T12:00:00.000Z"),
				machineNoIp("m1", MachineState.RUNNING,
						UtcTime.parse("2014-01-13T11:00:00.000Z")),
				machineNoIp("m2", MachineState.REQUESTED, null));
		MachinePool multiPoolClone = pool(
				UtcTime.parse("2014-01-13T12:00:00.000Z"),
				machineNoIp("m1", MachineState.RUNNING,
						UtcTime.parse("2014-01-13T11:00:00.000Z")),
				machineNoIp("m2", MachineState.REQUESTED, null));

		assertThat(empty1, is(empty1Clone));
		assertThat(empty1, is(not(empty2)));

		assertThat(singlePool, is(singlePoolClone));
		assertThat(singlePool, is(not(empty1)));
		assertThat(singlePool, is(not(multiPool)));

		assertThat(multiPool, is(multiPoolClone));
		assertThat(multiPool, is(not(empty1)));
		assertThat(multiPool, is(not(singlePool)));

		// a bit far-fetched but test with null timestamps (*could* occur if a
		// bad machine pool was parsed from json)
		MachinePool noTimestamp = JsonUtils.toObject(JsonUtils
				.parseJsonResource("json/invalidpool-missing-timestamp.json"),
				MachinePool.class);
		MachinePool noTimestampClone = JsonUtils.toObject(JsonUtils
				.parseJsonResource("json/invalidpool-missing-timestamp.json"),
				MachinePool.class);
		assertThat(noTimestamp, is(noTimestampClone));
		assertThat(empty1, is(not(noTimestamp)));
		assertThat(singlePool, is(not(noTimestamp)));
		assertThat(multiPool, is(not(noTimestamp)));
		assertThat(noTimestamp, is(not(multiPool)));
	}

	/**
	 * Test hashcode.
	 */
	@Test
	public void testHashcode() throws IOException {
		MachinePool empty1 = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"));
		MachinePool empty1Clone = pool(DateTime
				.parse("2014-01-13T12:00:00.000Z"));
		// different timestamp
		MachinePool empty2 = pool(UtcTime.parse("2014-01-14T15:00:00.000Z"));

		// single-member pool
		MachinePool singlePool = pool(
				UtcTime.parse("2014-01-13T12:00:00.000Z"),
				machineNoIp("m1", MachineState.PENDING,
						UtcTime.parse("2014-01-13T11:00:00.000Z")));
		MachinePool singlePoolClone = pool(
				UtcTime.parse("2014-01-13T12:00:00.000Z"),
				machineNoIp("m1", MachineState.PENDING,
						UtcTime.parse("2014-01-13T11:00:00.000Z")));

		// multi-member pool
		MachinePool multiPool = pool(
				UtcTime.parse("2014-01-13T12:00:00.000Z"),
				machineNoIp("m1", MachineState.RUNNING,
						UtcTime.parse("2014-01-13T11:00:00.000Z")),
				machineNoIp("m2", MachineState.REQUESTED, null));
		MachinePool multiPoolClone = pool(
				UtcTime.parse("2014-01-13T12:00:00.000Z"),
				machineNoIp("m1", MachineState.RUNNING,
						UtcTime.parse("2014-01-13T11:00:00.000Z")),
				machineNoIp("m2", MachineState.REQUESTED, null));

		assertThat(empty1.hashCode(), is(empty1Clone.hashCode()));
		assertThat(empty1.hashCode(), is(not(empty2.hashCode())));

		assertThat(singlePool.hashCode(), is(singlePoolClone.hashCode()));
		assertThat(singlePool.hashCode(), is(not(empty1.hashCode())));
		assertThat(singlePool.hashCode(), is(not(multiPool.hashCode())));

		assertThat(multiPool.hashCode(), is(multiPoolClone.hashCode()));
		assertThat(multiPool.hashCode(), is(not(empty1.hashCode())));
		assertThat(multiPool.hashCode(), is(not(singlePool.hashCode())));
	}

	/**
	 * Verifies proper parsing of a {@link MachinePool} from its JSON
	 * representation.
	 */
	@Test
	public void parseEmptyMachinePoolFromJson() throws IOException {
		MachinePool expectedPool = pool(DateTime
				.parse("2014-01-13T12:00:00.000Z"));

		MachinePool parsedPool = MachinePool.fromJson(loadJson(emptyPoolJson));
		assertThat(parsedPool, is(expectedPool));
	}

	/**
	 * Verifies proper conversion of an empty {@link MachinePool} to its JSON
	 * representation.
	 *
	 * @throws IOException
	 */
	@Test
	public void convertEmptyMachinePoolToJson() throws IOException {
		MachinePool pool = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"));
		assertThat(pool.toJson(),
				is(JsonUtils.parseJsonResource(emptyPoolJson)));
	}

	/**
	 * Verifies proper parsing of a {@link MachinePool} containing a single
	 * {@link Machine}.
	 *
	 * @throws IOException
	 */
	@Test
	public void parseSingleMachinePoolFromJson() throws IOException {
		Machine machine1 = new Machine("m1", MachineState.PENDING,
				ServiceState.IN_SERVICE,
				UtcTime.parse("2014-01-13T11:00:00.000Z"), ips("1.2.3.4"),
				ips("1.2.3.5"), parseJsonString("{\"k1\": \"v1\"}"));
		MachinePool expectedPool = pool(
				UtcTime.parse("2014-01-13T12:00:00.000Z"), machine1);

		MachinePool parsedPool = MachinePool
				.fromJson(loadJson(singleMachinePoolJson));
		assertThat(parsedPool, is(expectedPool));
	}

	/**
	 * Verifies proper JSON conversion of a {@link MachinePool} containing a
	 * single {@link Machine}.
	 *
	 * @throws IOException
	 */
	@Test
	public void convertSingleMachinePoolToJson() throws IOException {
		Machine machine1 = new Machine("m1", MachineState.PENDING,
				ServiceState.IN_SERVICE,
				UtcTime.parse("2014-01-13T11:00:00.000Z"), ips("1.2.3.4"),
				ips("1.2.3.5"), parseJsonString("{\"k1\": \"v1\"}"));
		MachinePool pool = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"),
				machine1);

		assertThat(pool.toJson(),
				is(JsonUtils.parseJsonResource(singleMachinePoolJson)));
	}

	/**
	 * Verifies proper parsing of a {@link MachinePool} containing several
	 * {@link Machine}s, where one {@link Machine} doesn't have a launch time
	 * and one {@link Machine} has meta data.
	 *
	 * @throws IOException
	 */
	@Test
	public void parseMultiMachinePoolFromJson() throws IOException {
		Machine machine1 = new Machine("m1", MachineState.RUNNING,
				ServiceState.IN_SERVICE,
				UtcTime.parse("2014-01-13T11:00:00.000Z"), ips("1.2.3.4"),
				ips(), parseJsonString("{\"k1\": \"v1\"}"));
		Machine machine2 = machine("m2", MachineState.REQUESTED, null, null,
				null);
		MachinePool expectedPool = pool(
				UtcTime.parse("2014-01-13T12:00:00.000Z"), machine1, machine2);

		MachinePool parsedPool = MachinePool
				.fromJson(loadJson(multiMachinePoolJson));
		assertThat(parsedPool, is(expectedPool));
	}

	/**
	 * Verifies proper JSON conversion of a {@link MachinePool} containing
	 * several {@link Machine}s, where one {@link Machine} doesn't have a launch
	 * time and one {@link Machine} has meta data.
	 *
	 * @throws IOException
	 */
	@Test
	public void convertMultiMachinePoolToJson() throws IOException {
		Machine machine1 = new Machine("m1", MachineState.RUNNING,
				ServiceState.IN_SERVICE,
				UtcTime.parse("2014-01-13T11:00:00.000Z"), ips("1.2.3.4"),
				ips(), parseJsonString("{\"k1\": \"v1\"}"));
		Machine machine2 = new Machine("m2", MachineState.REQUESTED,
				ServiceState.UNKNOWN, null, null, null);
		MachinePool pool = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"),
				machine1, machine2);

		assertThat(pool.toJson(),
				is(JsonUtils.parseJsonResource(multiMachinePoolJson)));
	}

	@Test(expected = NullPointerException.class)
	public void parseInvalidPoolMissingMachines() throws IOException {
		MachinePool
				.fromJson(loadJson("json/invalidpool-missing-machines.json"));
	}

	@Test(expected = NullPointerException.class)
	public void parseInvalidPoolMissingTimestamp() throws IOException {
		MachinePool
				.fromJson(loadJson("json/invalidpool-missing-timestamp.json"));
	}

	@Test(expected = NullPointerException.class)
	public void parseInvalidPoolWithMachineMissingId() throws IOException {
		MachinePool
				.fromJson(loadJson("json/invalidpool-machine-missing-id.json"));
	}

	@Test(expected = NullPointerException.class)
	public void parseInvalidPoolWithMachineMissingMachineState()
			throws IOException {
		MachinePool
				.fromJson(loadJson("json/invalidpool-machine-missing-machinestate.json"));
	}

	private String loadJson(String resourcePath) {
		try {
			return JsonUtils
					.toString(JsonUtils.parseJsonResource(resourcePath));
		} catch (Exception e) {
			throw new RuntimeException(String.format(
					"failed to parse JSON resource '%s': %s", resourcePath,
					e.getMessage()), e);
		}
	}

}
