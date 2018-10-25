package com.elastisys.scale.cloudpool.google.commons.api.compute.functions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.types.CloudProviders;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.google.commons.api.compute.metadata.MetadataKeys;
import com.elastisys.scale.cloudpool.google.commons.utils.MetadataUtil;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.io.Resources;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.model.Instance;

/**
 * Verifies that {@link Instance} are correctly translated into {@link Machine}
 * representations.
 */
public class TestInstanceToMachine {

    /** Object under test. */
    private InstanceToMachine converter;

    @Before
    public void beforeTestMethod() {
        this.converter = new InstanceToMachine();
    }

    /**
     * Test converting a (metadata-complete) {@link Instance} to a
     * {@link Machine} representation.
     */
    @Test
    public void convertInstance() throws IOException {
        Instance instance = instance("instances/instance.json");
        Machine machine = this.converter.apply(instance);

        // the instance's URL is used as machine id
        assertThat(machine.getId(), is(instance.getSelfLink()));
        assertThat(machine.getMachineState(), is(MachineState.RUNNING));
        assertThat(machine.getServiceState(), is(ServiceState.UNKNOWN));
        assertThat(machine.getMembershipStatus(), is(MembershipStatus.defaultStatus()));
        assertThat(machine.getLaunchTime(), is(UtcTime.parse(instance.getCreationTimestamp())));
        assertThat(machine.getRequestTime(), is(nullValue()));
        assertThat(machine.getPrivateIps(), is(Arrays.asList(instance.getNetworkInterfaces().get(0).getNetworkIP())));
        assertThat(machine.getPublicIps(),
                is(Arrays.asList(instance.getNetworkInterfaces().get(0).getAccessConfigs().get(0).getNatIP())));
        assertThat(machine.getRequestTime(), is(nullValue()));

        assertThat(machine.getCloudProvider(), is(CloudProviders.GCE));
        assertThat(machine.getRegion(), is("europe-west1"));
        assertThat(machine.getMachineSize(), is("n1-standard-1"));

        assertThat(machine.getMetadata(), is(nullValue()));
    }

    /**
     * An instance's status should be correctly converted to a corresponding
     * {@link MachineState}.
     */
    @Test
    public void convertMachineState() {
        Instance instance = instance("instances/instance.json");

        instance.setStatus("PROVISIONING");
        assertThat(this.converter.apply(instance).getMachineState(), is(MachineState.PENDING));

        instance.setStatus("STAGING");
        assertThat(this.converter.apply(instance).getMachineState(), is(MachineState.PENDING));

        instance.setStatus("RUNNING");
        assertThat(this.converter.apply(instance).getMachineState(), is(MachineState.RUNNING));

        instance.setStatus("STOPPING");
        assertThat(this.converter.apply(instance).getMachineState(), is(MachineState.TERMINATING));

        instance.setStatus("SUSPENDING");
        assertThat(this.converter.apply(instance).getMachineState(), is(MachineState.TERMINATING));

        instance.setStatus("SUSPENDED");
        assertThat(this.converter.apply(instance).getMachineState(), is(MachineState.TERMINATED));

        instance.setStatus("TERMINATED");
        assertThat(this.converter.apply(instance).getMachineState(), is(MachineState.TERMINATED));
    }

    /**
     * If a {@link MetadataKeys#MEMBERSHIP_STATUS} metadata key has been set on
     * the {@link Instance}, it should be parsed and set on the {@link Machine}.
     */
    @Test
    public void convertInstanceWithMembershipStatusMetadata() {
        Instance instance = instance("instances/instance.json");

        setInstanceMetadata(instance, MetadataKeys.MEMBERSHIP_STATUS, MembershipStatus.defaultStatus());
        assertThat(this.converter.apply(instance).getMembershipStatus(), is(MembershipStatus.defaultStatus()));

        setInstanceMetadata(instance, MetadataKeys.MEMBERSHIP_STATUS, MembershipStatus.awaitingService());
        assertThat(this.converter.apply(instance).getMembershipStatus(), is(MembershipStatus.awaitingService()));

        setInstanceMetadata(instance, MetadataKeys.MEMBERSHIP_STATUS, MembershipStatus.blessed());
        assertThat(this.converter.apply(instance).getMembershipStatus(), is(MembershipStatus.blessed()));

        setInstanceMetadata(instance, MetadataKeys.MEMBERSHIP_STATUS, MembershipStatus.disposable());
        assertThat(this.converter.apply(instance).getMembershipStatus(), is(MembershipStatus.disposable()));
    }

