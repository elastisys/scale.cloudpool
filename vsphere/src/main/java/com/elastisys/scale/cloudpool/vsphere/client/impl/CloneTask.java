package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tagger.Tagger;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Callable;

public class CloneTask implements Callable {

    private Tagger tagger;
    private Task task;
    private List<Tag> tags;

    public CloneTask(Tagger tagger, Task task, List<Tag> tags) {
        this.tagger = tagger;
        this.task = task;
        this.tags = tags;
    }

    @Override
    public VirtualMachine call() throws RemoteException, InterruptedException {
        task.waitForTask();
        ManagedObjectReference mor = (ManagedObjectReference) task.getTaskInfo().getResult();
        VirtualMachine virtualMachine = new VirtualMachine(task.getServerConnection(), mor);
        for (Tag tag : tags) {
            tagger.tag(virtualMachine, tag);
        }
        return virtualMachine;
    }
}
