package com.elastisys.scale.cloudpool.aws.ec2.driver;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.CloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.PoolFetchConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.PoolUpdateConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.RetriesConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleInConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.multiplexing.AlertersConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.elastisys.scale.commons.net.smtp.SmtpClientConfig;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.common.collect.Lists;

public class TestUtils {

	public static BaseCloudPoolConfig config(String scalingGroupName) {
		Ec2PoolDriverConfig awsApiConfig = new Ec2PoolDriverConfig(
				"awsAccessKeyId", "awsSecretAccessKey", "eu-west-1");
		CloudPoolConfig scalingGroupConfig = new CloudPoolConfig(
				scalingGroupName,
				JsonUtils.toJson(awsApiConfig).getAsJsonObject());
		String encodedUserData = Base64Utils.toBase64("#!/bin/bash",
				"sudo apt-get update -qy", "sudo apt-get install -qy apache2");
		ScaleOutConfig scaleUpConfig = new ScaleOutConfig("m1.small", "",
				"instancekey", Arrays.asList("webserver"), encodedUserData);
		ScaleInConfig scaleDownConfig = new ScaleInConfig(
				VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, 300);
		SmtpAlerterConfig smtpAlerter = new SmtpAlerterConfig(
				Arrays.asList("receiver@destination.com"),
				"noreply@elastisys.com", "cloud pool alert!",
				"INFO|WARN|ERROR|FATAL",
				new SmtpClientConfig("smtp.host.com", 25, null, false));
		List<HttpAlerterConfig> httpAlerters = Arrays.asList();
		AlertersConfig alertSettings = new AlertersConfig(
				Arrays.asList(smtpAlerter), httpAlerters);

		TimeInterval refreshInterval = new TimeInterval(30L, TimeUnit.SECONDS);
		TimeInterval reachabilityTimeout = new TimeInterval(5L,
				TimeUnit.MINUTES);
		PoolFetchConfig poolFetch = new PoolFetchConfig(
				new RetriesConfig(3, new TimeInterval(2L, TimeUnit.SECONDS)),
				refreshInterval, reachabilityTimeout);
		PoolUpdateConfig poolUpdate = new PoolUpdateConfig(
				new TimeInterval(60L, TimeUnit.SECONDS));
		return new BaseCloudPoolConfig(scalingGroupConfig, scaleUpConfig,
				scaleDownConfig, alertSettings, poolFetch, poolUpdate);
	}

	public static List<Instance> ec2Instances(Instance... instances) {
		return Lists.newArrayList(instances);
	}

	public static Instance ec2Instance(String id, String state,
			List<Tag> tags) {
		return new Instance().withInstanceId(id)
				.withInstanceType(InstanceType.M1Small)
				.withState(new InstanceState().withName(state)).withTags(tags);
	}
}
