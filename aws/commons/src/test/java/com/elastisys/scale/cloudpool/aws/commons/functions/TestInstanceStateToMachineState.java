package com.elastisys.scale.cloudpool.aws.commons.functions;

import static com.amazonaws.services.ec2.model.InstanceStateName.Pending;
import static com.amazonaws.services.ec2.model.InstanceStateName.ShuttingDown;
import static com.amazonaws.services.ec2.model.InstanceStateName.Stopped;
import static com.amazonaws.services.ec2.model.InstanceStateName.Stopping;
import static com.amazonaws.services.ec2.model.InstanceStateName.Terminated;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.aws.commons.functions.InstanceStateToMachineState;

/**
 * Exercises the {@link InstanceStateToMachineState} class.
 *
 *
 *
 */
public class TestInstanceStateToMachineState {

    @Test
    public void convert() {
        assertThat(convert(state(Pending)), is(MachineState.PENDING));
        assertThat(convert(state(InstanceStateName.Running)), is(MachineState.RUNNING));
        assertThat(convert(state(ShuttingDown)), is(MachineState.TERMINATING));
        assertThat(convert(state(Stopped)), is(MachineState.TERMINATED));
        assertThat(convert(state(Stopping)), is(MachineState.TERMINATING));
        assertThat(convert(state(Terminated)), is(MachineState.TERMINATED));
    }

    private InstanceState state(InstanceStateName stateName) {
        return new InstanceState().withName(stateName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertUnrecognizedState() {
        convert(new InstanceState().withName("UNRECOGNIZED"));
    }

    private MachineState convert(InstanceState instanceState) {
        return new InstanceStateToMachineState().apply(instanceState);
    }
}
