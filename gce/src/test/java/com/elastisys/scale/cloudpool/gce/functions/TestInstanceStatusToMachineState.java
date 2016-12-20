package com.elastisys.scale.cloudpool.gce.functions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.google.api.services.compute.model.Instance;

/**
 * Verify that {@link Instance} status is properly translated into a
 * {@link MachineState}. See
 * https://cloud.google.com/compute/docs/instances/checking-instance-status#instance_statuses
 */
public class TestInstanceStatusToMachineState {

    /** Object under test. */
    private InstanceStatusToMachineStatus statusConverter;

    @Before
    public void beforeTestMethod() {
        this.statusConverter = new InstanceStatusToMachineStatus();
    }

    @Test
    public void onValidStates() {
        assertThat(this.statusConverter.apply("PROVISIONING"), is(MachineState.PENDING));
        assertThat(this.statusConverter.apply("STAGING"), is(MachineState.PENDING));
        assertThat(this.statusConverter.apply("RUNNING"), is(MachineState.RUNNING));
        assertThat(this.statusConverter.apply("STOPPING"), is(MachineState.TERMINATING));
        assertThat(this.statusConverter.apply("SUSPENDING"), is(MachineState.TERMINATING));
        assertThat(this.statusConverter.apply("SUSPENDED"), is(MachineState.TERMINATED));
        assertThat(this.statusConverter.apply("TERMINATED"), is(MachineState.TERMINATED));
    }

    @Test(expected = IllegalArgumentException.class)
    public void onInvalidState() {
        this.statusConverter.apply("BOOTING");
    }
}
