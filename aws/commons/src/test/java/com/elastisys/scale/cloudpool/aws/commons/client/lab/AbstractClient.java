package com.elastisys.scale.cloudpool.aws.commons.client.lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;

public abstract class AbstractClient {

	static Logger logger = LoggerFactory.getLogger(AbstractClient.class);

	// TODO: set environment variables
	private static final String AWS_ACCESS_KEY_ID = System
			.getenv("AWS_ACCESS_KEY_ID");
	private static final String AWS_SECRET_ACCESS_KEY = System
			.getenv("AWS_SECRET_ACCESS_KEY");

	protected static final AWSCredentials AWS_CREDENTIALS = new BasicAWSCredentials(
			AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY);

}
