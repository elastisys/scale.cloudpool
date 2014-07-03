package com.elastisys.scale.cloudadapters.commons.adapter.liveness;

import com.elastisys.scale.cloudadapers.api.types.LivenessState;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.commons.net.ssh.SshCommandResult;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * Represents the result of a liveness test for a {@link Machine}.
 * 
 * @see LivenessTracker
 * 
 * 
 */
public class LivenessTestResult {

	/** The {@link Machine} for which the liveness test was carried out. */
	private final Machine machine;
	/** The {@link LivenessState} of the {@link Machine}. */
	private final LivenessState state;
	/**
	 * The result of executing the liveness test command, if it could be
	 * executed. If the liveness test command could not be executed, details
	 * about the error are found in {@link #error}.
	 */
	private final Optional<SshCommandResult> commandResult;
	/** Error details about why the liveness test command could not be executed. */
	private final Optional<Throwable> error;

	/**
	 * Creates a {@link LivenessTestResult} for a liveness test where the
	 * liveness test command could be executed against the {@link Machine}.
	 * 
	 * @param machine
	 *            The {@link Machine} for which the liveness test was carried
	 *            out.
	 * @param state
	 *            The {@link LivenessState} of the {@link Machine}.
	 * @param commandResult
	 *            The result of executing the liveness test command.
	 */
	public LivenessTestResult(Machine machine, LivenessState state,
			SshCommandResult commandResult) {
		this(machine, state, commandResult, null);
	}

	/**
	 * Creates a {@link LivenessTestResult} for a liveness test where liveness
	 * test command could not be executed against the {@link Machine}.
	 * 
	 * @param machine
	 *            The {@link Machine} for which the liveness test was carried
	 *            out.
	 * @param state
	 *            The {@link LivenessState} of the {@link Machine}.
	 * @param error
	 *            Error details about why the liveness test command could not be
	 *            executed.
	 */
	public LivenessTestResult(Machine machine, LivenessState state,
			Throwable error) {
		this(machine, state, null, error);
	}

	private LivenessTestResult(Machine machine, LivenessState state,
			SshCommandResult commandResult, Throwable error) {
		this.machine = machine;
		this.state = state;
		this.commandResult = Optional.fromNullable(commandResult);
		this.error = Optional.fromNullable(error);
	}

	public Machine getMachine() {
		return this.machine;
	}

	public LivenessState getState() {
		return this.state;
	}

	public Optional<SshCommandResult> getCommandResult() {
		return this.commandResult;
	}

	public Optional<Throwable> getError() {
		return this.error;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.machine, this.state, this.commandResult,
				this.error);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LivenessTestResult) {
			LivenessTestResult that = (LivenessTestResult) obj;
			return Objects.equal(this.machine, that.machine)
					&& Objects.equal(this.state, that.state)
					&& Objects.equal(this.commandResult, that.commandResult)
					&& Objects.equal(this.error, that.error);
		}
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("machine", this.machine.getId()).add("state", this.state)
				.add("commandResult", this.commandResult)
				.add("error", this.error).toString();
	}
}
