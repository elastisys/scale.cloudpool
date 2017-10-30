package com.elastisys.scale.cloudpool.azure.driver.functions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.microsoft.azure.management.batch.ProvisioningState;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;

/**
 * Verifies the behavior of {@link VmToMachineState}.
 */
public class TestVmToMachineState {

    /**
     * This test checks exercises {@link VmToMachineState} when only a
     * provisioning state is set. Typically happens during creation of VM
     * (before a hypervisor powerState is set).
     */
    @Test
    public void withOnlyProvisioningState() {
        assertThat(convert(vm(ProvisioningState.CANCELLED, null)), is(MachineState.TERMINATED));
        assertThat(convert(vm(ProvisioningState.CREATING, null)), is(MachineState.PENDING));
        assertThat(convert(vm(ProvisioningState.DELETING, null)), is(MachineState.TERMINATING));
        assertThat(convert(vm(ProvisioningState.FAILED, null)), is(MachineState.PENDING));
        assertThat(convert(vm(ProvisioningState.INVALID, null)), is(MachineState.PENDING));
        assertThat(convert(vm(ProvisioningState.SUCCEEDED, null)), is(MachineState.RUNNING));
    }

    /**
     * Verifies state translation when only a powerState is set on a VM. From
     * what we can gather, this scenario should never happen (a
     * provisioningState is always set). Nevertheless, it *could* happen.
     *
     */
    @Test
    public void withOnlyPowerState() {
        assertThat(convert(vm(null, PowerState.DEALLOCATED)), is(MachineState.TERMINATED));
        assertThat(convert(vm(null, PowerState.DEALLOCATING)), is(MachineState.TERMINATING));
        assertThat(convert(vm(null, PowerState.RUNNING)), is(MachineState.RUNNING));
        assertThat(convert(vm(null, PowerState.STARTING)), is(MachineState.PENDING));
        assertThat(convert(vm(null, PowerState.STOPPED)), is(MachineState.TERMINATED));
        assertThat(convert(vm(null, PowerState.STOPPING)), is(MachineState.TERMINATING));
    }

    /**
     * When a provisioningState and a powerState is set, the provisioningState
     * determines the {@link MachineState}, unless in
     * provisioningState/succeded, in which case the powerState may carry more
     * detailed state.
     */
    @Test
    public void withProvisioningStateAndPowerState() {
        assertThat(convert(vm(ProvisioningState.CANCELLED, PowerState.RUNNING)), is(MachineState.TERMINATED));
        assertThat(convert(vm(ProvisioningState.CREATING, PowerState.RUNNING)), is(MachineState.PENDING));
        assertThat(convert(vm(ProvisioningState.DELETING, PowerState.RUNNING)), is(MachineState.TERMINATING));
        assertThat(convert(vm(ProvisioningState.FAILED, PowerState.RUNNING)), is(MachineState.PENDING));
        assertThat(convert(vm(ProvisioningState.INVALID, PowerState.RUNNING)), is(MachineState.PENDING));

        assertThat(convert(vm(ProvisioningState.SUCCEEDED, PowerState.RUNNING)), is(MachineState.RUNNING));
        assertThat(convert(vm(ProvisioningState.SUCCEEDED, PowerState.DEALLOCATING)), is(MachineState.TERMINATING));
        assertThat(convert(vm(ProvisioningState.SUCCEEDED, PowerState.DEALLOCATED)), is(MachineState.TERMINATED));
        assertThat(convert(vm(ProvisioningState.SUCCEEDED, PowerState.STARTING)), is(MachineState.PENDING));
        assertThat(convert(vm(ProvisioningState.SUCCEEDED, PowerState.STOPPED)), is(MachineState.TERMINATED));
        assertThat(convert(vm(ProvisioningState.SUCCEEDED, PowerState.STOPPING)), is(MachineState.TERMINATING));
    }

    /**
     * When a VM is being deleted, it may or may not have a powerState/running
     * and has provisioningState/deleting.
     */
    @Test
    public void onDeletingVm() {
        assertThat(convert(vm(ProvisioningState.DELETING, PowerState.RUNNING)), is(MachineState.TERMINATING));
        assertThat(convert(vm(ProvisioningState.DELETING, null)), is(MachineState.TERMINATING));
    }

    /**
     * When the {@link PowerState} is unknown, the {@link ProvisioningState} is
     * used to determine {@link MachineState}.
     */
    @Test
    public void withUnknownPowerState() {
        assertThat(convert(vm(ProvisioningState.CANCELLED, PowerState.UNKNOWN)), is(MachineState.TERMINATED));
        assertThat(convert(vm(ProvisioningState.CREATING, PowerState.UNKNOWN)), is(MachineState.PENDING));
        assertThat(convert(vm(ProvisioningState.DELETING, PowerState.UNKNOWN)), is(MachineState.TERMINATING));
        assertThat(convert(vm(ProvisioningState.FAILED, PowerState.UNKNOWN)), is(MachineState.PENDING));
        assertThat(convert(vm(ProvisioningState.INVALID, PowerState.UNKNOWN)), is(MachineState.PENDING));
        assertThat(convert(vm(ProvisioningState.SUCCEEDED, PowerState.UNKNOWN)), is(MachineState.RUNNING));
    }

    /**
     * Verify that an exception is raised on failure to determine a
     * {@link MachineState}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withNullPowerStateAndNullProvisioningState() {
        assertThat(convert(vm(null, null)), is(MachineState.TERMINATED));
    }

    private MachineState convert(VirtualMachine vm) {
        return new VmToMachineState().apply(vm);
    }

    private VirtualMachine vm(ProvisioningState provisioningState, PowerState powerState) {
        VirtualMachine vmMock = mock(VirtualMachine.class);

        when(vmMock.powerState()).thenReturn(powerState);
        if (provisioningState == null) {
            when(vmMock.provisioningState()).thenReturn(null);
        } else {
            when(vmMock.provisioningState()).thenReturn(provisioningState.toString());
        }
        return vmMock;
    }
}
