package com.elastisys.scale.cloudpool.aws.commons.poolclient;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2ScaleOutConfig;
import com.elastisys.scale.commons.util.base64.Base64Utils;

/**
 * Exercises {@link Ec2ScaleOutConfig}.
 */
public class TestEc2ScaleOutConfig {
    @Test
    public void basicSanity() {
        List<String> bootScript = Arrays.asList("#!/bin/bash", "apt-get update -qy && apt-get isntall apache2 -qy");
        String encodedUserData = Base64Utils.toBase64(bootScript);
        List<String> securityGroups = Arrays.asList("webserver");
        Ec2ScaleOutConfig config = new Ec2ScaleOutConfig("m1.small", "ami-124567", "mykeypair", securityGroups,
                encodedUserData);
        config.validate();

        assertThat(config.getSize(), is("m1.small"));
        assertThat(config.getImage(), is("ami-124567"));
        assertThat(config.getKeyPair(), is("mykeypair"));
        assertThat(config.getSecurityGroups(), is(securityGroups));
        assertThat(config.getEncodedUserData(), is(encodedUserData));
    }

    /**
     * Only size and image are mandatory.
     */
    @Test
    public void onlyMandatoryArguments() {
        Ec2ScaleOutConfig config = new Ec2ScaleOutConfig("m1.small", "ami-124567", null, null, null);
        config.validate();

        assertThat(config.getSize(), is("m1.small"));
        assertThat(config.getImage(), is("ami-124567"));
        assertThat(config.getKeyPair(), is(nullValue()));
        assertThat(config.getSecurityGroups(), is(Collections.emptyList()));
        assertThat(config.getEncodedUserData(), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingSize() {
        List<String> bootScript = Arrays.asList("#!/bin/bash", "apt-get update -qy && apt-get isntall apache2 -qy");
        String encodedUserData = Base64Utils.toBase64(bootScript);
        List<String> securityGroups = Arrays.asList("webserver");
        Ec2ScaleOutConfig config = new Ec2ScaleOutConfig(null, "ami-124567", "mykeypair", securityGroups,
                encodedUserData);
        config.validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingImage() {
        List<String> bootScript = Arrays.asList("#!/bin/bash", "apt-get update -qy && apt-get isntall apache2 -qy");
        String encodedUserData = Base64Utils.toBase64(bootScript);
        List<String> securityGroups = Arrays.asList("webserver");
        Ec2ScaleOutConfig config = new Ec2ScaleOutConfig("m1.small", null, "mykeypair", securityGroups,
                encodedUserData);
        config.validate();
    }

    /**
     * It's okay to specify no key pair.
     */
    @Test
    public void missingKeypair() {
        List<String> bootScript = Arrays.asList("#!/bin/bash", "apt-get update -qy && apt-get isntall apache2 -qy");
        String encodedUserData = Base64Utils.toBase64(bootScript);
        List<String> securityGroups = Arrays.asList("webserver");
        Ec2ScaleOutConfig config = new Ec2ScaleOutConfig("m1.small", "ami-124567", null, securityGroups,
                encodedUserData);
        config.validate();
    }

    /**
     * It's okay to specify no security groups.
     */
    @Test
    public void missingSecurityGroups() {
        List<String> bootScript = Arrays.asList("#!/bin/bash", "apt-get update -qy && apt-get isntall apache2 -qy");
        String encodedUserData = Base64Utils.toBase64(bootScript);
        List<String> securityGroups = null;
        Ec2ScaleOutConfig config = new Ec2ScaleOutConfig("m1.small", "ami-124567", "mykeypair", securityGroups,
                encodedUserData);
        config.validate();
    }

    /**
     * It's okay to specify no user data.
     */
    @Test
    public void missingEncodedUserData() {
        String encodedUserData = null;
        List<String> securityGroups = Arrays.asList("webserver");
        Ec2ScaleOutConfig config = new Ec2ScaleOutConfig("m1.small", "ami-124567", "mykeypair", securityGroups,
                encodedUserData);
        config.validate();
    }
}
