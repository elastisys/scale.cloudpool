package com.elastisys.scale.cloudpool.azure.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Exercise {@link NetworkSettings}.
 */
public class TestNetworkSettings {

    /** Sample virtual network name. */
    private static final String VIRTUAL_NETWORK = "myapp-net";
    /** Sample subnet name. */
    private static final String SUBNET = "default";
    /** Sample security groups. */
    private static final List<String> SECURITY_GROUPS = Arrays.asList("ssh", "web");

    /**
     * Only virtualNetwork and subnet are mandatory.
     */
    @Test
    public void onlyMandatoryArgs() {
        Boolean assignPublicIp = null;
        List<String> networkSecurityGroups = null;
        NetworkSettings settings = new NetworkSettings(VIRTUAL_NETWORK, SUBNET, assignPublicIp, networkSecurityGroups);
        settings.validate();

        assertThat(settings.getVirtualNetwork(), is(VIRTUAL_NETWORK));
        assertThat(settings.getSubnetName(), is(SUBNET));

        // check defaults
        assertThat(settings.getAssignPublicIp(), is(NetworkSettings.DEFAULT_ASSIGN_PUBLIC_IP));
        assertThat(settings.getNetworkSecurityGroups(), is(Collections.emptyList()));
    }

    /**
     * Should be possible to specify values for all arguments.
     */
    @Test
    public void specifyAllArgs() {
        Boolean assignPublicIp = true;
        NetworkSettings settings = new NetworkSettings(VIRTUAL_NETWORK, SUBNET, assignPublicIp, SECURITY_GROUPS);
        settings.validate();

        assertThat(settings.getVirtualNetwork(), is(VIRTUAL_NETWORK));
        assertThat(settings.getSubnetName(), is(SUBNET));
        assertThat(settings.getAssignPublicIp(), is(true));
        assertThat(settings.getNetworkSecurityGroups(), is(SECURITY_GROUPS));
    }

    /**
     * virtualNetwork is mandatory.
     */
    @Test
    public void missingVirtualNetwork() {
        try {
            new NetworkSettings(null, SUBNET, true, SECURITY_GROUPS).validate();
            fail("should fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("virtualNetwork"));
        }
    }

    /**
     * subnet is mandatory.
     */
    @Test
    public void missingSubnet() {
        try {
            new NetworkSettings(VIRTUAL_NETWORK, null, true, SECURITY_GROUPS).validate();
            fail("should fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("subnetName"));
        }
    }

}
