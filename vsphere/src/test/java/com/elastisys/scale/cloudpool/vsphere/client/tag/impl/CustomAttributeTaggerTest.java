package com.elastisys.scale.cloudpool.vsphere.client.tag.impl;

import com.elastisys.scale.cloudpool.vsphere.client.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.client.tag.Tagging;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.CustomFieldValue;
import com.vmware.vim25.mo.VirtualMachine;
import org.junit.Before;
import org.junit.Test;

import java.rmi.RemoteException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomAttributeTaggerTest {

    private VirtualMachine virtualMachineMock;

    @Before
    public void setup(){
        virtualMachineMock = mock(VirtualMachine.class);
    }

    @Test
    public void newVirtualMachineShouldNotBeTagged() {
        assertFalse(new CustomAttributeTagger().isTagged(virtualMachineMock, Tag.CLOUD_POOL));
    }

    @Test
    public void taggedVirtualMachineShouldBeTagged() throws RemoteException {
        Tagging customAttributeTagger = new CustomAttributeTagger();
        CustomFieldStringValue cfsv = new CustomFieldStringValue();
        cfsv.setKey(1);
        cfsv.setValue("Set");
        customAttributeTagger.tag(virtualMachineMock, Tag.CLOUD_POOL);
        assertTrue(customAttributeTagger.isTagged((virtualMachineMock), Tag.CLOUD_POOL));
    }

}