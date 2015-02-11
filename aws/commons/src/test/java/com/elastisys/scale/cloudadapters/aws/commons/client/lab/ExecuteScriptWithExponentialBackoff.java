package com.elastisys.scale.cloudadapters.aws.commons.client.lab;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.commons.net.retryable.Retryers;
import com.elastisys.scale.commons.net.ssh.SshCommandRequester;
import com.elastisys.scale.commons.net.ssh.SshCommandResult;

public class ExecuteScriptWithExponentialBackoff {
	static Logger logger = LoggerFactory
			.getLogger(ExecuteScriptWithExponentialBackoff.class);

	// TODO: set to the host name that you wish to ssh to
	private static final String hostname = "ec2-54-224-30-116.compute-1.amazonaws.com";
	// TODO: set to script/commands to execute on machine
	private static final String script = "ls -al";

	// TODO: set to user name used to log in to node
	private static final String username = "ubuntu";
	// TODO: set to user's private key used to log in to node
	private static final String privateKeyPath = System
			.getenv("EC2_INSTANCE_KEY");

	public static void main(String[] args) throws Exception {
		Callable<SshCommandResult> requester = new SshCommandRequester(
				hostname, 22, username, privateKeyPath, script, 2000, 2000);
		Callable<SshCommandResult> retryer = Retryers
				.exponentialBackoffRetryer("persistent-ls", requester, 1,
						TimeUnit.SECONDS, 5);

		logger.info("Final response: " + retryer.call());
	}
}
