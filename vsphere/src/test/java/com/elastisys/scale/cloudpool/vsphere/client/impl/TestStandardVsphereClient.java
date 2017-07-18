package com.elastisys.scale.cloudpool.vsphere.client.impl;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.TestUtils;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;

import com.vmware.vim25.mo.ServiceInstance;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.net.URL;

public class TestStandardVsphereClient {

    private static StandardVsphereClient vsphereClient;
    private static VsphereApiSettings vsphereApiSettings;
    private static VsphereProvisioningTemplate vsphereProvisioningTemplate;
    private static ServiceInstance mockServiceInstance;

    @BeforeClass
    public static void setUpBeforeClass() {
        mockServiceInstance = mock(ServiceInstance.class);
        DriverConfig driverConfig = TestUtils.loadDriverConfig("config/valid-vsphere-config.json");
        vsphereApiSettings = driverConfig.parseCloudApiSettings(VsphereApiSettings.class);
        vsphereProvisioningTemplate = driverConfig.parseProvisioningTemplate(VsphereProvisioningTemplate.class);
    }

    @Before
    public void setUp() throws Exception {
        vsphereClient = spy(new StandardVsphereClient());
    }

    @Test
    public void testConfigure() throws Exception {
        doReturn(mockServiceInstance).when(vsphereClient).createServiceInstance(Mockito.any(URL.class), Matchers.anyString(),
                Matchers.anyString(), Matchers.anyBoolean());
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
    }

}
