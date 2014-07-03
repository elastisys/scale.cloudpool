package com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.impl;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.LivenessConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.SshLivenessTest;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.SshLivenessTestFactory;

/**
 * An {@link SshLivenessTestFactory} that produces
 * {@link StandardSshLivenessTest}s.
 * 
 * @see StandardSshLivenessTest
 * 
 * 
 * 
 */
public class StandardSshLivenessTestFactory implements SshLivenessTestFactory {

	@Override
	public SshLivenessTest createBootTimeCheck(Machine machine,
			LivenessConfig livenessConfig) {
		int sshPort = livenessConfig.getSshPort();
		String username = livenessConfig.getLoginUser();
		String privateKey = livenessConfig.getLoginKey();
		String command = livenessConfig.getBootTimeCheck().getCommand();
		int maxRetries = livenessConfig.getBootTimeCheck().getMaxRetries();
		long delay = livenessConfig.getBootTimeCheck().getRetryDelay();

		String taskName = String.format("boot-liveness-waiter{%s}",
				machine.getId());
		SshLivenessTest livenessTest = new StandardSshLivenessTest(machine,
				sshPort, username, privateKey, command, maxRetries, delay,
				taskName);
		return livenessTest;
	}

	@Override
	public SshLivenessTest createRunTimeCheck(Machine machine,
			LivenessConfig livenessConfig) {
		int sshPort = livenessConfig.getSshPort();
		String username = livenessConfig.getLoginUser();
		String privateKey = livenessConfig.getLoginKey();
		String command = livenessConfig.getRunTimeCheck().getCommand();
		int maxRetries = livenessConfig.getRunTimeCheck().getMaxRetries();
		long delay = livenessConfig.getRunTimeCheck().getRetryDelay();

		String taskName = String.format("runtime-liveness-check{%s}",
				machine.getId());
		SshLivenessTest livenessTest = new StandardSshLivenessTest(machine,
				sshPort, username, privateKey, command, maxRetries, delay,
				taskName);
		return livenessTest;
	}
}
