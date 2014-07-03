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
 * always returns a given {@link SshCommandResult}.
 * 
 * 
 * 
 */
public class FixedResultSshLivenessTestFactory implements
		SshLivenessTestFactory {
	static Logger LOG = LoggerFactory
			.getLogger(FixedResultSshLivenessTestFactory.class);

	/**
	 * The {@link SshCommandResult} that all produced {@link SshLivenessTest}s
	 * will return.
	 */
	private final SshCommandResult fixedResult;

	public FixedResultSshLivenessTestFactory(SshCommandResult fixedResult) {
		this.fixedResult = fixedResult;
	}

	@Override
	public SshLivenessTest createBootTimeCheck(Machine machine,
			LivenessConfig livenessConfig) {
		return new FixedResultSshLivenessTest(machine, this.fixedResult);
	}

	@Override
	public SshLivenessTest createRunTimeCheck(Machine machine,
			LivenessConfig livenessConfig) {
		return new FixedResultSshLivenessTest(machine, this.fixedResult);
	}

	public static class FixedResultSshLivenessTest implements SshLivenessTest {

		private final Machine targetMachine;
		/**
		 * The {@link SshCommandResult} that the {@link SshLivenessTest} will
		 * return.
		 */
		private final SshCommandResult fixedResult;

		public FixedResultSshLivenessTest(Machine targetMachine,
				SshCommandResult fixedResult) {
			this.targetMachine = targetMachine;
			this.fixedResult = fixedResult;
		}

		@Override
		public SshCommandResult call() throws Exception {
			LOG.debug("running " + getClass().getSimpleName() + " ...");
			return this.fixedResult;
		}

		@Override
		public Machine getMachine() {
			return this.targetMachine;
		}
	}

}
