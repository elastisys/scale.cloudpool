package com.elastisys.scale.cloudadapters.aws.commons.client.lab;

import static java.lang.String.format;

import java.util.List;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.elastisys.scale.cloudadapters.aws.commons.requests.ec2.GetInstances;
import com.elastisys.scale.cloudadapters.aws.commons.requests.ec2.TerminateInstance;

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
				region, filter).call();
		for (Instance instance : runningInstances) {
			logger.debug("  Terminating instance " + instance);
			InstanceStateChange stateChange = new TerminateInstance(
					awsCredentials, region, instance.getInstanceId()).call();
			logger.debug("  Result: " + stateChange);
		}

	}
}
