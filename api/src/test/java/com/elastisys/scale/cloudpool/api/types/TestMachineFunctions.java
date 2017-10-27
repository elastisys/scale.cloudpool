package com.elastisys.scale.cloudpool.api.types;

import static com.elastisys.scale.cloudpool.api.types.TestUtils.ips;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Function;
import com.google.gson.JsonObject;

/**
 * Verifies the behavior of {@link Function}s declared for the {@link Machine}
 * class.
 *
 *
 *
 */
public class TestMachineFunctions {

    @Test
    public void testToShortMachineString() {
        DateTime now = UtcTime.now();
        JsonObject metadata = JsonUtils.parseJsonString("{\"id\": \"i-1\"}").getAsJsonObject();
        Machine m1 = Machine.builder().id("i-1").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").requestTime(now).launchTime(now).publicIps(ips("1.2.3.4"))
                .privateIps(ips("1.2.3.5")).metadata(metadata).build();

        assertFalse(Machine.toShortString().apply(m1).contains("metadata"));
    }

    @Test
    public void testToShortMachineFormat() {
        DateTime now = UtcTime.now();
        JsonObject metadata = JsonUtils.parseJsonString("{\"id\": \"i-1\"}").getAsJsonObject();
        Machine m1 = Machine.builder().id("i-2").machineState(MachineState.RUNNING).cloudProvider("AWS-EC2")
                .region("us-east-1").machineSize("m1.small").requestTime(now).launchTime(now).publicIps(ips("1.2.3.4"))
                .privateIps(ips("1.2.3.5")).metadata(metadata).build();

        Machine m1Stripped = Machine.toShortFormat().apply(m1);
        // all fields should be equal except metadata which should be null
        assertThat(m1Stripped.getId(), is(m1.getId()));
        assertThat(m1Stripped.getMachineState(), is(m1.getMachineState()));
        assertThat(m1Stripped.getMembershipStatus(), is(m1.getMembershipStatus()));
        assertThat(m1Stripped.getServiceState(), is(m1.getServiceState()));
        assertThat(m1Stripped.getLaunchTime(), is(m1.getLaunchTime()));
        assertThat(m1Stripped.getPublicIps(), is(m1.getPublicIps()));
        assertThat(m1Stripped.getPrivateIps(), is(m1.getPrivateIps()));
        assertThat(m1Stripped.getMetadata(), is(nullValue()));
    }
}
