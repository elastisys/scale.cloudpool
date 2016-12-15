package com.elastisys.scale.cloudpool.aws.ec2.driver;

import java.util.Arrays;
import java.util.List;

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
    /** Sample keypair. */
    private static final String KEYPAIR = "ssh-loginkey";
    /** Sample security groups. */
    private static final List<String> SECURITY_GROUPS = Arrays.asList("webserver");
    /** Sample base64-encoded user data. */
    private static final String USER_DATA = Base64Utils
            .toBase64(Arrays.asList("#!/bin/bash", "apt-get update -qy && apt-get isntall apache2 -qy"));

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
        Ec2ProvisioningTemplate provisioningTemplate = new Ec2ProvisioningTemplate(INSTANCE_TYPE, AMI, KEYPAIR,
                SECURITY_GROUPS, USER_DATA);

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
