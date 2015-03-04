package com.elastisys.scale.cloudpool.aws.ec2.driver;

import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.aws.ec2.driver.Ec2PoolDriverConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.AlertSettings;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.MailServerSettings;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleInConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.CloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.collect.Lists;

public class TestUtils {

	public static BaseCloudPoolConfig config(String scalingGroupName) {
		Ec2PoolDriverConfig awsApiConfig = new Ec2PoolDriverConfig(
				"awsAccessKeyId", "awsSecretAccessKey", "eu-west-1");
		CloudPoolConfig scalingGroupConfig = new CloudPoolConfig(
				scalingGroupName, JsonUtils.toJson(awsApiConfig)
						.getAsJsonObject());
		ScaleOutConfig scaleUpConfig = new ScaleOutConfig("m1.small", "",
				"instancekey", Arrays.asList("webserver"), Arrays.asList(
						"#!/bin/bash", "sudo apt-get update -qy",
						"sudo apt-get install apache2 -qy"));
		ScaleInConfig scaleDownConfig = new ScaleInConfig(
				VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, 300);
		AlertSettings alertSettings = new AlertSettings(
				"AwsAsScalingGroup alert",
				Arrays.asList("receiver@destination.com"),
				"INFO|WARN|ERROR|FATAL", "noreply@elastisys.com",
				new MailServerSettings("smtp.host.com", 25, null, false));
		Integer poolUpdatePeriod = 60;
		return new BaseCloudPoolConfig(scalingGroupConfig, scaleUpConfig,
				scaleDownConfig, alertSettings, poolUpdatePeriod);
	}

	public static List<Instance> ec2Instances(Instance... instances) {
		return Lists.newArrayList(instances);
	}

	public static Instance ec2Instance(String id, String state, List<Tag> tags) {
		return new Instance().withInstanceId(id)
				.withState(new InstanceState().withName(state)).withTags(tags);
	}
}
