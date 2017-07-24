package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.collect.Lists;
import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.mo.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;
import java.util.List;

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
        DriverConfig driverConfig = JsonUtils.toObject(JsonUtils.parseJsonResource("config/valid-vsphere-config.json"), DriverConfig.class);
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
        ManagedEntity[] vms = {mock(VirtualMachine.class)};
        doReturn(vms).when(mockInventoryNavigator).searchManagedEntities(anyString());
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
        List<VirtualMachine> virtualMachines = vsphereClient.getVirtualMachines(Lists.newArrayList());
        assertEquals(virtualMachines.size(), 1);
    }

}
