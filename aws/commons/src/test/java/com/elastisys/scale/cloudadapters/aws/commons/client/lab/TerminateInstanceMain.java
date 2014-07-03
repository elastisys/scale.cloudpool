package com.elastisys.scale.cloudadapters.aws.commons.client.lab;

import static java.lang.String.format;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.elastisys.scale.cloudadapters.aws.commons.requests.ec2.TerminateInstance;

public class TerminateInstanceMain extends AbstractClient {

	// TODO: set to region where machine to terminate is hosted
	private static final String region = "us-east-1";
	// TODO: set to instance id of machine to terminate
	private static final String instanceId = "i-6f1e490d";

	public static void main(String[] args) throws Exception {
		logger.info(format("Terminating instance %s in region %s", instanceId,
				region));
		InstanceStateChange stateChange = new TerminateInstance(
				new PropertiesCredentials(credentialsFile), region, instanceId)
				.call();
		logger.info("Terminating instances: " + stateChange);
	}

}
