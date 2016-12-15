package com.elastisys.scale.cloudpool.aws.commons.poolclient;

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
 * Exercises {@link Ec2ProvisioningTemplate}.
 */
public class TestEc2ProvisioningTemplate {

    /** Sample instance type. */
    private static final String INSTANCE_TYPE = "m1.small";
    /** Sample image. */
    private static final String AMI = "ami-12345678";
    /** Sample keypair. */
    private static final String KEYPAIR = "ssh-loginkey";
    /** Sample security groups. */
    private static final List<String> SECURITY_GROUPS = Arrays.asList("webserver");
    /** Sample base64-encoded user data. */
    private static final String USER_DATA = Base64Utils
            .toBase64(Arrays.asList("#!/bin/bash", "apt-get update -qy && apt-get isntall apache2 -qy"));

    @Test
    public void basicSanity() {
        Ec2ProvisioningTemplate config = new Ec2ProvisioningTemplate(INSTANCE_TYPE, AMI, KEYPAIR, SECURITY_GROUPS,
                USER_DATA);
        config.validate();

        assertThat(config.getSize(), is(INSTANCE_TYPE));
        assertThat(config.getImage(), is(AMI));
        assertThat(config.getKeyPair(), is(KEYPAIR));
        assertThat(config.getSecurityGroups(), is(SECURITY_GROUPS));
        assertThat(config.getEncodedUserData(), is(USER_DATA));
    }

    /**
     * Only size and image are mandatory.
     */
    @Test
    public void onlyMandatoryArguments() {
        Ec2ProvisioningTemplate config = new Ec2ProvisioningTemplate(INSTANCE_TYPE, AMI, null, null, null);
        config.validate();

        assertThat(config.getSize(), is(INSTANCE_TYPE));
        assertThat(config.getImage(), is(AMI));

        assertThat(config.getKeyPair(), is(nullValue()));
        assertThat(config.getSecurityGroups(), is(Collections.emptyList()));
        assertThat(config.getEncodedUserData(), is(nullValue()));
    }

    @Test
    public void missingSize() {
        try {
            new Ec2ProvisioningTemplate(null, AMI, KEYPAIR, SECURITY_GROUPS, USER_DATA).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("size"));
        }
    }

    @Test
    public void missingImage() {
        try {
            new Ec2ProvisioningTemplate(INSTANCE_TYPE, null, KEYPAIR, SECURITY_GROUPS, USER_DATA).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("image"));
        }
    }

}
