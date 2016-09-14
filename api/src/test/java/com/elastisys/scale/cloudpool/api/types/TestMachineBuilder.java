package com.elastisys.scale.cloudpool.api.types;

import static com.elastisys.scale.cloudpool.api.types.TestUtils.ips;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

/**
 * Exercise the {@link Machine.Builder} class.
 */
public class TestMachineBuilder {

    /**
     * Check that default values are given to non-mandatory fields.
     */
    @Test
    public void createMinimal() {
        Machine m1 = Machine.builder().id("i-1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").build();
        assertThat(m1.getId(), is("i-1"));
        assertThat(m1.getMachineState(), is(MachineState.RUNNING));
        assertThat(m1.getCloudProvider(), is("AWS-EC2"));
        assertThat(m1.getRegion(), is("us-east-1"));
        assertThat(m1.getMachineSize(), is("m1.small"));

        // check defaults
        assertThat(m1.getServiceState(), is(ServiceState.UNKNOWN));
        assertThat(m1.getMembershipStatus(), is(MembershipStatus.defaultStatus()));
        assertThat(m1.getRequestTime(), is(nullValue()));
        assertThat(m1.getLaunchTime(), is(nullValue()));
        List<String> emptyList = Collections.emptyList();
        assertThat(m1.getPublicIps(), is(emptyList));
        assertThat(m1.getPrivateIps(), is(emptyList));
        assertThat(m1.getMetadata(), is(nullValue()));
    }

