package com.elastisys.scale.cloudadapters.commons.adapter.liveness.testing;

import java.util.concurrent.Callable;

import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.commons.net.ssh.SshCommandResult;

/**
 * A liveness test that, when {@link #call()}ed, checks the liveness of a
 * {@link Machine} by executing a command over SSH on the remote {@link Machine}
 * .
 * <p/>
 * The result of calling a {@link SshLivenessTest} is either a
 * {@link SshCommandResult} if the SSH command was able to run, or an
 * {@link Exception} in case the SSH command for some reason could not be
 * executed (for example, due to a connection refused error).
 * 
 * @see SshLivenessTestFactory
 * 
 * 
 */
public interface SshLivenessTest extends Callable<SshCommandResult> {

	/**
	 * Returns the {@link Machine} that this liveness test applies to.
	 * 
	 * @return The targeted {@link Machine}.
	 */
	public Machine getMachine();
}
