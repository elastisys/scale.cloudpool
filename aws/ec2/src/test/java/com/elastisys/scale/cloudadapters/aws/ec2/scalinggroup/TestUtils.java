package com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup;

import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.AlertSettings;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.MailServerSettings;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleDownConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScalingGroupConfig;
import com.elastisys.scale.cloudadapters.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.collect.Lists;

public class TestUtils {

	public static BaseCloudAdapterConfig config(String scalingGroupName) {
		Ec2ScalingGroupConfig awsApiConfig = new Ec2ScalingGroupConfig(
				"awsAccessKeyId", "awsSecretAccessKey", "eu-west-1");
		ScalingGroupConfig scalingGroupConfig = new ScalingGroupConfig(
				scalingGroupName, JsonUtils.toJson(awsApiConfig)
						.getAsJsonObject());
		ScaleUpConfig scaleUpConfig = new ScaleUpConfig("m1.small", "",
				"instancekey", Arrays.asList("webserver"), Arrays.asList(
						"#!/bin/bash", "sudo apt-get update -qy",
						"sudo apt-get install apache2 -qy"));
		ScaleDownConfig scaleDownConfig = new ScaleDownConfig(
				VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, 300);
		AlertSettings alertSettings = new AlertSettings(
				"AwsAsScalingGroup alert",
				Arrays.asList("receiver@destination.com"),
				"INFO|WARN|ERROR|FATAL", "noreply@elastisys.com",
				new MailServerSettings("smtp.host.com", 25, null, false));
		Integer poolUpdatePeriod = 60;
		return new BaseCloudAdapterConfig(scalingGroupConfig, scaleUpConfig,
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
