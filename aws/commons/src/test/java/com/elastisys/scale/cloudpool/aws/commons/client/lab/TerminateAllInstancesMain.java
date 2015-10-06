package com.elastisys.scale.cloudpool.aws.commons.client.lab;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.util.List;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.elastisys.scale.cloudpool.aws.commons.functions.AwsEc2Functions;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.GetInstances;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.TerminateInstances;
import com.google.common.collect.Lists;

public class TerminateAllInstancesMain extends AbstractClient {

	/** TODO: set to the AWS region to connect to. */
	private static final String region = "us-east-1";

	public static void main(String[] args) throws Exception {
		logger.info(format("Terminating all running instances in region %s",
				region));

		Filter filter = new Filter("instance-state-name").withValues("running");
		PropertiesCredentials awsCredentials = new PropertiesCredentials(
				credentialsFile);
		List<Instance> runningInstances = new GetInstances(awsCredentials,
				region).withFilters(asList(filter)).call();
		List<String> instanceIds = Lists.transform(runningInstances,
				AwsEc2Functions.toInstanceId());
		logger.debug("Terminating instances {}", instanceIds);
		List<InstanceStateChange> stateChanges = new TerminateInstances(
				awsCredentials, region, instanceIds).call();
		logger.debug("Result: " + stateChanges);
	}
}