    /**
     * If a {@link MetadataKeys#SERVICE_STATE} metadata key has been set on the
     * {@link Instance}, it should be parsed and set on the {@link Machine}.
     */
    @Test
    public void convertInstanceWithServiceStateMetadata() {
        Instance instance = instance("instances/instance.json");

        setInstanceMetadata(instance, MetadataKeys.SERVICE_STATE, ServiceState.BOOTING);
        assertThat(this.converter.apply(instance).getServiceState(), is(ServiceState.BOOTING));

        setInstanceMetadata(instance, MetadataKeys.SERVICE_STATE, ServiceState.IN_SERVICE);
        assertThat(this.converter.apply(instance).getServiceState(), is(ServiceState.IN_SERVICE));

        setInstanceMetadata(instance, MetadataKeys.SERVICE_STATE, ServiceState.OUT_OF_SERVICE);
        assertThat(this.converter.apply(instance).getServiceState(), is(ServiceState.OUT_OF_SERVICE));

        setInstanceMetadata(instance, MetadataKeys.SERVICE_STATE, ServiceState.UNHEALTHY);
        assertThat(this.converter.apply(instance).getServiceState(), is(ServiceState.UNHEALTHY));

        setInstanceMetadata(instance, MetadataKeys.SERVICE_STATE, ServiceState.UNKNOWN);
        assertThat(this.converter.apply(instance).getServiceState(), is(ServiceState.UNKNOWN));
    }

    /**
     * Converter should not freak out if {@link Instance} is missing a network
     * interface (it may do so, for example, early in the boot process).
     */
    @Test
    public void missingNetworkInterface() {
        Instance instance = instance("instances/instance.json");
        instance.setNetworkInterfaces(null);

        assertThat(this.converter.apply(instance).getPrivateIps(), is(Collections.emptyList()));
        assertThat(this.converter.apply(instance).getPublicIps(), is(Collections.emptyList()));
    }

    /**
     * Converter should not freak out if {@link Instance} is missing a private
     * IP (it may do so, for example, early in the boot process).
     */
    @Test
    public void missingPrivateIp() {
        Instance instance = instance("instances/instance.json");
        instance.getNetworkInterfaces().get(0).setNetworkIP(null);

        assertThat(this.converter.apply(instance).getPrivateIps(), is(Collections.emptyList()));
    }

    /**
     * Converter should not freak out if {@link Instance} is missing a public IP
     * (it may do so, for example, early in the boot process).
     */
    @Test
    public void missingPublicIp() {
        Instance instance = instance("instances/instance.json");
        instance.getNetworkInterfaces().get(0).getAccessConfigs().get(0).setNatIP(null);

        assertThat(this.converter.apply(instance).getPublicIps(), is(Collections.emptyList()));
    }

    /**
     * Converter should not freak out if {@link Instance} is missing a public IP
     * (it may do so, for example, early in the boot process).
     */
    @Test
    public void missingPublicIpAccessConfig() {
        Instance instance = instance("instances/instance.json");
        instance.getNetworkInterfaces().get(0).setAccessConfigs(null);

        assertThat(this.converter.apply(instance).getPublicIps(), is(Collections.emptyList()));
    }

    private static void setInstanceMetadata(Instance instance, String key, Object value) {
        Map<String, String> metadataMap = MetadataUtil.toMap(instance.getMetadata());
        metadataMap.put(key, JsonUtils.toString(JsonUtils.toJson(value)));
        instance.getMetadata().setItems(MetadataUtil.toItems(metadataMap));
    }

    public static Instance instance(String resourcePath) {
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        try {
            JsonParser parser = jsonFactory
                    .createJsonParser(new FileInputStream(Resources.getResource(resourcePath).getFile()));
            return parser.parse(Instance.class);
        } catch (Exception e) {
            throw new RuntimeException("failed to load Instance json file " + resourcePath + ": " + e.getMessage(), e);
        }
    }
}
