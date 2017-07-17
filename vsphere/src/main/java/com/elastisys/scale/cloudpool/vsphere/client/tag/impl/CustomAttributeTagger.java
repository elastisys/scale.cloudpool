package com.elastisys.scale.cloudpool.vsphere.client.tag.impl;

import com.elastisys.scale.cloudpool.api.NotFoundException;
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
        try {
            if (getCustomValue(me, tag).equals(TagValues.Set)) {
                return true;
            }
            return false;
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public void tag(ManagedEntity me, String tag) throws RemoteException {
        VirtualMachine vm = (VirtualMachine) me;
        vm.setCustomValue(tag, TagValues.Set);
    }

    @Override
    public void untag(ManagedEntity me, String tag) throws RemoteException {
        VirtualMachine vm = (VirtualMachine) me;
        vm.setCustomValue(tag, TagValues.Unset);
    }

    private String getCustomValue(ManagedEntity me, String definition) throws RemoteException, NotFoundException {
        CustomFieldValue[] cfvArr = me.getCustomValue();
        CustomFieldDef[] cfdArr = me.getAvailableField();
        Integer key = -1;
        if (cfvArr == null || cfdArr == null) {
            throw new NotFoundException();
        }

        // fetch the key for the CustomFieldDefinition
        for(CustomFieldDef def : cfdArr) {
            if(definition.equals(def.getName())){
                key = def.getKey();
            }
        }
        if(key == -1){
            throw new NotFoundException();
        }

        // check if the key has a corresponding CustomFieldValue
        for(CustomFieldValue cfv : cfvArr){
            CustomFieldStringValue cfsv = (CustomFieldStringValue) cfv;
            if(cfv.getKey() == key) {
                return cfsv.getValue();
            }
        }
        throw new NotFoundException();
    }

    private static final class TagValues {
        public static final String Set = "Set";
        public static final String Unset = "Unset";
    }

}