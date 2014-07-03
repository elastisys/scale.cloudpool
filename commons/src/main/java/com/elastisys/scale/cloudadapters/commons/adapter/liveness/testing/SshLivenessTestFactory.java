package com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.LivenessConfig;

/**
 * A factory that produces SSH-based liveness tests to check the liveness status
 * of {@link Machine}s.
 * 
 * @see SshLivenessTest
 * 
 * 
 * 
 */
public interface SshLivenessTestFactory {
	/**
	 * Factory method for creating a <i>boot-time</i> SSH-based liveness test
	 * for a given {@link Machine}.
	 * 
	 * @param machine
	 *            The server instance that is being booted.
	 * 
	 * @param livenessConfig
	 *            The boot-time liveness check parameters.
	 * @return A ready-to-use boot-time {@link SshLivenessTest} for the machine.
	 */
	public SshLivenessTest createBootTimeCheck(Machine machine,
			LivenessConfig livenessConfig);

	/**
	 * Factory method for creating a <i>run-time</i> SSH-based liveness check
	 * for a given {@link Machine}.
	 * 
	 * @param machine
	 *            The server instance for which to create the liveness test.
	 * @param livenessConfig
	 *            The run-time liveness check parameters.
	 * @return A ready-to-use run-time {@link SshLivenessTest} for the machine.
	 */
	public SshLivenessTest createRunTimeCheck(Machine machine,
			LivenessConfig livenessConfig);
}
