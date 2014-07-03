package com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.SshLivenessTest;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;
import com.elastisys.scale.commons.net.retryable.retryhandlers.RetrySshCommand;
import com.elastisys.scale.commons.net.ssh.SshCommandRequester;
import com.elastisys.scale.commons.net.ssh.SshCommandResult;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

/**
 * A {@link SshLivenessTest} that, when executed, attempts a limited number of
 * times to execute a liveness test SSH command against a {@link Machine}.
 * <p/>
 * If the command succeeds (with a {@code 0} exit code), the
 * {@link SshCommandResult} is returned by the task. If none of the attempts
 * succeed, an {@link Exception} is raised.
 * 
 * 
 * 
 */
public class StandardSshLivenessTest implements SshLivenessTest {
	static Logger LOG = LoggerFactory.getLogger(StandardSshLivenessTest.class);

	/** {@link Machine} whose liveness will be checked. */
	private final Machine machine;
	/**
	 * The SSH port to connect to. Typically 22.
	 */
	private final int sshPort;

	/** SSH login user. */
	private final String sshUser;
	/** Private SSH login key. */
	private final String sshKeyPath;

	/**
	 * Command used to evaluate the liveness status of the server. If the
	 * command returns a zero value, the server is deemed alive.
	 */
	private final String command;

	/** Maximum number of retries to attempt before giving up. */
	private final int maxRetries;
	/** Retry delay (in seconds). */
	private final long retryDelay;
	/**
	 * Human-readable name of the {@link RetryableRequest} that will drive the
	 * liveness test.
	 */
	private final String taskName;

	/**
	 * Creates a new {@link StandardSshLivenessTest}.
	 * 
	 * @param machine
	 *            {@link Machine} whose liveness will be checked.
	 * @param sshPort
	 *            The SSH port to connect to. Typically 22.
	 * @param sshUser
	 *            SSH login user.
	 * @param sshKeyPath
	 *            Private SSH login key.
	 * @param command
	 *            Command used to evaluate the liveness status of the server. If
	 *            the command returns a zero value, the server is deemed alive.
	 * @param maxRetries
	 *            Maximum number of retries to attempt before giving up.
	 * @param retryDelay
	 *            Retry delay (in seconds).
	 * @param taskName
	 *            Human-readable name of the {@link RetryableRequest} that will
	 *            drive the liveness test.
	 */
	public StandardSshLivenessTest(Machine machine, int sshPort,
			String sshUser, String sshKeyPath, String command, int maxRetries,
			long retryDelay, String taskName) {

		checkArgument(machine != null, "null machine");
		checkArgument(sshPort > 0, "sshPort must be > 0");
		checkArgument(sshUser != null, "null sshUser");
		checkArgument(sshKeyPath != null, "null sshKeyPath");
		checkArgument(new File(sshKeyPath).isFile(),
				"private key file '%s' does not exist", sshKeyPath);
		checkArgument(command != null, "null command");
		checkArgument(maxRetries >= 0, "maxRetries must be >= 0");
		checkArgument(retryDelay >= 0, "retryDelay must be >= 0");
		checkArgument(taskName != null, "null taskName");

		this.machine = machine;
		this.sshPort = sshPort;
		this.sshUser = sshUser;
		this.sshKeyPath = sshKeyPath;
		this.command = command;

		this.maxRetries = maxRetries;
		this.retryDelay = retryDelay;
		this.taskName = taskName;
	}

	@Override
	public SshCommandResult call() throws Exception {

		String publicIp = Iterables.getFirst(this.machine.getPublicIps(), null);
		Preconditions.checkArgument(publicIp != null,
				"cannot check liveness: no public IP address on machine %s",
				this.machine.getId());

		long delayInMillis = this.retryDelay * 1000;

		RetryableRequest<SshCommandResult> sshCommand = new RetryableRequest<SshCommandResult>(
				new SshCommandRequester(publicIp, this.sshPort, this.sshUser,
						this.sshKeyPath, this.command), new RetrySshCommand(
						this.maxRetries, delayInMillis), this.taskName);
		SshCommandResult response = sshCommand.call();
		return response;
	}

	@Override
	public Machine getMachine() {
		return this.machine;
	}
}
