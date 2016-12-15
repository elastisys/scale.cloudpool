package com.elastisys.scale.cloudpool.openstack.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.elastisys.scale.commons.util.base64.Base64Utils;

/**
 * Exercises {@link ProvisioningTemplate}.
 */
public class TestProvisioningTemplate {

    private static final String SERVER_IMAGE = "ubuntu-16.04";
    private static final String SERVER_SIZE = "m1.small";
    private static final String KEYPAIR = "ssh-loginkey";
    private static final List<String> SECURITY_GROUPS = Arrays.asList("webserver");
    private static final String USER_DATA = Base64Utils
            .toBase64(Arrays.asList("#!/bin/bash", "apt-get update -qy && apt-get isntall apache2 -qy"));

    /**
     * Verify that all parameters can be explicitly given.
     */
    @Test
    public void onCompleteArgs() {
        List<String> networks = Arrays.asList("private");
        boolean assignFloatingIp = true;
        ProvisioningTemplate config = new ProvisioningTemplate(SERVER_SIZE, SERVER_IMAGE, KEYPAIR, SECURITY_GROUPS,
                USER_DATA, networks, assignFloatingIp);
        config.validate();

        assertThat(config.getSize(), is(SERVER_SIZE));
        assertThat(config.getImage(), is(SERVER_IMAGE));
        assertThat(config.getKeyPair(), is(KEYPAIR));
        assertThat(config.getSecurityGroups(), is(SECURITY_GROUPS));
        assertThat(config.getEncodedUserData(), is(USER_DATA));
        assertThat(config.getNetworks(), is(networks));
        assertThat(config.isAssignFloatingIp(), is(assignFloatingIp));
    }

    /**
     * Only size and image are mandatory. Others are not required or have
     * defaults.
     */
    @Test
    public void onlyMandatoryArguments() {
        ProvisioningTemplate config = new ProvisioningTemplate(SERVER_SIZE, SERVER_IMAGE, null, null, null);
        config.validate();

        assertThat(config.getSize(), is(SERVER_SIZE));
        assertThat(config.getImage(), is(SERVER_IMAGE));

        assertThat(config.getKeyPair(), is(nullValue()));
        assertThat(config.getSecurityGroups(), is(Collections.emptyList()));
        assertThat(config.getEncodedUserData(), is(nullValue()));
        assertThat(config.getNetworks(), is(Collections.emptyList()));
        assertThat(config.isAssignFloatingIp(), is(ProvisioningTemplate.DEFAULT_ASSIGN_FLOATING_IP));
    }

    @Test
    public void missingSize() {
        try {
            ProvisioningTemplate config = new ProvisioningTemplate(null, SERVER_IMAGE, KEYPAIR, SECURITY_GROUPS,
                    USER_DATA);
            config.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("size"));
        }
    }

    @Test
    public void missingImage() {
        try {
            ProvisioningTemplate config = new ProvisioningTemplate(SERVER_SIZE, null, KEYPAIR, SECURITY_GROUPS,
                    USER_DATA);
            config.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("image"));
        }

    }

}
