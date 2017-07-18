package com.elastisys.scale.cloudpool.vsphere.client.tagger.impl;

import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.client.tagger.Tagger;
import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.mo.VirtualMachine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.rmi.RemoteException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestCustomAttributeTagger {

    private VirtualMachine mockVirtualMachine;
    private Tagger customAttributeTagger;

    @Before
    public void setup(){
        mockVirtualMachine = mock(VirtualMachine.class);
        customAttributeTagger = new CustomAttributeTagger();
    }

    @Test
    public void newVirtualMachineShouldNotBeTagged() throws RemoteException {
        CustomFieldDef[] cfdArr = {};
        when(mockVirtualMachine.getAvailableField()).thenReturn(cfdArr);
        Tag mockTag = createMockTag("PoolName", "MyCloudPool");
        assertFalse(customAttributeTagger.isTagged(mockVirtualMachine, mockTag));
    }

    @Test
    public void tagWithoutError() throws RemoteException {
        Tag mockTag = createMockTag("PoolName", "MyCloudPool");
        customAttributeTagger.tag(mockVirtualMachine, mockTag);
    }

    @Test(expected = RemoteException.class)
    public void nonInstantiatedVirtualMachineShouldThrowException() throws RemoteException {
        Mockito.doThrow(new RemoteException()).when(mockVirtualMachine).setCustomValue(anyString(), anyString());
        Tag mockTag = createMockTag("PoolName", "MyCloudPool");
        customAttributeTagger.tag(mockVirtualMachine, mockTag);
    }

    @Test
    public void taggedVirtualMachineShouldBeTagged() throws RemoteException {
        Tag mockTag = createMockTag("PoolName", "MyCloudPool");
        setupMockTags(mockTag.getKey(), mockTag.getValue());
        assertTrue(customAttributeTagger.isTagged(mockVirtualMachine, mockTag));
    }

    @Test
    public void untaggedVirtualMachineShouldNotBeTagged() throws RemoteException {
        Tag mockTag = createMockTag("PoolName", "MyCloudPool");
        setupMockTags("PoolName", "");
        customAttributeTagger.untag(mockVirtualMachine, mockTag);
        assertFalse(customAttributeTagger.isTagged(mockVirtualMachine, mockTag));
    }

    private Tag createMockTag(String key, String value) {
        Tag mockTag = mock(Tag.class);
        doReturn(key).when(mockTag).getKey();
        doReturn(value).when(mockTag).getValue();
        return mockTag;
    }

    private void setupMockTags(String tag, String value) throws RemoteException {
        int key = 1;
        CustomFieldStringValue cfsv = new CustomFieldStringValue();
        cfsv.setKey(key);
        cfsv.setValue(value);
        CustomFieldStringValue[] cfsvArr = {cfsv};
        CustomFieldDef cfd = new CustomFieldDef();
        cfd.setName(tag);
        cfd.setKey(key);
        CustomFieldDef[] cfdArr = {cfd};
        when(mockVirtualMachine.getAvailableField()).thenReturn(cfdArr);
        when(mockVirtualMachine.getCustomValue()).thenReturn(cfsvArr);
    }

}
