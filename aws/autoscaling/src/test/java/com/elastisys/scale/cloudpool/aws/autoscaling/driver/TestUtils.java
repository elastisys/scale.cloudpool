package com.elastisys.scale.cloudpool.aws.autoscaling.driver;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.LifecycleState;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceType;
import com.elastisys.scale.cloudpool.api.types.Machine;
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
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.common.collect.Lists;

public class TestUtils {

	public static BaseCloudPoolConfig config(String scalingGroupName) {
		AwsAsPoolDriverConfig awsApiConfig = new AwsAsPoolDriverConfig(
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
		AlertsConfig alertSettings = new AlertsConfig(
				Arrays.asList(smtpAlerter), httpAlerters);

		Integer poolUpdatePeriod = 60;
		return new BaseCloudPoolConfig(scalingGroupConfig, scaleUpConfig,
				scaleDownConfig, alertSettings, poolUpdatePeriod);
	}

	public static AutoScalingGroup group(String name,
			LaunchConfiguration launchConfig, int desiredCapacity,
			Collection<com.amazonaws.services.ec2.model.Instance> ec2Instances) {
		AutoScalingGroup autoScalingGroup = new AutoScalingGroup()
				.withAutoScalingGroupName(name)
				.withLaunchConfigurationName(
						launchConfig.getLaunchConfigurationName())
				.withDesiredCapacity(desiredCapacity)
				.withInstances(toAsInstances(ec2Instances));
		return autoScalingGroup;
	}

	public static Collection<Instance> asInstances(Instance... instances) {
		return Lists.newArrayList(instances);
	}

	public static List<com.amazonaws.services.ec2.model.Instance> ec2Instances(
			com.amazonaws.services.ec2.model.Instance... instances) {
		return Lists.newArrayList(instances);
	}

	public static Collection<Machine> machines(Machine... machines) {
		return Lists.newArrayList(machines);
	}

	public static Collection<Instance> toAsInstances(
			Collection<com.amazonaws.services.ec2.model.Instance> ec2Instances) {
		List<Instance> asInstances = Lists.newArrayList();
		for (com.amazonaws.services.ec2.model.Instance ec2Instance : ec2Instances) {
			Instance asInstance = new Instance()
					.withInstanceId(ec2Instance.getInstanceId())
					.withLifecycleState(
							ec2StateToLifecycleState(ec2Instance.getState()));
			asInstances.add(asInstance);
		}
		return asInstances;
	}

	private static LifecycleState ec2StateToLifecycleState(
			InstanceState state) {
		switch (state.getName()) {
		case "pending":
			return LifecycleState.Pending;
		case "running":
			return LifecycleState.InService;
		case "shutting-down":
			return LifecycleState.Terminating;
		case "terminated":
			return LifecycleState.Terminated;
		case "stopping":
			return LifecycleState.Terminating;
		case "stopped":
			return LifecycleState.Terminated;
		default:
			throw new IllegalArgumentException(
					String.format("unrecognized instance state: '%s'", state));
		}
	}

	public static com.amazonaws.services.ec2.model.Instance ec2Instance(
			String id, String state) {
		return new com.amazonaws.services.ec2.model.Instance()
				.withInstanceId(id).withInstanceType(InstanceType.M1Medium)
				.withState(new InstanceState().withName(state));
	}

	public static com.amazonaws.services.ec2.model.Instance spotInstance(
			String spotId, String instanceId, String state) {
		return new com.amazonaws.services.ec2.model.Instance()
				.withInstanceId(instanceId)
				.withInstanceType(InstanceType.M1Medium)
				.withState(new InstanceState().withName(state))
				.withSpotInstanceRequestId(spotId);
	}

}
