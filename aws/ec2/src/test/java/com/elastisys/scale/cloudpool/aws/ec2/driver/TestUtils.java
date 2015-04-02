package com.elastisys.scale.cloudpool.aws.ec2.driver;

import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.commons.basepool.config.AlertsConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.CloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleInConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.elastisys.scale.commons.net.smtp.SmtpClientConfig;
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
		SmtpAlerterConfig smtpAlerter = new SmtpAlerterConfig(
				Arrays.asList("receiver@destination.com"),
				"noreply@elastisys.com", "cloud pool alert!",
				"INFO|WARN|ERROR|FATAL", new SmtpClientConfig("smtp.host.com",
						25, null, false));
		List<HttpAlerterConfig> httpAlerters = Arrays.asList();
		AlertsConfig alertSettings = new AlertsConfig(
				Arrays.asList(smtpAlerter), httpAlerters);

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
