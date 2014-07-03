package com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.impl;

import static com.elastisys.scale.cloudadapters.commons.adapter.BaseAdapterTestUtils.machine;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.SshLivenessTest;
import com.elastisys.scale.cloudadapters.commons.testutils.sshserver.PermissiveSshServer;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.net.ssh.SshCommandResult;

/**
 * Exercises the {@link StandardSshLivenessTest} class by running it against an
 * SSH server that is embedded in the test.
 * 
 * 
 * 
 */
public class TestStandardSshLivenessTest {

	static Logger LOG = LoggerFactory
			.getLogger(TestStandardSshLivenessTest.class);

	/** Embedded SSH server used in the tests. */
	private static PermissiveSshServer sshServer;

	@BeforeClass
	public static void onSetup() throws IOException {
		int port = HostUtils.findFreePorts(1).get(0);
		sshServer = new PermissiveSshServer(port,
				"src/test/resources/security/serverkey.pem");

		sshServer.start();
		LOG.info("SSH server started on port " + port);
	}

	@Before
	public void beforeTestMethod() {
		// reset command counter
		sshServer.setCommandCounter(0);
	}

	@AfterClass
	public static void onTeardown() throws Exception {
		sshServer.stop();
	}

	/**
	 * Tests a SSH liveness test that returns with success (zero exit code).
	 */
	@Test
	public void testSuccessfulLivenessTest() throws Exception {
		int maxRetries = 5;
		SshLivenessTest livenessTest = new StandardSshLivenessTest(machine(
				"i-1", "localhost"), sshServer.getPort(), "ubuntu",
				"src/test/resources/security/clientkey.pem", "echo success",
				maxRetries, 0, "liveness-test");
		SshCommandResult result = livenessTest.call();
		assertThat(result.getExitStatus(), is(0));
		assertThat(result.getStderr().length(), is(0));
		assertTrue(result.getStdout().length() > 0);

		// command should have succeeded on first attempt
		assertThat(sshServer.getCommandCounter(), is(1));
	}

	/**
	 * Tests a SSH liveness test that fails with non-zero exit code.
	 */
	@Test
	public void testUnsuccessfulLivenessTest() throws Exception {
		int maxRetries = 5;
		SshLivenessTest livenessTest = new StandardSshLivenessTest(machine(
				"i-1", "localhost"), sshServer.getPort(), "ubuntu",
				"src/test/resources/security/clientkey.pem",
				"service unknownservice status", maxRetries, 0, "liveness-test");
		SshCommandResult result = livenessTest.call();
		assertTrue(result.getExitStatus() != 0);
		assertTrue(result.getStderr().length() > 0);
		assertTrue(result.getStdout().length() == 0);

		// command should not have succeeded => server should have received the
		// full number of retries
		assertThat(sshServer.getCommandCounter(), is(maxRetries + 1));
	}

	/**
	 * Tests a SSH liveness test that fails due to a connection refused error.
	 */
	@Test
	public void testErroneousLivenessTest() throws Exception {
		int maxRetries = 5;
		// attempt to connect to the wrong port
		int wrongPort = HostUtils.findFreePorts(1).get(0);
		SshLivenessTest livenessTest = new StandardSshLivenessTest(machine(
				"i-1", "localhost"), wrongPort, "ubuntu",
				"src/test/resources/security/clientkey.pem",
				"service unknownservice status", maxRetries, 0, "liveness-test");

		try {
			livenessTest.call();
			fail("server should be unreachable");
		} catch (ExecutionException e) {
			// expected
			// assertThat(e.getCause(), is(ConnectException.class));
			assertThat(e.getCause().getMessage(),
					is("Maximum number of retries (5) exceeded. "
							+ "Last error: java.net.ConnectException: "
							+ "Connection refused"));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullMachine() {
		new StandardSshLivenessTest(null, sshServer.getPort(), "ubuntu",
				"src/test/resources/security/clientkey.pem", "echo success", 5,
				10, "liveness-test");
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithIllegalSshPort() {
		new StandardSshLivenessTest(machine("i-1", "localhost"), -1, "ubuntu",
				"src/test/resources/security/clientkey.pem", "echo success", 5,
				10, "liveness-test");
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullSshUser() {
		new StandardSshLivenessTest(machine("i-1", "localhost"),
				sshServer.getPort(), null,
				"src/test/resources/security/clientkey.pem", "echo success", 5,
				10, "liveness-test");
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullPrivateKey() {
		new StandardSshLivenessTest(machine("i-1", "localhost"),
				sshServer.getPort(), "ubuntu", null, "echo success", 5, 10,
				"liveness-test");
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNonExistingPrivateKey() {
		new StandardSshLivenessTest(machine("i-1", "localhost"),
				sshServer.getPort(), "ubuntu", "/missing/private/key.pem",
				"echo success", 5, 10, "liveness-test");
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNullCommand() {
		new StandardSshLivenessTest(machine("i-1", "localhost"),
				sshServer.getPort(), "ubuntu",
				"src/test/resources/security/clientkey.pem", null, 5, 10,
				"liveness-test");
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNegativeMaxRetries() {
		new StandardSshLivenessTest(machine("i-1", "localhost"),
				sshServer.getPort(), "ubuntu",
				"src/test/resources/security/clientkey.pem", "echo success",
				-1, 10, "liveness-test");
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithNegativeRetryDelay() {
		new StandardSshLivenessTest(machine("i-1", "localhost"),
				sshServer.getPort(), "ubuntu",
				"src/test/resources/security/clientkey.pem", "echo success", 5,
				-1, "liveness-test");
	}
}