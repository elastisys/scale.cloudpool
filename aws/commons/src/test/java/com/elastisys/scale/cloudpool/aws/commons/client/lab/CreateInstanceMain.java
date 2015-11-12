package com.elastisys.scale.cloudpool.aws.commons.client.lab;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.List;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.CreateInstances;

import jersey.repackaged.com.google.common.collect.Iterables;

public class CreateInstanceMain extends AbstractClient {

	// TODO: set to region where you want machine to be hosted
	private static final String region = "us-east-1";
	// TODO: set to zone (within region) to place machine in
	private static final String availabilityZone = "us-east-1a";

	// TODO: set to one of your EC2 security groups
	private static final List<String> securityGroups = Arrays
			.asList("webserver");
	// TODO: set to one of your EC2 key pairs
	private static final String keyPair = System.getenv("EC2_KEYPAIR");

	// TODO: set to an ec2 instance type
	private static final String instanceType = "m1.small";
	// TODO: set to an AMI (amazon machine image) id
	private static final String imageId = "ami-018c9568";

	// TODO: set to a user data boot script
	private static final String bootScript = "#!/bin/bash" + "\n"
			+ "sudo apt-get update && "
			+ "sudo apt-get install -y --force-yes apache2";

	public static void main(String[] args) throws Exception {
		logger.info(
				format("Starting instance in region %s, availability zone %s",
						region, availabilityZone));

		CreateInstances request = new CreateInstances(AWS_CREDENTIALS, region,
				new ClientConfiguration(), availabilityZone, securityGroups,
				keyPair, instanceType, imageId, bootScript, 1,
				Arrays.asList(new Tag("Name", "myinstance")));
		List<Instance> instances = request.call();
		Instance instance = Iterables.getOnlyElement(instances);
		logger.info("Launched instance : " + instance.getInstanceId() + ": "
				+ instance.getState());
	}
}
