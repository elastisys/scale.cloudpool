package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tagger.Tagger;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Auxiliary class that is responsible for synchronously waiting on Vcenter to clone a VirtualMachine.
 */
public class CloneTask implements Callable {

    private Tagger tagger;
    private Task task;
    private List<Tag> tags;

    public CloneTask(Tagger tagger, Task task, List<Tag> tags) {
        checkNotNull(tagger);
        checkNotNull(task);
        checkNotNull(tags);
        this.tagger = tagger;
        this.task = task;
        this.tags = tags;
    }

    @Override
    public String call() throws RemoteException, InterruptedException {
        String result = task.waitForTask();
        if (result.equals(TaskInfoState.success.name())) {
            ManagedObjectReference mor = (ManagedObjectReference) task.getTaskInfo().getResult();
            VirtualMachine virtualMachine = new VirtualMachine(task.getServerConnection(), mor);
            for (Tag tag : tags) {
                tagger.tag(virtualMachine, tag);
            }

        }
        return result;
    }
}
