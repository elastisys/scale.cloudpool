package com.elastisys.scale.cloudpool.vsphere.client.tag.impl;

import com.elastisys.scale.cloudpool.vsphere.client.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.client.tag.Tagger;
import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.mo.VirtualMachine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.rmi.RemoteException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomAttributeTaggerTest {

    private VirtualMachine virtualMachineMock;
    private Tagger customAttributeTagger;

    @Before
    public void setup(){
        virtualMachineMock = mock(VirtualMachine.class);
        customAttributeTagger = new CustomAttributeTagger();
    }

    @Test
    public void newVirtualMachineShouldNotBeTagged() throws RemoteException {
        CustomFieldDef[] cfdArr = {};
        when(virtualMachineMock.getAvailableField()).thenReturn(cfdArr);
        assertFalse(customAttributeTagger.isTagged(virtualMachineMock, Tag.CLOUD_POOL));
    }

    @Test
    public void tagWithoutError() throws RemoteException {
        customAttributeTagger.tag(virtualMachineMock, Tag.CLOUD_POOL);
    }

    @Test(expected = RemoteException.class)
    public void nonInstantiatedVirtualMachineShouldThrowException() throws RemoteException {
        Mockito.doThrow(new RemoteException()).when(virtualMachineMock).setCustomValue(anyString(), anyString());
        customAttributeTagger.tag(virtualMachineMock, Tag.CLOUD_POOL);
    }

    @Test
    public void taggedVirtualMachineShouldBeTagged() throws RemoteException {
        CustomFieldStringValue cfsv = new CustomFieldStringValue();
        cfsv.setKey(1);
        cfsv.setValue("Set");
        CustomFieldStringValue[] cfsvArr = {cfsv};
        CustomFieldDef cfd = new CustomFieldDef();
        cfd.setName(Tag.CLOUD_POOL);
        cfd.setKey(1);
        CustomFieldDef[] cfdArr = {cfd};
        when(virtualMachineMock.getAvailableField()).thenReturn(cfdArr);
        when(virtualMachineMock.getCustomValue()).thenReturn(cfsvArr);
        assertTrue(customAttributeTagger.isTagged((virtualMachineMock), Tag.CLOUD_POOL));
    }

}