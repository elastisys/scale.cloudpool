package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.ScalingTag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.VsphereTag;
import com.elastisys.scale.cloudpool.vsphere.util.MockedVm;
import com.google.common.collect.Lists;
import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.mo.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static com.elastisys.scale.cloudpool.vsphere.util.TestUtils.loadDriverConfig;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class TestStandardVsphereClient {

    private static StandardVsphereClient vsphereClient;
    private static VsphereApiSettings vsphereApiSettings;
    private static VsphereProvisioningTemplate vsphereProvisioningTemplate;

    private static ServiceInstance mockServiceInstance;
    private static InventoryNavigator mockInventoryNavigator;
    private static CustomFieldsManager mockCustomFieldsManager;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        DriverConfig driverConfig = loadDriverConfig("config/valid-vsphere-config.json");
        vsphereApiSettings = driverConfig.parseCloudApiSettings(VsphereApiSettings.class);
        vsphereProvisioningTemplate = driverConfig.parseProvisioningTemplate(VsphereProvisioningTemplate.class);
        vsphereClient = spy(new StandardVsphereClient());
    }

    @Before
    public void setUp() throws Exception {
        mockServiceInstance = mock(ServiceInstance.class);
        mockCustomFieldsManager = mock(CustomFieldsManager.class);
        mockInventoryNavigator = mock(InventoryNavigator.class);
        doReturn(mockServiceInstance).when(vsphereClient).createServiceInstance(any(URL.class), anyString(),
                anyString(), anyBoolean());
        when(mockServiceInstance.getCustomFieldsManager()).thenReturn(mockCustomFieldsManager);
        when(mockCustomFieldsManager.getField()).thenReturn(new CustomFieldDef[0]);
        doReturn(mockInventoryNavigator).when(vsphereClient).createInventoryNavigator(any(Folder.class));
        doReturn(mock(Folder.class)).when(mockServiceInstance).getRootFolder();
    }

    @Test
    public void testConfigure() throws Exception {
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
    }

    @Test
    public void listMachinesShouldReturnListOfVms() throws Exception {
        ManagedEntity[] vms = {new MockedVm().build()};
        doReturn(vms).when(mockInventoryNavigator).searchManagedEntities(anyString());
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        List<VirtualMachine> virtualMachines = vsphereClient.getVirtualMachines(Lists.newArrayList());
        assertEquals(virtualMachines.size(), 1);
    }

    @Test
    public void launchMachinesShouldAddVms() throws Exception {
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        List<Tag> tags = Lists.newArrayList();
        Tag tag = new VsphereTag(ScalingTag.CLOUD_POOL, "StandardVsphereClientTestPool");
        tags.add(tag);
        VirtualMachine vm = new MockedVm().build();
        when(mockInventoryNavigator.searchManagedEntity(eq("VirtualMachine"), anyString())).thenReturn(vm);
        when(mockInventoryNavigator.searchManagedEntity(eq("Folder"), anyString())).thenReturn(mock(Folder.class));
        when(mockInventoryNavigator.searchManagedEntity(eq("ResourcePool"), anyString())).thenReturn(mock(ResourcePool.class));
        when(vm.cloneVM_Task(any(), anyString(), any())).thenReturn(mock(Task.class));
        List<VirtualMachine> virtualMachines = vsphereClient.launchVirtualMachines(2, tags);
        verify(vm, times(2)).cloneVM_Task(any(Folder.class), anyString(), any(VirtualMachineCloneSpec.class));
        assertEquals(virtualMachines.size(), 2);
    }

    @Test
    public void terminateMachinesShouldRemoveVms() throws Exception {
        VirtualMachine virtualMachine = new MockedVm().withName("Vm_destroy").build();
        VirtualMachine[] virtualMachineArr = {virtualMachine};
        when(mockInventoryNavigator.searchManagedEntity(eq("Folder"), anyString())).thenReturn(mock(Folder.class));
        when(mockInventoryNavigator.searchManagedEntities("VirtualMachine")).thenReturn(virtualMachineArr);
        when(virtualMachine.powerOffVM_Task()).thenReturn(mock(Task.class));
        when(virtualMachine.destroy_Task()).thenReturn(mock(Task.class));
        vsphereClient.terminateVirtualMachines(Arrays.asList("Vm_destroy"));
        verify(virtualMachine, times(1)).destroy_Task();
    }

}
