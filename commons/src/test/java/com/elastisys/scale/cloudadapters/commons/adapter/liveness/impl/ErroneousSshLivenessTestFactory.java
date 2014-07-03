package com.elastisys.scale.cloudadapters.commons.adapter.liveness.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.LivenessConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.SshLivenessTest;
import com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing.SshLivenessTestFactory;
import com.elastisys.scale.commons.net.ssh.SshCommandResult;

/**
 * {@link SshLivenessTestFactory} that produces {@link SshLivenessTest} that
 * will fail by raising an {@link Exception} (specified by the creator of the
 * factory).
 * 
 * 
 * 
 */
public class ErroneousSshLivenessTestFactory implements SshLivenessTestFactory {
	static Logger LOG = LoggerFactory
			.getLogger(ErroneousSshLivenessTestFactory.class);

	/**
	 * The error that {@link SshLivenessTest}s produced by the factory will
	 * throw.
	 */
	private final Exception livenessTestError;

	/**
	 * Creates a new {@link ErroneousSshLivenessTestFactory}.
	 * 
	 * @param livenessTestError
	 *            The error that {@link SshLivenessTest}s produced by the
	 *            factory will throw.
	 */
	public ErroneousSshLivenessTestFactory(Exception livenessTestError) {
		this.livenessTestError = livenessTestError;
	}

	@Override
	public SshLivenessTest createBootTimeCheck(Machine machine,
			LivenessConfig livenessConfig) {
		return new ErroneousSshLivenessTest(machine, this.livenessTestError);
	}

	@Override
	public SshLivenessTest createRunTimeCheck(Machine machine,
			LivenessConfig livenessConfig) {
		return new ErroneousSshLivenessTest(machine, this.livenessTestError);
	}

	public static class ErroneousSshLivenessTest implements SshLivenessTest {

		private final Machine targetMachine;
		/**
		 * The error that {@link SshLivenessTest} will throw when executed.
		 */
		private final Exception livenessTestError;

		public ErroneousSshLivenessTest(Machine targetMachine,
				Exception livenessTestError) {
			this.targetMachine = targetMachine;
			this.livenessTestError = livenessTestError;
		}

		@Override
		public SshCommandResult call() throws Exception {
			LOG.debug("running " + getClass().getSimpleName() + " ...");
			throw this.livenessTestError;
		}

		@Override
		public Machine getMachine() {
			return this.targetMachine;
		}
	}
}
