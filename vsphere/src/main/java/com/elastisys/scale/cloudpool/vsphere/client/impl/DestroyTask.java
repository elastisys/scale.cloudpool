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
 * This class implements the Callable interface and may be delegated to an Executor.
 */
public class DestroyTask implements Callable {
    VirtualMachine virtualMachine;

    public DestroyTask(VirtualMachine virtualMachine) {
        checkNotNull(virtualMachine);
        this.virtualMachine = virtualMachine;
    }

    /**
     * This method contains the synchronous work necessary to power off and remove a VirtualMachine.
     *
     * @return A String signifying the final status of the task.
     * @throws RemoteException      This exception is thrown if an error occurred in communication with Vcenter.
     * @throws InterruptedException This exception will be thrown if the waiting thread was interrupted.
     */
    @Override
    public String call() throws RemoteException, InterruptedException {
        VirtualMachinePowerState powerState = virtualMachine.getRuntime().getPowerState();
        String result;
        if (powerState == VirtualMachinePowerState.poweredOn) {
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
