package com.elastisys.scale.cloudpool.aws.ec2.driver;

import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2ProvisioningTemplate;
import com.elastisys.scale.cloudpool.aws.ec2.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class TestUtils {
    /** Sample AWS access key id. */
    private static final String ACCESS_KEY_ID = "awsAccessKeyId";
    /** Sample AWS secret access key. */
    private static final String SECRET_ACCESS_KEY = "awsSecretAccessKey";
    /** Sample region */
    private static final String REGION = "us-east-1";

    /** Sample instance type. */
    private static final String INSTANCE_TYPE = "m1.small";
    /** Sample image. */
    private static final String AMI = "ami-12345678";
    private static final List<String> SUBNET_IDS = asList("subnet-44b5786b", "subnet-dcd15f97");
    private static final boolean ASSIGN_PUBLIC_IP = true;
    /** Sample keypair. */
    private static final String KEYPAIR = "ssh-loginkey";
    private static final String IAM_INSTANCE_PROFILE = "arn:aws:iam::123456789012:instance-profile/my-iam-profile";
    /** Sample security groups. */
    private static final List<String> SECURITY_GROUP_IDS = Arrays.asList("sg-12345678");
    /** Sample base64-encoded user data. */
    private static final String USER_DATA = Base64Utils
            .toBase64(Arrays.asList("#!/bin/bash", "apt-get update -qy && apt-get isntall apache2 -qy"));
    private static final boolean EBS_OPTIMIZED = true;
    private static final Map<String, String> TAGS = ImmutableMap.of("Cluster", "mycluster");

    /**
     * Creates a {@link DriverConfig} from a {@link BaseCloudPoolConfig}
     *
     * @param config
     * @return
     */
    public static DriverConfig driverConfig(BaseCloudPoolConfig config) {
        return new DriverConfig(config.getName(), JsonUtils.toJson(config.getCloudApiSettings()).getAsJsonObject(),
                JsonUtils.toJson(config.getProvisioningTemplate()).getAsJsonObject());
    }

    public static DriverConfig driverConfig(String cloudPoolName) {
        CloudApiSettings cloudApiSettings = new CloudApiSettings(ACCESS_KEY_ID, SECRET_ACCESS_KEY, REGION);
        Ec2ProvisioningTemplate provisioningTemplate = new Ec2ProvisioningTemplate(INSTANCE_TYPE, AMI, SUBNET_IDS,
                ASSIGN_PUBLIC_IP, KEYPAIR, IAM_INSTANCE_PROFILE, SECURITY_GROUP_IDS, USER_DATA, EBS_OPTIMIZED, TAGS);

        return new DriverConfig(cloudPoolName, JsonUtils.toJson(cloudApiSettings).getAsJsonObject(),
                JsonUtils.toJson(provisioningTemplate).getAsJsonObject());
    }

    public static List<Instance> ec2Instances(Instance... instances) {
        return Lists.newArrayList(instances);
    }

    public static Instance ec2Instance(String id, String state, List<Tag> tags) {
        return new Instance().withInstanceId(id).withInstanceType(InstanceType.M1Small)
                .withState(new InstanceState().withName(state)).withTags(tags);
    }
}
