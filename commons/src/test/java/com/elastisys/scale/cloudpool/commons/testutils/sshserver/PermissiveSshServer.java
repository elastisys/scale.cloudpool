package com.elastisys.scale.cloudpool.commons.testutils.sshserver;

import java.io.IOException;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SSH server intended to be used in tests, which accepts both password and
 * public key authentication from anyone (all credentials are accepted).
 * 
 * 
 * 
 */
public class PermissiveSshServer {
	static final Logger LOG = LoggerFactory
			.getLogger(PermissiveSshServer.class);

	/** The embedded SSH server. */
	private final SshServer sshd;

	/** Running counter of the number of commands received by the server. */
	private int commandCounter = 0;

	public PermissiveSshServer(int port, String serverKeyPath) {
		this.sshd = SshServer.setUpDefaultServer();
		this.sshd.setPort(port);
		this.sshd
				.setPasswordAuthenticator(new PermissivePasswordAuthenticator());
		this.sshd
				.setPublickeyAuthenticator(new PermissivePublicKeyAuthenticator());
		this.sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(
				serverKeyPath));
		this.sshd.setCommandFactory(new CommandFactory() {
			@Override
			public Command createCommand(String command) {
				PermissiveSshServer.this.commandCounter++;
				LOG.info("SSH server received command '{}'", command);
				return new ExternalProcessCommand(command);
			}
		});
		this.sshd.setShellFactory(new ProcessShellFactory(new String[] {
				"/bin/bash", "-i", "-l" }));
	}

	/**
	 * Re-sets the number of commands received thus far by the server.
	 * 
	 * @param counter
	 *            The counter value to set.
	 */
	public void setCommandCounter(int counter) {
		this.commandCounter = 0;
	}

	/**
	 * Returns the total number of commands received thus far by the server.
	 * 
	 * @return
	 */
	public int getCommandCounter() {
		return this.commandCounter;
	}

	public int getPort() {
		return this.sshd.getPort();
	}

	public void start() throws IOException {
		LOG.info("starting ssh server on port {}", this.sshd.getPort());
		this.sshd.start();
		LOG.info("started ssh server on port {}", this.sshd.getPort());
	}

	public void stop() throws InterruptedException {
		LOG.info("stopping ssh server");
		this.sshd.stop(true);
	}

	public static void main(String[] args) throws Exception {
		PermissiveSshServer sshServer = new PermissiveSshServer(22222,
				"src/test/resources/security/serverkey.pem");
		sshServer.start();
		System.err.println("RETURN to stop");
		System.in.read();
		System.err.println("Stopping ...");
		sshServer.stop();
	}
}
