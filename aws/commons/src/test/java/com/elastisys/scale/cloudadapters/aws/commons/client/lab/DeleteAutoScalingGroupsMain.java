package com.elastisys.scale.cloudadapters.aws.commons.client.lab;

import java.util.List;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import com.elastisys.scale.cloudadapters.aws.commons.requests.autoscaling.DeleteAutoScalingGroup;
import com.elastisys.scale.cloudadapters.aws.commons.requests.autoscaling.GetAutoScalingGroups;
import com.google.common.collect.Lists;

public class DeleteAutoScalingGroupsMain extends AbstractClient {

	// TODO: set to region where auto-scaling group(s) are hosted
	private static final String region = "us-east-1";

	public static void main(String[] args) throws Exception {
		logger.info("deleting auto-scaling groups in region " + region);

		PropertiesCredentials awsCredentials = new PropertiesCredentials(
				credentialsFile);
		List<AutoScalingGroup> autoScalingGroups = new GetAutoScalingGroups(
				awsCredentials, region).call();
		for (AutoScalingGroup group : autoScalingGroups) {
			logger.info("deleting auto-scaling group: " + group);
			logger.info(String.format("  with instances: %s",
					instanceIds(group)));
			new DeleteAutoScalingGroup(awsCredentials, region,
					group.getAutoScalingGroupName()).call();
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
