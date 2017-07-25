package com.elastisys.scale.cloudpool.vsphere.tagger.impl;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.ScalingTag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.VsphereTag;
import com.elastisys.scale.cloudpool.vsphere.tagger.Tagger;
import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.CustomFieldValue;
import com.vmware.vim25.mo.*;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CustomAttributeTagger implements Tagger {

    public void initialize(ServiceInstance si) throws RemoteException {
        CustomFieldsManager customFieldsManager = si.getCustomFieldsManager();
        List<CustomFieldDef> cfdList = Arrays.asList(customFieldsManager.getField());
        Collection<String> tags = getTags();
        List<String> tagDefinitions = cfdList.stream().map(CustomFieldDef::getName).collect(Collectors.toList());
        for(String tag : tags) {
            if(!tagDefinitions.contains(tag)) {
                customFieldsManager.addCustomFieldDef(tag, VirtualMachine.class.getSimpleName(), null, null);
            }
        }
    }

    @Override
    public boolean isTagged(ManagedEntity me, Tag vsphereTag) throws RemoteException {
        try {
            if (getCustomValue(me, vsphereTag.getKey()).equals(vsphereTag.getValue())) {
                return true;
            }
            return false;
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public void tag(ManagedEntity me, Tag tag) throws RemoteException {
        VirtualMachine vm = (VirtualMachine) me;
        vm.setCustomValue(tag.getKey(), tag.getValue());
    }

    @Override
    public void untag(ManagedEntity me, Tag tag) throws RemoteException {
        VirtualMachine vm = (VirtualMachine) me;
        vm.setCustomValue(tag.getKey(), "");
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

    Collection<String> getTags() {
        return ScalingTag.getValues();
    }

}