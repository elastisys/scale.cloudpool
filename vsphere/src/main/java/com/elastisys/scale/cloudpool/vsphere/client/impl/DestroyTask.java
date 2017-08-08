package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Auxiliary class that is responsible for synchronously waiting on Vcenter to power off and destroy a VirtualMachine.
 */
public class DestroyTask implements Callable {
    VirtualMachine virtualMachine;

    public DestroyTask(VirtualMachine virtualMachine) {
        checkNotNull(virtualMachine);
        this.virtualMachine = virtualMachine;
    }

    @Override
    public String call() throws RemoteException, InterruptedException {
        VirtualMachinePowerState powerState = virtualMachine.getRuntime().getPowerState();
        String result;
        if(powerState == VirtualMachinePowerState.poweredOn) {
            Task powerOffTask = virtualMachine.powerOffVM_Task();
            result = powerOffTask.waitForTask();
            if (!result.equals(TaskInfoState.success.name())) {
                return result;
            }
        }
        Task destroyTask = virtualMachine.destroy_Task();
        result = destroyTask.waitForTask();
        return result;
    }
}
