package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.elastisys.scale.cloudpool.vsphere.util.MockedVm;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class TestDestroyTask {

    VirtualMachine virtualMachine;
    Task powerOffTask;
    Task destroyVmTask;

    @Before
    public void setUp() throws Exception {
        this.virtualMachine = new MockedVm().withPowerState(VirtualMachinePowerState.poweredOn).build();
        this.powerOffTask = mock(Task.class);
        this.destroyVmTask = mock(Task.class);
        when(virtualMachine.powerOffVM_Task()).thenReturn(powerOffTask);
        when(powerOffTask.waitForTask()).thenReturn("success");
        when(virtualMachine.destroy_Task()).thenReturn(destroyVmTask);
        when(destroyVmTask.waitForTask()).thenReturn("success");
    }

    @Test(expected = NullPointerException.class)
    public void testWithNoVirtualMachine() {
        new DestroyTask(null);
    }

    @Test
    public void testWithValidVirtualMachine() throws Exception {
        DestroyTask destroyTask = new DestroyTask(virtualMachine);
        String result = destroyTask.call();
        assertEquals(result, TaskInfoState.success.name());
        verify(powerOffTask, times(1)).waitForTask();
        verify(destroyVmTask, times(1)).waitForTask();
    }

    @Test
    public void testWithFailedPowerOffTask() throws Exception {
        DestroyTask destroyTask = new DestroyTask(virtualMachine);
        when(powerOffTask.waitForTask()).thenReturn("error");
        String result = destroyTask.call();
        assertEquals(result, TaskInfoState.error.name());
        verify(destroyVmTask, times(0)).waitForTask();
    }

    @Test
    public void testWithFailedDestroyTask() throws Exception {
        DestroyTask destroyTask = new DestroyTask(virtualMachine);
        when(destroyVmTask.waitForTask()).thenReturn("error");
        String result = destroyTask.call();
        assertEquals(result, TaskInfoState.error.name());
        verify(destroyVmTask, times(1)).waitForTask();
    }

}
