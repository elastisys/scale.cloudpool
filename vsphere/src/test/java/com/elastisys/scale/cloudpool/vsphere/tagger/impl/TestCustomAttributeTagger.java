package com.elastisys.scale.cloudpool.vsphere.tagger.impl;

import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.ScalingTag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.VsphereTag;
import com.elastisys.scale.cloudpool.vsphere.tagger.Tagger;
import com.elastisys.scale.cloudpool.vsphere.util.MockedVm;
import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.mo.VirtualMachine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.rmi.RemoteException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class TestCustomAttributeTagger {

    private Tagger customAttributeTagger;
    private Tag mockTag;

    @Before
    public void setup() throws Exception{
        customAttributeTagger = new CustomAttributeTagger();
        mockTag = new VsphereTag(ScalingTag.CLOUD_POOL, "MockedTag");
    }

    @Test
    public void newVirtualMachineShouldNotBeTagged() throws RemoteException {
        VirtualMachine mockVirtualMachine = new MockedVm().build();
        CustomFieldDef[] cfdArr = {};
        when(mockVirtualMachine.getAvailableField()).thenReturn(cfdArr);
        assertFalse(customAttributeTagger.isTagged(mockVirtualMachine, mockTag));
    }

    @Test
    public void tagWithoutError() throws RemoteException {
        VirtualMachine mockVirtualMachine = new MockedVm().build();
        customAttributeTagger.tag(mockVirtualMachine, mockTag);
    }

    @Test(expected = RemoteException.class)
    public void nonInstantiatedVirtualMachineShouldThrowException() throws RemoteException {
        VirtualMachine mockVirtualMachine = new MockedVm().build();
        Mockito.doThrow(new RemoteException()).when(mockVirtualMachine).setCustomValue(anyString(), anyString());
        customAttributeTagger.tag(mockVirtualMachine, mockTag);
    }

    @Test
    public void taggedVirtualMachineShouldBeTagged() throws RemoteException {
        VirtualMachine mockVirtualMachine = new MockedVm().withTag(mockTag).build();
        assertTrue(customAttributeTagger.isTagged(mockVirtualMachine, mockTag));
    }

    @Test
    public void untaggedVirtualMachineShouldNotBeTagged() throws RemoteException {
        VirtualMachine mockVirtualMachine = new MockedVm().build();
        customAttributeTagger.untag(mockVirtualMachine, mockTag);
        assertFalse(customAttributeTagger.isTagged(mockVirtualMachine, mockTag));
    }

}
