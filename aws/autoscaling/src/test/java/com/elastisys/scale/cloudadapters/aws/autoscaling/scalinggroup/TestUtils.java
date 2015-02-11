package com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.LifecycleState;
import com.amazonaws.services.ec2.model.InstanceState;
import com.elastisys.scale.cloudadapers.api.types.Machine;
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
		AwsAsScalingGroupConfig awsApiConfig = new AwsAsScalingGroupConfig(
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
				"noreply@elastisys.com", "ERROR|FATAL", new MailServerSettings(
						"smtp.host.com", 25, null, false));
		Integer poolUpdatePeriod = 60;
		return new BaseCloudAdapterConfig(scalingGroupConfig, scaleUpConfig,
				scaleDownConfig, alertSettings, poolUpdatePeriod);
	}

	public static AutoScalingGroup group(String name, int desiredCapacity,
			Collection<com.amazonaws.services.ec2.model.Instance> ec2Instances) {
		AutoScalingGroup autoScalingGroup = new AutoScalingGroup()
		.withAutoScalingGroupName(name)
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
			Instance asInstance = new Instance().withInstanceId(
					ec2Instance.getInstanceId()).withLifecycleState(
							ec2StateToLifecycleState(ec2Instance.getState()));
			asInstances.add(asInstance);
		}
		return asInstances;
	}

	private static LifecycleState ec2StateToLifecycleState(InstanceState state) {
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
			throw new IllegalArgumentException(String.format(
					"unrecognized instance state: '%s'", state));
		}
	}

	public static com.amazonaws.services.ec2.model.Instance ec2Instance(
			String id, String state) {
		return new com.amazonaws.services.ec2.model.Instance().withInstanceId(
				id).withState(new InstanceState().withName(state));
	}
}
