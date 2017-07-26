package com.elastisys.scale.cloudpool.vsphere.driver;

import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.elastisys.scale.cloudpool.vsphere.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.rmi.RemoteException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TestVspherePoolDriverConfiguration {

    private VspherePoolDriver driver;
    private VsphereClient mockedClient = mock(VsphereClient.class);
    private String minimalConfigPath = "config/minimal-valid-vsphere-config.json";
    private String specificConfigPath = "config/valid-vsphere-config.json";
    private String missingUrlConfig = "config/missing-url-vsphere-config.json";
    private String missingUsernameConfig = "config/missing-username-vsphere-config.json";
    private String missingPasswordConfig = "config/missing-password-vsphere-config.json";
    private String missingTemplateConfig = "config/missing-template-vsphere-config.json";

    @Before
    public void setUp() throws Exception {
        driver = new VspherePoolDriver(mockedClient);
    }

    @Test
    public void configuredDriverShouldBeConfigured() throws RemoteException {
        DriverConfig configuration = TestUtils.loadDriverConfig(specificConfigPath);
        assertFalse(driver.isConfigured());
        driver.configure(configuration);
        assertTrue(driver.isConfigured());
        assertNotNull(driver.getPoolName());
        verify(mockedClient).configure(any(VsphereApiSettings.class), any(VsphereProvisioningTemplate.class));
    }

    @Test
    public void minimalDriverConfiguration() throws RemoteException {
        DriverConfig configuration = TestUtils.loadDriverConfig(minimalConfigPath);
        assertFalse(driver.isConfigured());
        driver.configure(configuration);
        assertTrue(driver.isConfigured());
        verify(mockedClient).configure(any(VsphereApiSettings.class), any(VsphereProvisioningTemplate.class));
    }

    @Test
    public void reconfigure() throws RemoteException {
        DriverConfig configuration1 = TestUtils.loadDriverConfig(minimalConfigPath);
        DriverConfig configuration2 = TestUtils.loadDriverConfig(specificConfigPath);

        assertFalse(driver.isConfigured());
        driver.configure(configuration1);
        assertTrue(driver.isConfigured());
        driver.configure(configuration2);
        assertTrue(driver.isConfigured());
        verify(mockedClient, times(2)).configure(any(VsphereApiSettings.class), any(VsphereProvisioningTemplate.class));
    }

    @Test
    public void configureWithMissingUrl() {
        try {
            DriverConfig configuration = TestUtils.loadDriverConfig(missingUrlConfig);
            driver.configure(configuration);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("URL"));
        }
        assertFalse(driver.isConfigured());
    }

    @Test
    public void configureWithMissingUsername() {
        try {
            DriverConfig configuration = TestUtils.loadDriverConfig(missingUsernameConfig);
            driver.configure(configuration);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("username"));
        }
        assertFalse(driver.isConfigured());
    }

    @Test
    public void configureWithMissingPassword() {
        try {
            DriverConfig configuration = TestUtils.loadDriverConfig(missingPasswordConfig);
            driver.configure(configuration);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("password"));
        }
        assertFalse(driver.isConfigured());
    }

    @Test
    public void configureWithMissingTemplate() {
        try {
            DriverConfig configuration = TestUtils.loadDriverConfig(missingTemplateConfig);
            driver.configure(configuration);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("template"));
        }
        assertFalse(driver.isConfigured());
    }

    @Test(expected = CloudPoolDriverException.class)
    public void configureWithConnectionProblem() throws RemoteException {
        DriverConfig configuration = TestUtils.loadDriverConfig(specificConfigPath);
        doThrow(RemoteException.class).when(mockedClient).configure(any(), any());
        driver.configure(configuration);
    }

    @Test(expected = IllegalStateException.class)
    public void listMachinesWithoutConfig() {
        driver.listMachines();
    }

    @Test(expected = IllegalStateException.class)
    public void startMachinesWithoutConfig() {
        driver.startMachines(1);
    }

    @Test(expected = IllegalStateException.class)
    public void terminateMachineWithoutConfig() {
        driver.terminateMachine("machine-id");
    }

    @Test(expected = IllegalStateException.class)
    public void attachMachineWithoutConfig() {
        driver.attachMachine("machine-id");
    }

    @Test(expected = IllegalStateException.class)
    public void detachMachineWithoutConfig() {
        driver.detachMachine("machine-id");
    }

    @Test(expected = IllegalStateException.class)
    public void setServiceStateWithoutConfig() {
        driver.setServiceState("machine-id", ServiceState.IN_SERVICE);
    }

    @Test(expected = IllegalStateException.class)
    public void setMembershipStatusWithoutConfig() {
        driver.setMembershipStatus("machine-id", MembershipStatus.defaultStatus());
    }

    @Test(expected = IllegalStateException.class)
    public void getNameWithoutConfig() {
        driver.getPoolName();
    }

    @Test
    public void testGetPoolName() {
        DriverConfig configuration = TestUtils.loadDriverConfig(minimalConfigPath);
        driver.configure(configuration);
        assertEquals(configuration.getPoolName(), driver.getPoolName());
    }

}
