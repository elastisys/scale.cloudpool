package com.elastisys.scale.cloudpool.vsphere.tagger.impl;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.ScalingTag;
import com.elastisys.scale.cloudpool.vsphere.tagger.Tagger;
import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.CustomFieldValue;
import com.vmware.vim25.mo.CustomFieldsManager;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This implementation of the Tagger interface uses CustomAttributes to
 * implement tagging, since the Vsphere API does not yet support other kinds of
 * tagging.
 *
 * Note: This implementation requires that the tagger is initialized by calling
 * `initialize()` before usage.
 */
public class CustomAttributeTagger implements Tagger {

    /**
     * Initialize the Tagged by defining all CustomAttributes needed.
     * 
     * @param serviceInstance
     *            The ServiceInstance used for communicating with Vcenter.
     * @throws RemoteException
     *             if some error occurred in communication with Vcenter.
     */
    public void initialize(ServiceInstance serviceInstance) throws RemoteException {
        CustomFieldsManager customFieldsManager = serviceInstance.getCustomFieldsManager();
        List<CustomFieldDef> cfdList = Arrays.asList(customFieldsManager.getField());
        Collection<String> tags = getTags();
        List<String> tagDefinitions = cfdList.stream().map(CustomFieldDef::getName).collect(Collectors.toList());
        for (String tag : tags) {
            if (!tagDefinitions.contains(tag)) {
                customFieldsManager.addCustomFieldDef(tag, VirtualMachine.class.getSimpleName(), null, null);
            }
        }
    }

    @Override
    public boolean isTagged(ManagedEntity me, Tag tag) throws RemoteException {
        try {
            return getCustomValue(me, tag.getKey()).equals(tag.getValue());
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

    /**
     * Extract the value corresponding to a definition string.
     * 
     * @param me
     *            The ManagedEntity to extract the value from.
     * @param definition
     *            The definition string used as key.
     * @return The value.
     * @throws RemoteException
     *             if some error occurred in communication with Vcenter.
     * @throws NotFoundException
     *             if the ManagedEntity cannot be found.
     */
    private String getCustomValue(ManagedEntity me, String definition) throws RemoteException, NotFoundException {
        CustomFieldValue[] cfvArr = me.getCustomValue();
        CustomFieldDef[] cfdArr = me.getAvailableField();
        Integer key = -1;
        if (cfvArr == null || cfdArr == null) {
            throw new NotFoundException();
        }

        // fetch the key for the CustomFieldDefinition
        for (CustomFieldDef def : cfdArr) {
            if (definition.equals(def.getName())) {
                key = def.getKey();
            }
        }
        if (key == -1) {
            throw new NotFoundException();
        }

        // check if the key has a corresponding CustomFieldValue
        for (CustomFieldValue cfv : cfvArr) {
            CustomFieldStringValue cfsv = (CustomFieldStringValue) cfv;
            if (cfv.getKey() == key) {
                return cfsv.getValue();
            }
        }
        throw new NotFoundException();
    }

    /**
     * All tags used in the pool. This method can be useful for initializing all
     * the tags.
     * 
     * @return A collection of tag definitions.
     */
    Collection<String> getTags() {
        return ScalingTag.getValues();
    }

}