package com.elastisys.scale.cloudpool.vsphere.driver;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.any;

import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

import java.rmi.RemoteException;

public class TestVspherePoolDriver {

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
    public void testVspherePoolDriver() {
        fail("Not yet implemented");
    }

    @Test
    public void configuredDriverShouldBeConfigured() throws RemoteException{
        DriverConfig configuration = loadDriverConfig(specificConfigPath);
        assertFalse(driver.isConfigured());
        driver.configure(configuration);
        assertTrue(driver.isConfigured());
        verify(mockedClient).configure(any(VsphereApiSettings.class), any(VsphereProvisioningTemplate.class));
    }

    @Test
    public void minimalDriverConfiguration() throws RemoteException{
        DriverConfig configuration = loadDriverConfig(minimalConfigPath);
        assertFalse(driver.isConfigured());
        driver.configure(configuration);
        assertTrue(driver.isConfigured());
        verify(mockedClient).configure(any(VsphereApiSettings.class), any(VsphereProvisioningTemplate.class));
    }

    @Test
    public void reconfigure() throws RemoteException {
        DriverConfig configuration1 = loadDriverConfig(minimalConfigPath);
        DriverConfig configuration2 = loadDriverConfig(specificConfigPath);

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
            DriverConfig config = loadDriverConfig(missingUrlConfig);
            driver.configure(config);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("URL"));
        }
        assertFalse(driver.isConfigured());
    }

    @Test
    public void configureWithMissingUsername() {
        try {
            DriverConfig config = loadDriverConfig(missingUsernameConfig);
            driver.configure(config);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("username"));
        }
        assertFalse(driver.isConfigured());
    }

    @Test
    public void configureWithMissingPassword() {
        try {
            DriverConfig config = loadDriverConfig(missingPasswordConfig);
            driver.configure(config);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("password"));
        }
        assertFalse(driver.isConfigured());
    }

    @Test
    public void configureWithMissingTemplate() {
        try {
            DriverConfig config = loadDriverConfig(missingTemplateConfig);
            driver.configure(config);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("template"));
        }
        assertFalse(driver.isConfigured());
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
    public void testListMachines() {
        fail("Not yet implemented");
    }

    @Test
    public void testStartMachines() {
        fail("Not yet implemented");
    }

    @Test
    public void testTerminateMachine() {
        fail("Not yet implemented");
    }

    @Test
    public void testAttachMachine() {
        fail("Not yet implemented");
    }

    @Test
    public void testDetachMachine() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetServiceState() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetMembershipStatus() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetPoolName() {
        fail("Not yet implemented");
    }

    private DriverConfig loadDriverConfig(String resourcePath) {
        BaseCloudPoolConfig baseConfig = JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath),
                BaseCloudPoolConfig.class);
        JsonObject cloudApiSettings = JsonUtils.toJson(baseConfig.getCloudApiSettings()).getAsJsonObject();
        JsonObject provisioningTemplate = JsonUtils.toJson(baseConfig.getProvisioningTemplate()).getAsJsonObject();
        DriverConfig config = new DriverConfig(baseConfig.getName(), cloudApiSettings, provisioningTemplate);
        return config;
    }

}
