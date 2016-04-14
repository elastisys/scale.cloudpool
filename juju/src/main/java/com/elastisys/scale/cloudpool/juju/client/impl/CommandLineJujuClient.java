package com.elastisys.scale.cloudpool.juju.client.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.cloudpool.juju.client.JujuClient;
import com.elastisys.scale.cloudpool.juju.config.JujuCloudPoolConfig;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Manages the Juju deployment by calling out to the juju command line tool. The
 * tool set <strong>must</strong> be installed for this to work. Ensure that you
 * follow
 * <a href="https://jujucharms.com/docs/stable/getting-started" target="_blank">
 * the official instructions</a> to install them correctly.
 *
 *
 *
 * @author Elastisys AB <techteam@elastisys.com>
 *
 */
public class CommandLineJujuClient implements JujuClient {
	private static final Logger LOG = LoggerFactory.getLogger(CommandLineJujuClient.class);
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private final Gson gson;

	private JujuCloudPoolConfig config;

	public CommandLineJujuClient() {
		this.config = null;
		this.gson = new Gson();
	}

	@Override
	public void configure(JujuCloudPoolConfig config) throws IOException {
		final File jujuDirectory = new File(getJujuHome());

		LOG.info("Configuring cloud pool in {} directory...", jujuDirectory);

		Set<PosixFilePermission> restrictivePermissions = EnumSet.of(PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE);

		FileUtils.forceMkdir(jujuDirectory);
		FileUtils.cleanDirectory(jujuDirectory);

		File environmentsDotYaml = new File(jujuDirectory, "environments.yaml");
		Files.write(config.getEnvironment().getConfig(), environmentsDotYaml, UTF8);
		java.nio.file.Files.setPosixFilePermissions(environmentsDotYaml.toPath(), restrictivePermissions);

		File currentEnvironment = new File(jujuDirectory, "current-environment");
		Files.write(config.getEnvironment().getName(), currentEnvironment, UTF8);
		java.nio.file.Files.setPosixFilePermissions(currentEnvironment.toPath(), restrictivePermissions);

		File environmentsDirectory = new File(jujuDirectory, "environments");
		FileUtils.forceMkdir(environmentsDirectory);
		File environmentDotJenv = new File(environmentsDirectory, config.getEnvironment().getName() + ".jenv");
		Files.write(config.getEnvironment().getJenv(), environmentDotJenv, UTF8);
		java.nio.file.Files.setPosixFilePermissions(environmentDotJenv.toPath(), restrictivePermissions);

		File sshDirectory = new File(jujuDirectory, "ssh");
		FileUtils.forceMkdir(sshDirectory);
		File privateKeyFile = new File(sshDirectory, "juju_id_rsa");
		Files.write(config.getEnvironment().getPrivkey(), privateKeyFile, UTF8);
		java.nio.file.Files.setPosixFilePermissions(privateKeyFile.toPath(), restrictivePermissions);

		File publicKeyFile = new File(sshDirectory, "juju_id_rsa.pub");
		Files.write(config.getEnvironment().getPubkey(), publicKeyFile, UTF8);
		java.nio.file.Files.setPosixFilePermissions(publicKeyFile.toPath(), restrictivePermissions);

		this.config = config;
	}

	private String getJujuHome() {
		return getEnvironmentVariableOrDefault("JUJU_HOME", System.getProperty("user.home") + "/.juju");
	}

	private String getEnvironmentVariableOrDefault(String variable, String defaultValue) {
		String value;
		value = System.getProperty(variable);
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

	@Override
	public void setDesiredSize(Integer desiredSize) {
		// TODO Auto-generated method stub

	}

	@Override
	public PoolSizeSummary getPoolSize() throws IOException {
		ensureConfigured();

		String output = command("juju", "status", "--format=json");
		JsonObject jsonOutput = this.gson.fromJson(output, JsonObject.class);

		switch (this.config.getMode()) {
		case MACHINES:
			return getMachinePoolSummary(jsonOutput);
		case UNITS:
			return getUnitPoolSummary(jsonOutput);
		default:
			throw new RuntimeException(
					String.format("Programmer error, configuration mode %s is unknown", this.config.getMode()));
		}
	}

	private PoolSizeSummary getUnitPoolSummary(JsonObject jsonOutput) {
		int active;
		int allocated;
		int desiredSize = Integer.MAX_VALUE;

		JsonElement services = jsonOutput.get("services");

		PoolSizeSummary poolSizeSummary = new PoolSizeSummary(desiredSize, allocated, active);

		return poolSizeSummary;
	}

	private PoolSizeSummary getMachinePoolSummary(JsonObject jsonOutput) {
		int active = 0;
		int allocated;
		int desiredSize = Integer.MAX_VALUE;

		JsonObject machines = jsonOutput.get("machines").getAsJsonObject();
		Set<Entry<String, JsonElement>> entries = machines.entrySet();

		for (Entry<String, JsonElement> machineEntry : entries) {
			final String agentState = machineEntry.getValue().getAsJsonObject().get("agent-state").getAsString();
			if (agentState.equals("started")) {
				active++;
			}
		}

		allocated = entries.size();

		PoolSizeSummary poolSizeSummary = new PoolSizeSummary(desiredSize, allocated, active);

		return poolSizeSummary;
	}

	@Override
	public MachinePool getMachinePool() {

	}

	private void ensureConfigured() {
		if (this.config == null) {
			throw new IllegalStateException("cloudpool has not been configured");
		}
	}

	/**
	 * Runs a command at returns its standard output.
	 *
	 * @param tokens
	 *            The tokens that make up the command line.
	 * @return The standard output from the command.
	 * @throws IOException
	 *             Thrown if the command could not be started.
	 * @throws InterruptedException
	 *             Thrown if interrupted waiting for the process to finish.
	 */
	private String command(String... tokens) throws IOException {
		LOG.debug("Running command specified by {}", Arrays.asList(tokens));

		final Process process = new ProcessBuilder(tokens).start();

		final Scanner stdoutScanner = new Scanner(process.getInputStream());
		final StringBuilder sb = new StringBuilder();
		while (stdoutScanner.hasNext()) {
			sb.append(stdoutScanner.next());
		}
		stdoutScanner.close();

		try {
			process.waitFor();
		} catch (InterruptedException e) {
			throw new IOException("Interrupted waiting for child process to terminate", e);
		}

		if (process.exitValue() != 0) {
			throw new IOException(
					String.format("The command %s exited with non-zero exit status", Arrays.asList(tokens)));
		}

		return sb.toString();
	}
}
