package com.elastisys.scale.cloudpool.vsphere.client.tag.impl;

import com.elastisys.scale.cloudpool.vsphere.client.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.client.tag.Tagging;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.VirtualMachine;

import java.rmi.RemoteException;

public class CustomAttributeTagger implements Tagging {

    @Override
    public boolean isTagged(ManagedEntity me, String tag) {
        return false;
    }

    @Override
    public void tag(ManagedEntity me, String tag) throws RemoteException {
        VirtualMachine vm = (VirtualMachine) me;
        vm.setCustomValue(tag.toString(), TagValues.Set);
    }

    private final class TagValues {
        public static final String Set = "Set";
    }

}