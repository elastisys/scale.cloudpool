package com.elastisys.scale.cloudadapters.commons.adapter.tasks.lab;

import java.util.Arrays;

import org.joda.time.DateTime;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.impl.StandardSshLivenessTest;
import com.elastisys.scale.commons.net.ssh.SshCommandResult;
import com.google.gson.JsonObject;

/**
 * Lab program for running a {@link StandardSshLivenessTest}.
 *
 */
public class LivenessTestMain {

	/** TODO: set to hostname or IP address */
	private static final String SSH_HOST = "<IP address>";
	/** TODO: set to login user. */
	private static final String LOGIN_USER = "ubuntu";
	/** TODO: set to private key path. */
	private static final String PRIVATE_KEY_PATH = System
			.getenv("OS_INSTANCE_KEYPATH");

	/** TODO: set to liveness test command. */
	private static final String LIVENESS_COMMAND = "service apache2 status | grep running";

	public static void main(String[] args) throws Exception {
		StandardSshLivenessTest task = new StandardSshLivenessTest(new Machine(
				"sshserver", MachineState.RUNNING, new DateTime(),
				Arrays.asList(SSH_HOST), null, new JsonObject()), 22,
				LOGIN_USER, PRIVATE_KEY_PATH, LIVENESS_COMMAND, 3, 5,
				"liveness-test");
		SshCommandResult result = task.call();
		System.out.println("liveness test successful: "
				+ result.getExitStatus());

	}
}
