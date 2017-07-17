package com.elastisys.scale.cloudpool.vsphere.client.tag.impl;

import com.elastisys.scale.cloudpool.vsphere.client.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.client.tag.Tagger;
import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.CustomFieldValue;
import com.vmware.vim25.mo.*;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class CustomAttributeTagger implements Tagger {

    @Override
    public void initialize(ServiceInstance si) throws RemoteException {
        CustomFieldsManager customFieldsManager = si.getCustomFieldsManager();
        List<CustomFieldDef> cfdList = Arrays.asList(customFieldsManager.getField());
        HashSet<String> tags = Tag.getTags();
        List<String> tagDefinitions = cfdList.stream().map(CustomFieldDef::getName).collect(Collectors.toList());
        for(String tag : tags) {
            if(!tagDefinitions.contains(tag)) {
                customFieldsManager.addCustomFieldDef(tag, VirtualMachine.class.getSimpleName(), null, null);
            }
        }
    }

    @Override
    public boolean isTagged(ManagedEntity me, String tag) throws RemoteException {
        CustomFieldValue[] cfvArr = me.getCustomValue();
        CustomFieldDef[] cfdArr = me.getAvailableField();
        if (cfvArr == null || cfdArr == null) {
            return false;
        }
        Integer key = -1;
        for(CustomFieldDef def : cfdArr) {
            if(tag.equals(def.getName())){
                key = def.getKey();
            }
        }
        if(key == -1){
            return false;
        }
        for(CustomFieldValue cfv : cfvArr){
            CustomFieldStringValue cfsv = (CustomFieldStringValue) cfv;
            if(cfv.getKey() == key && cfsv.getValue().equals(TagValues.Set)) {
                return true;
            }
        }
        return false;

        /*
        if(cfvArr == null) {
            return false;
        }
        for (CustomFieldValue cfv : cfvArr) {
            CustomFieldStringValue cfsv = (CustomFieldStringValue) cfv;
            // TODO: use cfsv.getKey() to find the right custom attribute
            if(cfsv.getValue().equals(TagValues.Set)) {
                return true;
            }
        }
        return false;
        */
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