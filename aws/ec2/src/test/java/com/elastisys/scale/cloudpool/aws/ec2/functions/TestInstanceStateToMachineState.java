package com.elastisys.scale.cloudpool.aws.ec2.functions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.amazonaws.services.ec2.model.InstanceState;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.aws.ec2.functions.InstanceStateToMachineState;

/**
 * Exercises the {@link InstanceStateToMachineState} class.
 */
public class TestInstanceStateToMachineState {

	@Test
	public void convert() {
		assertThat(convert(instanceState("pending")), is(MachineState.PENDING));
		assertThat(convert(instanceState("running")), is(MachineState.RUNNING));
		assertThat(convert(instanceState("shutting-down")),
				is(MachineState.TERMINATING));
		assertThat(convert(instanceState("stopped")),
				is(MachineState.TERMINATED));
		assertThat(convert(instanceState("stopping")),
				is(MachineState.TERMINATING));
		assertThat(convert(instanceState("terminated")),
				is(MachineState.TERMINATED));
	}

	@Test(expected = IllegalArgumentException.class)
	public void convertUnrecognizedState() {
		convert(instanceState("UNRECOGNIZED"));
	}

	private MachineState convert(InstanceState instanceState) {
		return new InstanceStateToMachineState().apply(instanceState);
	}

	private InstanceState instanceState(String state) {
		return new InstanceState().withName(state);
	}
}
