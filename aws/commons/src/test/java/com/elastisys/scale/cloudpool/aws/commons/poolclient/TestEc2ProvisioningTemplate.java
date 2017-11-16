package com.elastisys.scale.cloudpool.aws.commons.poolclient;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.amazonaws.services.ec2.model.InstanceType;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.common.collect.ImmutableMap;

/**
 * Exercises {@link Ec2ProvisioningTemplate}.
 */
public class TestEc2ProvisioningTemplate {

    /** Sample instance type. */
    private static final String INSTANCE_TYPE = "m1.small";
    /** Sample image. */
    private static final String AMI = "ami-12345678";
    /** Sample subnet ids. */
    private static final List<String> SUBNET_IDS = Arrays.asList("subnet-1233456a", "subnet-123456b");

    /** Sample keypair. */
    private static final String KEYPAIR = "ssh-loginkey";
    /** Sample IAM instance profile. */
    private static final String INSTANCE_PROFILE_ARN = "arn:aws:iam::123456789012:instance-profile/my-iam-profile";
    /** Sample assign public ip. */
    private static final boolean ASSIGN_PUBLIC_IP = true;
    /** Sample security group ids. */
    private static final List<String> SECURITY_GROUP_IDS = Arrays.asList("sg-cc5957ab");
    /** Sample base64-encoded user data. */
    private static final String USER_DATA = Base64Utils
            .toBase64(Arrays.asList("#!/bin/bash", "apt-get update -qy && apt-get isntall apache2 -qy"));
    private static final boolean EBS_OPTIMIZED = false;
    private static final Map<String, String> TAGS = ImmutableMap.of("k", "v");

    @Test
    public void basicSanity() {
        Ec2ProvisioningTemplate config = new Ec2ProvisioningTemplate(INSTANCE_TYPE, AMI, SUBNET_IDS, ASSIGN_PUBLIC_IP,
                KEYPAIR, INSTANCE_PROFILE_ARN, SECURITY_GROUP_IDS, USER_DATA, EBS_OPTIMIZED, TAGS);
        config.validate();

        assertThat(config.getInstanceType(), is(InstanceType.fromValue(INSTANCE_TYPE)));
        assertThat(config.getAmiId(), is(AMI));
        assertThat(config.getSubnetIds(), is(SUBNET_IDS));
        assertThat(config.isAssignPublicIp(), is(ASSIGN_PUBLIC_IP));
        assertThat(config.getKeyPair(), is(KEYPAIR));
        assertThat(config.getIamInstanceProfileARN(), is(INSTANCE_PROFILE_ARN));
        assertThat(config.getSecurityGroupIds(), is(SECURITY_GROUP_IDS));
        assertThat(config.getEncodedUserData(), is(USER_DATA));
        assertThat(config.isEbsOptimized(), is(EBS_OPTIMIZED));
        assertThat(config.getTags(), is(TAGS));
    }

    /**
     * Only instanceType, amiId, and subnetIds are mandatory.
     */
    @Test
    public void onlyMandatoryArguments() {
        Ec2ProvisioningTemplate config = new Ec2ProvisioningTemplate(INSTANCE_TYPE, AMI, SUBNET_IDS, null, null, null,
                null, null, null, null);
        config.validate();

        assertThat(config.getInstanceType(), is(InstanceType.fromValue(INSTANCE_TYPE)));
        assertThat(config.getAmiId(), is(AMI));
        assertThat(config.getSubnetIds(), is(SUBNET_IDS));

        assertThat(config.isAssignPublicIp(), is(false));
        assertThat(config.getKeyPair(), is(nullValue()));
        assertThat(config.getIamInstanceProfileARN(), is(nullValue()));
        assertThat(config.getSecurityGroupIds(), is(Collections.emptyList()));
        assertThat(config.getEncodedUserData(), is(nullValue()));
        assertThat(config.isEbsOptimized(), is(false));
        assertThat(config.getTags(), is(Collections.emptyMap()));
    }

    @Test
    public void missingInstanceType() {
        try {
            new Ec2ProvisioningTemplate(null, AMI, SUBNET_IDS, ASSIGN_PUBLIC_IP, KEYPAIR, INSTANCE_PROFILE_ARN,
                    SECURITY_GROUP_IDS, USER_DATA, EBS_OPTIMIZED, TAGS).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("instanceType"));
        }
    }

    @Test
    public void unrecognizedInstanceType() {
        try {
            String badInstanceType = "m1.notfound";
            new Ec2ProvisioningTemplate(badInstanceType, AMI, SUBNET_IDS, ASSIGN_PUBLIC_IP, KEYPAIR,
                    INSTANCE_PROFILE_ARN, SECURITY_GROUP_IDS, USER_DATA, EBS_OPTIMIZED, TAGS).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("unrecognized"));
        }
    }

    @Test
    public void missingAmiId() {
        try {
            new Ec2ProvisioningTemplate(INSTANCE_TYPE, null, SUBNET_IDS, ASSIGN_PUBLIC_IP, KEYPAIR,
                    INSTANCE_PROFILE_ARN, SECURITY_GROUP_IDS, USER_DATA, EBS_OPTIMIZED, TAGS).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("amiId"));
        }
    }

    @Test
    public void missingSubnetIds() {
        try {
            new Ec2ProvisioningTemplate(INSTANCE_TYPE, AMI, null, ASSIGN_PUBLIC_IP, KEYPAIR, INSTANCE_PROFILE_ARN,
                    SECURITY_GROUP_IDS, USER_DATA, EBS_OPTIMIZED, TAGS).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("missing subnetIds"));
        }
    }

    @Test
    public void emptySubnetIds() {
        try {
            List<String> noSubnetIds = Collections.emptyList();
            new Ec2ProvisioningTemplate(INSTANCE_TYPE, AMI, noSubnetIds, ASSIGN_PUBLIC_IP, KEYPAIR,
                    INSTANCE_PROFILE_ARN, SECURITY_GROUP_IDS, USER_DATA, EBS_OPTIMIZED, TAGS).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("at least one subnet id must be specified"));
        }
    }

}
