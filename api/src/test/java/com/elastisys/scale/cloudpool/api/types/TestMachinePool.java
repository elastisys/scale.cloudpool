package com.elastisys.scale.cloudpool.api.types;

import static com.elastisys.scale.cloudpool.api.types.TestUtils.ips;
import static com.elastisys.scale.cloudpool.api.types.TestUtils.machineNoIp;
import static com.elastisys.scale.cloudpool.api.types.TestUtils.pool;
import static com.elastisys.scale.commons.json.JsonUtils.parseJsonString;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;

import org.joda.time.DateTime;
import org.junit.Test;

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
        Machine requested1 = Machine.builder().id("i-1").machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        Machine requested2 = Machine.builder().id("i-2").machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        Machine pending1 = Machine.builder().id("i-3").machineState(MachineState.PENDING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.BOOTING).requestTime(now)
                .launchTime(now).build();
        Machine running1 = Machine.builder().id("i-4").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.IN_SERVICE).requestTime(now)
                .launchTime(now).publicIps(asList("1.2.3.4")).build();
        Machine running2 = Machine.builder().id("i-5").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.UNHEALTHY).requestTime(now)
                .launchTime(now).publicIps(asList("1.2.3.4")).build();
        Machine running3 = Machine.builder().id("i-5.1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.OUT_OF_SERVICE).requestTime(now)
                .launchTime(now).publicIps(asList("1.2.3.5")).build();
        Machine terminating = Machine.builder().id("i-6").machineState(MachineState.TERMINATING)
                .cloudProvider("AWS-EC2").region("us-east-1").machineSize("m1.small").build();
        Machine terminated1 = Machine.builder().id("i-7").machineState(MachineState.TERMINATED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        Machine terminated2 = Machine.builder().id("i-8").machineState(MachineState.TERMINATED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        pool = new MachinePool(Arrays.asList(requested1, requested2, pending1, running1, running2, running3,
                terminating, terminated1, terminated2), now);
        // verify machines
        assertThat(pool.getMachines().size(), is(9));
        assertThat(pool.getMachines(), is(asList(requested1, requested2, pending1, running1, running2, running3,
                terminating, terminated1, terminated2)));
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
        Machine requested1 = Machine.builder().id("i-1").machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        Machine requested2 = Machine.builder().id("i-2").machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        Machine pending1 = Machine.builder().id("i-3").machineState(MachineState.PENDING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.BOOTING).requestTime(now)
                .launchTime(now).build();
        Machine running1 = Machine.builder().id("i-4").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.IN_SERVICE).requestTime(now)
                .launchTime(now).publicIps(asList("1.2.3.4")).build();
        Machine running2 = Machine.builder().id("i-5").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.UNHEALTHY).requestTime(now)
                .launchTime(now).publicIps(asList("1.2.3.4")).build();
        Machine running3 = Machine.builder().id("i-5.1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.OUT_OF_SERVICE).requestTime(now)
                .launchTime(now).publicIps(asList("1.2.3.5")).build();
        Machine terminating = Machine.builder().id("i-6").machineState(MachineState.TERMINATING)
                .cloudProvider("AWS-EC2").region("us-east-1").machineSize("m1.small").build();
        Machine terminated1 = Machine.builder().id("i-7").machineState(MachineState.TERMINATED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        Machine terminated2 = Machine.builder().id("i-8").machineState(MachineState.TERMINATED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        pool = new MachinePool(Arrays.asList(requested1, requested2, pending1, running1, running2, running3,
                terminating, terminated1, terminated2), now);
        // verify allocated machines: 2 requested + 1 pending + 3 running
        assertThat(pool.getAllocatedMachines().size(), is(6));
        assertThat(pool.getAllocatedMachines(),
                is(asList(requested1, requested2, pending1, running1, running2, running3)));
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
        Machine requested1 = Machine.builder().id("i-1").machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        Machine requested2 = Machine.builder().id("i-2").machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        Machine pending1 = Machine.builder().id("i-3").machineState(MachineState.PENDING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.BOOTING).requestTime(now)
                .launchTime(now).build();
        Machine running1 = Machine.builder().id("i-4").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.IN_SERVICE)
                .membershipStatus(MembershipStatus.blessed()).requestTime(now).launchTime(now)
                .publicIps(asList("1.2.3.4")).build();
        Machine running2 = Machine.builder().id("i-5").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.UNHEALTHY).requestTime(now)
                .launchTime(now).publicIps(asList("1.2.3.4")).build();
        Machine running3 = Machine.builder().id("i-5.1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.OUT_OF_SERVICE)
                .membershipStatus(MembershipStatus.awaitingService()).requestTime(now).launchTime(now)
                .publicIps(asList("1.2.3.5")).build();
        Machine terminating = Machine.builder().id("i-6").machineState(MachineState.TERMINATING)
                .cloudProvider("AWS-EC2").region("us-east-1").machineSize("m1.small").build();
        Machine terminated1 = Machine.builder().id("i-7").machineState(MachineState.TERMINATED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        Machine terminated2 = Machine.builder().id("i-8").machineState(MachineState.TERMINATED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        pool = new MachinePool(Arrays.asList(requested1, requested2, pending1, running1, running2, running3,
                terminating, terminated1, terminated2), now);
        // verify effective machines: 2 requested + 1 pending + 2 running that
        // don't have an inactive membership status
        assertThat(pool.getActiveMachines().size(), is(5));
        assertThat(pool.getActiveMachines(), is(asList(requested1, requested2, pending1, running1, running2)));
    }

    /**
     * Exercise {@link MachinePool#getStartedMachines()}
     */
    @Test
    public void testGetStartedMachines() {
        DateTime now = UtcTime.now();

        // on empty pool
        MachinePool pool = MachinePool.emptyPool(now);
        assertThat(pool.getActiveMachines().size(), is(0));

        // on a pool with a mix of machines in different states
        Machine requested1 = Machine.builder().id("i-1").machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        Machine requested2 = Machine.builder().id("i-2").machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        Machine pending1 = Machine.builder().id("i-3").machineState(MachineState.PENDING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.BOOTING).requestTime(now)
                .launchTime(now).build();
        Machine running1 = Machine.builder().id("i-4").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.IN_SERVICE)
                .membershipStatus(MembershipStatus.blessed()).requestTime(now).launchTime(now)
                .publicIps(asList("1.2.3.4")).build();
        Machine running2 = Machine.builder().id("i-5").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.UNHEALTHY).requestTime(now)
                .launchTime(now).publicIps(asList("1.2.3.4")).build();
        Machine running3 = Machine.builder().id("i-5.1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.OUT_OF_SERVICE)
                .membershipStatus(MembershipStatus.awaitingService()).requestTime(now).launchTime(now)
                .publicIps(asList("1.2.3.5")).build();
        Machine terminating = Machine.builder().id("i-6").machineState(MachineState.TERMINATING)
                .cloudProvider("AWS-EC2").region("us-east-1").machineSize("m1.small").build();
        Machine terminated1 = Machine.builder().id("i-7").machineState(MachineState.TERMINATED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        Machine terminated2 = Machine.builder().id("i-8").machineState(MachineState.TERMINATED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        pool = new MachinePool(Arrays.asList(requested1, requested2, pending1, running1, running2, running3,
                terminating, terminated1, terminated2), now);
        // verify started machines: 1 pending + 3 running that
        // don't have an inactive membership status
        assertThat(pool.getStartedMachines().size(), is(4));
        assertThat(pool.getStartedMachines(), is(asList(pending1, running1, running2, running3)));
    }

    /**
     * Test equality comparisons.
     */
    @Test
    public void testEquality() throws IOException {
        MachinePool empty1 = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"));
        MachinePool empty1Clone = pool(DateTime.parse("2014-01-13T12:00:00.000Z"));
        // different timestamp
        MachinePool empty2 = pool(UtcTime.parse("2014-01-14T15:00:00.000Z"));

        // single-member pool
        MachinePool singlePool = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"),
                machineNoIp("m1", MachineState.PENDING, UtcTime.parse("2014-01-13T11:00:00.000Z")));
        MachinePool singlePoolClone = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"),
                machineNoIp("m1", MachineState.PENDING, UtcTime.parse("2014-01-13T11:00:00.000Z")));

        // multi-member pool
        MachinePool multiPool = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"),
                machineNoIp("m1", MachineState.RUNNING, UtcTime.parse("2014-01-13T11:00:00.000Z")),
                machineNoIp("m2", MachineState.REQUESTED, null));
        MachinePool multiPoolClone = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"),
                machineNoIp("m1", MachineState.RUNNING, UtcTime.parse("2014-01-13T11:00:00.000Z")),
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
        MachinePool noTimestamp = JsonUtils
                .toObject(JsonUtils.parseJsonResource("json/invalidpool-missing-timestamp.json"), MachinePool.class);
        MachinePool noTimestampClone = JsonUtils
                .toObject(JsonUtils.parseJsonResource("json/invalidpool-missing-timestamp.json"), MachinePool.class);
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
        MachinePool empty1Clone = pool(DateTime.parse("2014-01-13T12:00:00.000Z"));
        // different timestamp
        MachinePool empty2 = pool(UtcTime.parse("2014-01-14T15:00:00.000Z"));

        // single-member pool
        MachinePool singlePool = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"),
                machineNoIp("m1", MachineState.PENDING, UtcTime.parse("2014-01-13T11:00:00.000Z")));
        MachinePool singlePoolClone = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"),
                machineNoIp("m1", MachineState.PENDING, UtcTime.parse("2014-01-13T11:00:00.000Z")));

        // multi-member pool
        MachinePool multiPool = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"),
                machineNoIp("m1", MachineState.RUNNING, UtcTime.parse("2014-01-13T11:00:00.000Z")),
                machineNoIp("m2", MachineState.REQUESTED, null));
        MachinePool multiPoolClone = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"),
                machineNoIp("m1", MachineState.RUNNING, UtcTime.parse("2014-01-13T11:00:00.000Z")),
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
        MachinePool expectedPool = pool(DateTime.parse("2014-01-13T12:00:00.000Z"));

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
        assertThat(pool.toJson(), is(JsonUtils.parseJsonResource(emptyPoolJson)));
    }

    /**
     * Verifies proper parsing of a {@link MachinePool} containing a single
     * {@link Machine}.
     *
     * @throws IOException
     */
    @Test
    public void parseSingleMachinePoolFromJson() throws IOException {
        Machine machine1 = Machine.builder().id("m1").machineState(MachineState.PENDING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.IN_SERVICE)
                .requestTime(UtcTime.parse("2014-01-13T11:00:00.000Z"))
                .launchTime(UtcTime.parse("2014-01-13T11:00:00.000Z")).publicIps(ips("1.2.3.4"))
                .privateIps(ips("1.2.3.5")).metadata(parseJsonString("{\"k1\": \"v1\"}").getAsJsonObject()).build();
        MachinePool expectedPool = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"), machine1);

        MachinePool parsedPool = MachinePool.fromJson(loadJson(singleMachinePoolJson));
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
        Machine machine1 = Machine.builder().id("m1").machineState(MachineState.PENDING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").serviceState(ServiceState.IN_SERVICE)
                .requestTime(UtcTime.parse("2014-01-13T11:00:00.000Z"))
                .launchTime(UtcTime.parse("2014-01-13T11:00:00.000Z")).publicIps(ips("1.2.3.4"))
                .privateIps(ips("1.2.3.5")).metadata(parseJsonString("{\"k1\": \"v1\"}").getAsJsonObject()).build();
        MachinePool pool = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"), machine1);

        assertThat(pool.toJson(), is(JsonUtils.parseJsonResource(singleMachinePoolJson)));
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
        Machine machine1 = Machine.builder().id("m1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").membershipStatus(new MembershipStatus(true, false))
                .serviceState(ServiceState.IN_SERVICE).requestTime(UtcTime.parse("2014-01-13T11:00:00.000Z"))
                .launchTime(UtcTime.parse("2014-01-13T11:00:00.000Z")).publicIps(ips("1.2.3.4"))
                .metadata(parseJsonString("{\"k1\": \"v1\"}").getAsJsonObject()).build();
        Machine machine2 = Machine.builder().id("m2").machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        MachinePool expectedPool = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"), machine1, machine2);

        MachinePool parsedPool = MachinePool.fromJson(loadJson(multiMachinePoolJson));
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
        Machine machine1 = Machine.builder().id("m1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").membershipStatus(new MembershipStatus(true, false))
                .serviceState(ServiceState.IN_SERVICE).requestTime(UtcTime.parse("2014-01-13T11:00:00.000Z"))
                .launchTime(UtcTime.parse("2014-01-13T11:00:00.000Z")).publicIps(ips("1.2.3.4"))
                .metadata(parseJsonString("{\"k1\": \"v1\"}").getAsJsonObject()).build();
        Machine machine2 = Machine.builder().id("m2").machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        MachinePool pool = pool(UtcTime.parse("2014-01-13T12:00:00.000Z"), machine1, machine2);

        assertThat(pool.toJson(), is(JsonUtils.parseJsonResource(multiMachinePoolJson)));
    }

    @Test(expected = NullPointerException.class)
    public void parseInvalidPoolMissingMachines() throws IOException {
        MachinePool.fromJson(loadJson("json/invalidpool-missing-machines.json"));
    }

    @Test(expected = NullPointerException.class)
    public void parseInvalidPoolMissingTimestamp() throws IOException {
        MachinePool.fromJson(loadJson("json/invalidpool-missing-timestamp.json"));
    }

    @Test(expected = NullPointerException.class)
    public void parseInvalidPoolWithMachineMissingId() throws IOException {
        MachinePool.fromJson(loadJson("json/invalidpool-machine-missing-id.json"));
    }

    @Test(expected = NullPointerException.class)
    public void parseInvalidPoolWithMachineMissingMachineState() throws IOException {
        MachinePool.fromJson(loadJson("json/invalidpool-machine-missing-machinestate.json"));
    }

    private String loadJson(String resourcePath) {
        try {
            return JsonUtils.toString(JsonUtils.parseJsonResource(resourcePath));
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("failed to parse JSON resource '%s': %s", resourcePath, e.getMessage()), e);
        }
    }

}
