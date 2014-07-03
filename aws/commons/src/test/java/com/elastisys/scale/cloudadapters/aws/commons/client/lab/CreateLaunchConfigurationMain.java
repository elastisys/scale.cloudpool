package com.elastisys.scale.cloudadapters.aws.commons.client.lab;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.List;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.autoscaling.model.AlreadyExistsException;
import com.elastisys.scale.cloudadapters.aws.commons.requests.autoscaling.CreateLaunchConfiguration;

public class CreateLaunchConfigurationMain extends AbstractClient {
	// TODO: set to the name of the auto-scaling launch configuration create
	private static final String launchConfigurationName = "end2endtest-launch-config";

	// TODO: set to region where you want the launch configuration created
	private static final String region = "us-east-1";

	// TODO: set to one of your EC2 security groups
	private static final List<String> securityGroups = Arrays
			.asList("webserver");
	// TODO: set to one of your EC2 key pairs
	private static final String keyPair = "end2end_keypair";

	// TODO: set to an ec2 instance type
	private static final String instanceType = "t1.micro";
	// TODO: set to an AMI (amazon machine image) id
	private static final String imageId = "ami-018c9568";
	// TODO: set to true for detailed (one-minute) CloudWatch monitoring
	private static final boolean detailedMonitoring = true;

	// TODO: set to a user data boot script (either as String or File)
	private static final String bootScript = "#!/bin/bash" + "\n"
			+ "sudo apt-get update && "
			+ "sudo apt-get install -y --force-yes apache2";

	public static void main(String[] args) throws Exception {
		try {
			logger.info(format(
					"Creating launch configuration '%s' with boot script:\n%s",
					launchConfigurationName, bootScript));
			new CreateLaunchConfiguration(new PropertiesCredentials(
					credentialsFile), region, launchConfigurationName,
					securityGroups, keyPair, instanceType, imageId, bootScript,
					detailedMonitoring).call();
		} catch (AlreadyExistsException e) {
			logger.warn(format("Ignoring creation of launch configuration %s,"
					+ " which already exists.", launchConfigurationName));
		}
	}
}
