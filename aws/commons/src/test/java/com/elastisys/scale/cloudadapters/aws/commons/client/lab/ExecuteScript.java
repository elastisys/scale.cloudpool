package com.elastisys.scale.cloudadapters.aws.commons.client.lab;

import com.elastisys.scale.commons.net.ssh.SshCommandRequester;

public class ExecuteScript extends AbstractClient {

	// TODO: set to the host name that you wish to ssh to
	private static final String hostname = "ec2-50-16-36-185.compute-1.amazonaws.com";
	// TODO: set to script/commands to execute on machine
	private static final String script = "ls -al";

	// TODO: set to user name used to log in to node
	private static final String username = "ubuntu";
	// TODO: set to user's private key used to log in to node
	private static final String privateKeyPath = System
			.getenv("EC2_INSTANCE_KEY");

	public static void main(String[] args) throws Exception {
		// TODO: add retry behavior
		SshCommandRequester requester = new SshCommandRequester(hostname, 22,
				username, privateKeyPath, script, 5000, 10000);
		logger.info(requester.call().toString());
	}
}
