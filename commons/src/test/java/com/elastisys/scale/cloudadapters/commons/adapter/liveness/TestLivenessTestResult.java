package com.elastisys.scale.cloudadapters.commons.adapter.liveness;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.types.LivenessState;
import com.elastisys.scale.cloudadapers.api.types.Machine;
import com.elastisys.scale.cloudadapers.api.types.MachineState;
import com.elastisys.scale.commons.net.ssh.SshCommandResult;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

/**
 * Exercises the {@link LivenessTestResult} class.
 * 
 * 
 * 
 */
public class TestLivenessTestResult {

	@Test
	public void createWhenLivenessTestWasAbleToRun() {
		Machine machine = machine("i-1");

		// first scenario: liveness test was successful (zero exit code)
		SshCommandResult liveCommandResult = new SshCommandResult(0, "OK", "");
		LivenessTestResult liveTestResult = new LivenessTestResult(machine,
				LivenessState.LIVE, liveCommandResult);
		assertThat(liveTestResult.getMachine(), is(machine));
		assertThat(liveTestResult.getState(), is(LivenessState.LIVE));
		assertThat(liveTestResult.getError().isPresent(), is(false));
		assertThat(liveTestResult.getCommandResult().isPresent(), is(true));
		assertThat(liveTestResult.getCommandResult().get(),
				is(liveCommandResult));

		// second scenario: liveness test was unsuccessful (non-zero exit code)
		SshCommandResult unhealthyCommandResult = new SshCommandResult(1,
				"NOT OK", "ERROR");
		LivenessTestResult unhealthyTestResult = new LivenessTestResult(
				machine, LivenessState.UNHEALTHY, unhealthyCommandResult);
		assertThat(unhealthyTestResult.getMachine(), is(machine));
		assertThat(unhealthyTestResult.getState(), is(LivenessState.UNHEALTHY));
		assertThat(unhealthyTestResult.getError().isPresent(), is(false));
		assertThat(unhealthyTestResult.getCommandResult().isPresent(), is(true));
		assertThat(unhealthyTestResult.getCommandResult().get(),
				is(unhealthyCommandResult));

	}

	@Test
	public void createWhenLivenessFailedToRun() {
		Machine machine = machine("i-1");

		// scenario: liveness test failed to execute at all (for example: ssh
		// connection refused)
		Throwable error = new RuntimeException("connection refused");
		LivenessTestResult liveTestResult = new LivenessTestResult(machine,
				LivenessState.UNHEALTHY, error);
		assertThat(liveTestResult.getMachine(), is(machine));
		assertThat(liveTestResult.getState(), is(LivenessState.UNHEALTHY));
		assertThat(liveTestResult.getError().isPresent(), is(true));
		assertThat(liveTestResult.getError().get(), is(error));
		assertThat(liveTestResult.getCommandResult().isPresent(), is(false));
	}

	private Machine machine(String withId) {
		List<String> publicIps = Lists.newArrayList();
		List<String> privateIps = Lists.newArrayList();
		return new Machine(withId, MachineState.RUNNING, UtcTime.now(),
				publicIps, privateIps, new JsonObject());
	}
}
