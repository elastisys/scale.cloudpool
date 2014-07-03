package com.elastisys.scale.cloudadapters.aws.commons.client.lab;

import java.util.List;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import com.elastisys.scale.cloudadapters.aws.commons.requests.autoscaling.GetAutoScalingGroups;
import com.google.common.collect.Lists;

public class ListAutoScalingGroupsMain extends AbstractClient {

	// TODO: set to region where auto-scaling group(s) are hosted
	private static final String region = "us-east-1";

	public static void main(String[] args) throws Exception {
		List<AutoScalingGroup> autoScalingGroups = new GetAutoScalingGroups(
				new PropertiesCredentials(credentialsFile), region).call();
		for (AutoScalingGroup group : autoScalingGroups) {
			logger.info("auto-scaling group: " + group);
			logger.info(String.format("  %s: min: %d, max: %d, desired: %d",
					group.getAutoScalingGroupName(), group.getMinSize(),
					group.getMaxSize(), group.getDesiredCapacity()));
			logger.info(String.format("    instances: %s", instanceIds(group)));
		}
	}

	private static List<String> instanceIds(AutoScalingGroup group) {
		List<String> ids = Lists.newArrayList();
		for (Instance instance : group.getInstances()) {
			ids.add(instance.getInstanceId());
		}
		return ids;
	}
}