    /**
     * Id is a mandatory field.
     */
    @Test
    public void createWithoutId() {
        try {
            Machine.builder().machineState(MachineState.RUNNING).cloudProvider("AWS-EC2").region("us-east-1")
                    .machineSize("m1.small").build();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("id"));
        }
    }

    /**
     * machine state is mandatory.
     */
    @Test
    public void createWithoutMachineState() {
        try {
            Machine.builder().id("i-1")/* .machineState(MachineState.RUNNING) */
                    .cloudProvider("AWS-EC2").region("us-east-1").machineSize("m1.small").build();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("machineState"));
        }
    }

    /**
     * A {@link Machine} must have a cloud provider.
     */
    @Test
    public void createWithoutCloudProvider() {
        try {
            Machine.builder().id("i-1").machineState(MachineState.RUNNING)
                    /* .cloudProvider("AWS-EC2") */.region("us-east-1").machineSize("m1.small").build();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("cloudProvider"));
        }
    }

    /**
     * A {@link Machine} must have a region.
     */
    @Test
    public void createWithoutRegion() {
        try {
            Machine.builder().id("i-1").machineState(MachineState.RUNNING)
                    .cloudProvider("AWS-EC2") /* .region("us-east-1") */
                    .machineSize("m1.small").build();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("region"));
        }
    }

    /**
     * A {@link Machine} must have a size.
     */
    @Test
    public void createWithoutMachineSize() {
        try {
            Machine.builder().id("i-1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2").region("us-east-1")
                    /* .machineSize("m1.small") */.build();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("machineSize"));
        }
    }

    /**
     * Specify a {@link ServiceState}.
     */
    @Test
    public void createWithServiceState() {
        Machine withServiceState = Machine.builder().id("i-1").machineState(MachineState.REQUESTED)
                .cloudProvider("AWS-EC2").region("us-east-1").machineSize("m1.small")
                .serviceState(ServiceState.IN_SERVICE).build();
        assertThat(withServiceState.getServiceState(), is(ServiceState.IN_SERVICE));
    }

    @Test
    public void createWithNullServiceState() {
        try {
            Machine.builder().id("i-1").machineState(MachineState.REQUESTED).serviceState(null).build();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("serviceState cannot be null"));
        }
    }

    @Test
    public void createWithMembershipStatus() {
        Machine machine = Machine.builder().id("i-1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").membershipStatus(MembershipStatus.blessed()).build();

        assertThat(machine.getMembershipStatus(), is(MembershipStatus.blessed()));
    }

    @Test
    public void createWithNullMembershipStatus() {
        try {
            Machine.builder().id("i-1").machineState(MachineState.RUNNING).membershipStatus(null).build();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("membershipStatus cannot be null"));
        }
    }

    @Test
    public void createWithRequestTime() {
        DateTime now = UtcTime.now();
        Machine machine = Machine.builder().id("i-1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").requestTime(now).build();

        assertThat(machine.getRequestTime(), is(now));
        assertThat(machine.getLaunchTime(), is(nullValue()));
    }

    /**
     * Request time can be <code>null</code>.
     */
    @Test
    public void createWithNullRequestTime() {
        Machine machine = Machine.builder().id("i-1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").requestTime(null).build();

        assertThat(machine.getRequestTime(), is(nullValue()));
    }

    @Test
    public void createWithLaunchTime() {
        DateTime now = UtcTime.now();
        Machine machine = Machine.builder().id("i-1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").launchTime(now).build();
        assertThat(machine.getLaunchTime(), is(now));
        assertThat(machine.getRequestTime(), is(nullValue()));
    }

    /**
     * Launch time can be <code>null</code>.
     */
    @Test
    public void createWithNullLaunchTime() {
        Machine machine = Machine.builder().id("i-1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").launchTime(null).build();

        assertThat(machine.getLaunchTime(), is(nullValue()));
    }

    /**
     * There have been times when a cloud provider returns a request time that
     * is later than the launch time. Even though this cannot be true, we still
     * need to allow it to prevent bad meta data from raising exceptions.
     */
    @Test
    public void createWithLaunchTimeBeforeRequestTime() {
        // requestTime later than launchTime
        DateTime requestTime = UtcTime.parse("2015-10-10T14:00:00.000Z");
        DateTime launchTime = UtcTime.parse("2015-10-10T12:00:00.000Z");

        Machine.builder().id("i-1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2").region("us-east-1")
                .machineSize("m1.small").requestTime(requestTime).launchTime(launchTime).build();
    }

    @Test
    public void createWithPrivateIpAddress() {
        Machine m = Machine.builder().id("i-1").machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").privateIp("1.2.3.4").privateIp("1.2.3.5").build();
        assertThat(m.getPublicIps(), is(ips()));
        assertThat(m.getPrivateIps(), is(ips("1.2.3.4", "1.2.3.5")));

        m = Machine.builder().id("i-1").machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").privateIps(ips("1.2.3.4", "1.2.3.5")).build();
        assertThat(m.getPublicIps(), is(ips()));
        assertThat(m.getPrivateIps(), is(ips("1.2.3.4", "1.2.3.5")));
    }

    @Test
    public void createWithNullPrivateIpAddress() {
        try {
            Machine.builder().id("i-1").machineState(MachineState.REQUESTED).privateIp(null).build();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("privateIp cannot be null"));
        }
    }

    @Test
    public void createWithPublicIpAddress() {
        Machine m = Machine.builder().id("i-1").machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").publicIp("1.2.3.4").publicIp("1.2.3.5").build();
        assertThat(m.getPrivateIps(), is(ips()));
        assertThat(m.getPublicIps(), is(ips("1.2.3.4", "1.2.3.5")));

        m = Machine.builder().id("i-1").machineState(MachineState.REQUESTED).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").publicIps(ips("1.2.3.4", "1.2.3.5")).build();
        assertThat(m.getPrivateIps(), is(ips()));
        assertThat(m.getPublicIps(), is(ips("1.2.3.4", "1.2.3.5")));
    }

    @Test
    public void createWithNullPublicIpAddress() {
        try {
            Machine.builder().id("i-1").machineState(MachineState.REQUESTED).publicIp(null).build();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("publicIp cannot be null"));
        }
    }

    @Test
    public void createWithMetadata() {
        JsonObject metadata = JsonUtils.parseJsonString("{'a': 1, 'b': 2, c: {'d': 4}}").getAsJsonObject();
        Machine machine = Machine.builder().id("i-1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").metadata(metadata).build();
        assertThat(machine.getMetadata(), is(metadata));
    }

    @Test
    public void createWithOnlyPublicIpAddress() {
        Machine onlyPublicIps = Machine.builder().id("i-1").machineState(MachineState.REQUESTED)
                .cloudProvider("AWS-EC2").region("us-east-1").machineSize("m1.small").publicIps(ips("1.2.3.4")).build();
        assertThat(onlyPublicIps.getPrivateIps(), is(ips()));
        assertThat(onlyPublicIps.getPublicIps(), is(ips("1.2.3.4")));
    }

}
