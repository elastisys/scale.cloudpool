package com.elastisys.scale.cloudpool.kubernetes.functions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.cloudpool.api.types.MachineState;

/**
 * Exercises {@link PodStateToMachineState}.
 */
public class TestPodStateToMachineState {

    /**
     * Verifies that available <a href=
     * "https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/">pod
     * phases</a> are correctly translated to {@link MachineState}s.
     */
    @Test
    public void convertPodPhase() {
        assertThat(new PodStateToMachineState().apply("Pending"), is(MachineState.PENDING));
        assertThat(new PodStateToMachineState().apply("Running"), is(MachineState.RUNNING));
        assertThat(new PodStateToMachineState().apply("Succeeded"), is(MachineState.TERMINATED));
        assertThat(new PodStateToMachineState().apply("Failed"), is(MachineState.TERMINATED));
    }

    /**
     * should fail on unrecognized pod phases.
     */
    @Test(expected = IllegalArgumentException.class)
    public void onUnrecognizedPodPhase() {
        new PodStateToMachineState().apply("Weird");
    }
}
