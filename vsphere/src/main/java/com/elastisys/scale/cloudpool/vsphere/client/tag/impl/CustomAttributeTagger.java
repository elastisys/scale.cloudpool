package com.elastisys.scale.cloudpool.vsphere.client.tag.impl;

import com.elastisys.scale.cloudpool.vsphere.client.tag.Tagger;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.CustomFieldValue;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

import java.rmi.RemoteException;

public class CustomAttributeTagger implements Tagger {

    @Override
    public boolean isTagged(ManagedEntity me, String tag) {
        CustomFieldValue[] cvfArr = me.getCustomValue();
        if(cvfArr == null) {
            return false;
        }
        for (CustomFieldValue cfv : cvfArr) {
            CustomFieldStringValue cfsv = (CustomFieldStringValue) cfv;
            if(cfsv.getValue().equals(TagValues.Set)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tag(ManagedEntity me, String tag) throws RemoteException {
        VirtualMachine vm = (VirtualMachine) me;
        vm.setCustomValue(tag, TagValues.Set);
    }

    private static final class TagValues {
        public static final String Set = "Set";
        public static final String Unset = "Unset";
    }

}