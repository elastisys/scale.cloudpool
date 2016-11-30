package com.elastisys.scale.cloudpool.commons.basepool.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.gson.JsonObject;

/**
 * Exercise {@link ScaleOutConfig}.
 */
public class TestScaleOutConfig {

    @Test
    public void basicSanity() {
        List<String> bootScript = Arrays.asList("#!/bin/bash", "apt-get update -qy && apt-get isntall apache2 -qy");
        String encodedUserData = Base64Utils.toBase64(bootScript);
        List<String> securityGroups = Arrays.asList("webserver");
        ScaleOutConfig config = new ScaleOutConfig("m1.small", "ami-124567", "mykeypair", securityGroups,
                encodedUserData);
        config.validate();

        assertThat(config.getSize(), is("m1.small"));
        assertThat(config.getImage(), is("ami-124567"));
        assertThat(config.getKeyPair(), is("mykeypair"));
        assertThat(config.getSecurityGroups(), is(securityGroups));
        assertThat(config.getEncodedUserData(), is(encodedUserData));
        assertThat(config.getExtensions(), is(nullValue()));
    }

    /**
     * Only size and image are mandatory.
     */
    @Test
    public void onlyMandatoryArguments() {
        ScaleOutConfig config = new ScaleOutConfig("m1.small", "ami-124567", null, null, null, null);
        config.validate();

        assertThat(config.getSize(), is("m1.small"));
        assertThat(config.getImage(), is("ami-124567"));
        assertThat(config.getKeyPair(), is(nullValue()));
        assertThat(config.getSecurityGroups(), is(Collections.emptyList()));
        assertThat(config.getEncodedUserData(), is(nullValue()));
        assertThat(config.getExtensions(), is(nullValue()));
    }

    /**
     * It should be possible to pass cloud-specific provisioning parameters via
     * the {@code extensions} element.
     */
    @Test
    public void withExtensions() {
        JsonObject extensions = JsonUtils
                .parseJsonString("{\"type\": \"vm\", \"network\": { \"vmnet-id\": 1, \"assign-public-ip\": true}}")
                .getAsJsonObject();
        ScaleOutConfig config = new ScaleOutConfig("m1.small", "ami-124567", null, null, null, extensions);
        config.validate();

        assertThat(config.getExtensions(), is(extensions));
    }

    @Test(expected = CloudPoolException.class)
    public void missingSize() {
        List<String> bootScript = Arrays.asList("#!/bin/bash", "apt-get update -qy && apt-get isntall apache2 -qy");
        String encodedUserData = Base64Utils.toBase64(bootScript);
        List<String> securityGroups = Arrays.asList("webserver");
        ScaleOutConfig config = new ScaleOutConfig(null, "ami-124567", "mykeypair", securityGroups, encodedUserData);
        config.validate();
    }

    @Test(expected = CloudPoolException.class)
    public void missingImage() {
        List<String> bootScript = Arrays.asList("#!/bin/bash", "apt-get update -qy && apt-get isntall apache2 -qy");
        String encodedUserData = Base64Utils.toBase64(bootScript);
        List<String> securityGroups = Arrays.asList("webserver");
        ScaleOutConfig config = new ScaleOutConfig("m1.small", null, "mykeypair", securityGroups, encodedUserData);
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
        ScaleOutConfig config = new ScaleOutConfig("m1.small", "ami-124567", null, securityGroups, encodedUserData);
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
        ScaleOutConfig config = new ScaleOutConfig("m1.small", "ami-124567", "mykeypair", securityGroups,
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
        ScaleOutConfig config = new ScaleOutConfig("m1.small", "ami-124567", "mykeypair", securityGroups,
                encodedUserData);
        config.validate();
    }

}
