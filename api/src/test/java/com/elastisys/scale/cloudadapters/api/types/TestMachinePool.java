package com.elastisys.scale.cloudadapters.api.types;

import static com.elastisys.scale.cloudadapters.api.types.TestUtils.ips;
import static com.elastisys.scale.cloudadapters.api.types.TestUtils.machine;
import static com.elastisys.scale.cloudadapters.api.types.TestUtils.machineNoIp;
import static com.elastisys.scale.cloudadapters.api.types.TestUtils.pool;
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
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

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
		Machine requested1 = new Machine("i-1", MachineState.REQUESTED, null,
				null, null, new JsonObject());
		Machine requested2 = new Machine("i-2", MachineState.REQUESTED, null,
				null, null, new JsonObject());
		Machine pending1 = new Machine("i-3", MachineState.PENDING, now, null,
				null, new JsonObject());
		Machine running1 = new Machine("i-4", MachineState.RUNNING, now,
				asList("1.2.3.4"), null, new JsonObject());
		Machine running2 = new Machine("i-5", MachineState.RUNNING, now,
				asList("1.2.3.4"), null, new JsonObject());
		Machine terminating = new Machine("i-6", MachineState.TERMINATING, now,
				null, null, new JsonObject());
		Machine terminated1 = new Machine("i-7", MachineState.TERMINATED, now,
				null, null, new JsonObject());
		Machine terminated2 = new Machine("i-8", MachineState.TERMINATED, now,
				null, null, new JsonObject());
		pool = new MachinePool(Arrays.asList(requested1, requested2, pending1,
				running1, running2, terminating, terminated1, terminated2), now);
		// verify machines
		assertThat(pool.getMachines().size(), is(8));
		assertThat(
				pool.getMachines(),
				is(asList(requested1, requested2, pending1, running1, running2,
						terminating, terminated1, terminated2)));
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
		Machine requested1 = new Machine("i-1", MachineState.REQUESTED, null,
				null, null, new JsonObject());
		Machine requested2 = new Machine("i-2", MachineState.REQUESTED, null,
				null, null, new JsonObject());
		Machine pending1 = new Machine("i-3", MachineState.PENDING, now, null,
				null, new JsonObject());
		Machine running1 = new Machine("i-4", MachineState.RUNNING, now,
				asList("1.2.3.4"), null, new JsonObject());
		Machine running2 = new Machine("i-5", MachineState.RUNNING, now,
				asList("1.2.3.4"), null, new JsonObject());
		Machine terminating = new Machine("i-6", MachineState.TERMINATING, now,
				null, null, new JsonObject());
		Machine terminated1 = new Machine("i-7", MachineState.TERMINATED, now,
				null, null, new JsonObject());
		Machine terminated2 = new Machine("i-8", MachineState.TERMINATED, now,
				null, null, new JsonObject());
		pool = new MachinePool(Arrays.asList(requested1, requested2, pending1,
				running1, running2, terminating, terminated1, terminated2), now);
		// verify active machines: 1 pending + 2 running
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
		Machine requested1 = new Machine("i-1", MachineState.REQUESTED, null,
				null, null, new JsonObject());
		Machine requested2 = new Machine("i-2", MachineState.REQUESTED, null,
				null, null, new JsonObject());
		Machine pending1 = new Machine("i-3", MachineState.PENDING, now, null,
				null, new JsonObject());
		Machine running1 = new Machine("i-4", MachineState.RUNNING, now,
				asList("1.2.3.4"), null, new JsonObject());
		Machine running2 = new Machine("i-5", MachineState.RUNNING, now,
				asList("1.2.3.4"), null, new JsonObject());
		Machine terminating = new Machine("i-6", MachineState.TERMINATING, now,
				null, null, new JsonObject());
		Machine terminated1 = new Machine("i-7", MachineState.TERMINATED, now,
				null, null, new JsonObject());
		Machine terminated2 = new Machine("i-8", MachineState.TERMINATED, now,
				null, null, new JsonObject());
		pool = new MachinePool(Arrays.asList(requested1, requested2, pending1,
				running1, running2, terminating, terminated1, terminated2), now);
		// verify allocated machines: 2 requested + 1 pending + 2 running
		assertThat(pool.getAllocatedMachines().size(), is(5));
		assertThat(
				pool.getAllocatedMachines(),
				is(asList(requested1, requested2, pending1, running1, running2)));
	}

	/**
	 * Test equality comparisons.
	 * 
	 * @throws IOException
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
						UtcTime.parse("2014-01-13T11:00:00.000Z"),
						new JsonObject()));
		MachinePool singlePoolClone = pool(
				UtcTime.parse("2014-01-13T12:00:00.000Z"),
				machineNoIp("m1", MachineState.PENDING,
						UtcTime.parse("2014-01-13T11:00:00.000Z"),
						new JsonObject()));

		// multi-member pool
		String metadata = "{ \"key\": \"value\", "
				+ "\"complex\": {\"k1\":\"v1\", \"k2\":\"v2\"} }";
		MachinePool multiPool = pool(
				UtcTime.parse("2014-01-13T12:00:00.000Z"),
				machineNoIp("m1", MachineState.RUNNING,
						UtcTime.parse("2014-01-13T11:00:00.000Z"),
						JsonUtils.parseJsonString(metadata)),
				machineNoIp("m2", MachineState.REQUESTED, null,
						new JsonObject()));
		MachinePool multiPoolClone = pool(
				UtcTime.parse("2014-01-13T12:00:00.000Z"),
				machineNoIp("m1", MachineState.RUNNING,
						UtcTime.parse("2014-01-13T11:00:00.000Z"),
						JsonUtils.parseJsonString(metadata)),
				machineNoIp("m2", MachineState.REQUESTED, null,
						new JsonObject()));

		assertThat(empty1, is(empty1Clone));
		assertThat(empty1, is(not(empty2)));

		assertThat(singlePool, is(singlePoolClone));
		assertThat(singlePool, is(not(empty1)));
		assertThat(singlePool, is(not(multiPool)));

		assertThat(multiPool, is(multiPoolClone));
		assertThat(multiPool, is(not(empty1)));
		assertThat(multiPool, is(not(singlePool)));
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
		Machine machine1 = machine("m1", MachineState.PENDING,
				UtcTime.parse("2014-01-13T11:00:00.000Z"), ips("1.2.3.4"),
				ips("1.2.3.5"), new JsonObject());
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
		Machine machine1 = machine("m1", MachineState.PENDING,
				UtcTime.parse("2014-01-13T11:00:00.000Z"), ips("1.2.3.4"),
				ips("1.2.3.5"), new JsonObject());
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
		JsonObject machine1Metadata = JsonUtils
				.parseJsonString("{ \"key\": \"value\", "
						+ "\"complex-key\": { \"k1\": \"v1\", \"k2\": \"v2\"} }");
		Machine machine1 = machine("m1", MachineState.RUNNING,
				UtcTime.parse("2014-01-13T11:00:00.000Z"), ips("1.2.3.4"),
				ips(), machine1Metadata);
		Machine machine2 = machine("m2", MachineState.REQUESTED, null, null,
				null, new JsonObject());
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
		JsonObject machine1Metadata = JsonUtils
				.parseJsonString("{ \"key\": \"value\", "
						+ "\"complex-key\": { \"k1\": \"v1\", \"k2\": \"v2\"} }");
		Machine machine1 = machine("m1", MachineState.RUNNING,
				UtcTime.parse("2014-01-13T11:00:00.000Z"), ips("1.2.3.4"),
				ips(), machine1Metadata);
		Machine machine2 = machine("m2", MachineState.REQUESTED, null, null,
				null, new JsonObject());
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
	public void parseInvalidPoolWithMachineMissingState() throws IOException {
		MachinePool
				.fromJson(loadJson("json/invalidpool-machine-missing-state.json"));
	}

	@Test(expected = NullPointerException.class)
	public void parseInvalidPoolWithMachineMissingMetadata() throws IOException {
		MachinePool
				.fromJson(loadJson("json/invalidpool-machine-missing-metadata.json"));
	}

	/**
	 * Attempts to parse a JSON representation of a {@link MachinePool}.
	 * 
	 * @param jsonRepresentation
	 *            JSON document as a {@link String}.
	 * @return
	 */
	private MachinePool parsePool(JsonObject jsonRepresentation) {
		return JsonUtils.toObject(jsonRepresentation, MachinePool.class);
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
