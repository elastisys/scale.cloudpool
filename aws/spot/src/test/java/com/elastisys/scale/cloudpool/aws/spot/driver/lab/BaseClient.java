package com.elastisys.scale.cloudpool.aws.spot.driver.lab;

public class BaseClient {
	protected static final String awsAccessKeyId = System
			.getenv("AWS_ACCESS_KEY_ID");
	protected static final String awsSecretAccessKey = System
			.getenv("AWS_SECRET_ACCESS_KEY");

	// TODO: set to the region you wish to operate against
	protected static final String region = "us-east-1";

}
