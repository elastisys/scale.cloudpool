package com.elastisys.scale.cloudadapters.commons.adapter.liveness;

import java.util.concurrent.Future;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapers.api.types.LivenessState;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.LivenessConfig;

/**
 * Runs liveness tests and tracks the observed liveness of {@link Machine}s,
 * which makes it capable of answering queries about the liveness state of
 * {@link Machine}s in a scaling group.
 * <p/>
 * Note that the {@code configure} method must be invoked prior to executing any
 * other methods. Implementations should throw {@link IllegalStateException}s
 * whenever a method is accessed before a configuration has been set.
 * <p/>
 * Implementors should take care to ensure that implementations are thread-safe,
 * since they may be called by several concurrent threads.
 * 
 * 
 * 
 */
public interface LivenessTracker {

	/**
	 * Sets a configuration for this {@link LivenessTracker}.
	 * 
	 * @param config
	 *            Tells the {@link LivenessTracker} how to carry out liveness
	 *            tests.
	 * @throws CloudAdapterException
	 *             On failure to apply the configuration.
	 */
	void configure(LivenessConfig config) throws CloudAdapterException;

	/**
	 * Runs a <i>boot-time</i> liveness test (according to the set
	 * {@link LivenessConfig}) on a given {@link Machine} in an asynchronous
	 * manner.
	 * <p/>
	 * When the <i>method</i> returns, the {@link Machine}'s latest observed
	 * liveness state is saved as {@link LivenessState#BOOTING}.
	 * <p/>
	 * When the <i>liveness test</i> task eventually finishes the latest
	 * observed liveness state has been set to either {@link LivenessState#LIVE}
	 * or {@link LivenessState#UNHEALTHY}.
	 * 
	 * @param machine
	 *            The {@link Machine} to test.
	 * @return The {@link Future} result of the liveness test task.
	 */
	public Future<LivenessTestResult> checkBootLiveness(Machine machine);

	/**
	 * Runs a <i>run-time</i> liveness test (according to the set
	 * {@link LivenessConfig}) on a given {@link Machine} in an asynchronous
	 * manner.
	 * <p/>
	 * When the <i>method</i> returns the liveness test task has been started.
	 * <p/>
	 * When the liveness test task eventually finishes the latest observed
	 * liveness state has been set to either {@link LivenessState#LIVE} or
	 * {@link LivenessState#UNHEALTHY}.
	 * 
	 * @param machine
	 *            The {@link Machine} to test.
	 * @return The {@link Future} result of the liveness test task.
	 */
	public Future<LivenessTestResult> checkRuntimeLiveness(Machine machine);

	/**
	 * Returns the last observed {@link LivenessState} of a certain
	 * {@link Machine}, or {@link LivenessState#UNKNOWN} if it no liveness
	 * status has (yet) been determined for the {@link Machine}.
	 * 
	 * @param machine
	 *            The {@link Machine} for which liveness state is requested.
	 * @return
	 */
	public LivenessState getLiveness(Machine machine);
}
