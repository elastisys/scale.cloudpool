package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.TestUtils;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;

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

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        DriverConfig driverConfig = TestUtils.loadDriverConfig("config/valid-vsphere-config.json");
        vsphereApiSettings = driverConfig.parseCloudApiSettings(VsphereApiSettings.class);
        vsphereProvisioningTemplate = driverConfig.parseProvisioningTemplate(VsphereProvisioningTemplate.class);
        vsphereClient = spy(new StandardVsphereClient());
    }

    @Before
    public void setUp() throws Exception {
        mockServiceInstance = mock(ServiceInstance.class);
        doReturn(mockServiceInstance).when(vsphereClient).createServiceInstance(any(URL.class), anyString(),
                anyString(), anyBoolean());
        mockInventoryNavigator = mock(InventoryNavigator.class);
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
        List<VirtualMachine> virtualMachines = vsphereClient.getVirtualMachines();
        assertEquals(virtualMachines.size(), 1);
    }

}
