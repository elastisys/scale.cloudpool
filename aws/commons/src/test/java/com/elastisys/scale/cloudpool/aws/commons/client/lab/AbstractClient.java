package com.elastisys.scale.cloudpool.aws.commons.client.lab;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractClient {
	static Logger logger = LoggerFactory.getLogger(AbstractClient.class);

	// TODO: point to a file containing 'accessKey' and 'secretKey' properties
	protected static final File credentialsFile = new File(
			System.getenv("HOME") + "/keys/ec2/credentials.properties");

}
