package com.elastisys.scale.cloudpool.vsphere.driver;

import static org.junit.Assert.*;

import com.elastisys.scale.commons.json.JsonUtils;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.any;

import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;

import java.rmi.RemoteException;

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
        DriverConfig configuration = JsonUtils.toObject(JsonUtils.parseJsonResource(specificConfigPath), DriverConfig.class);
        assertFalse(driver.isConfigured());
        driver.configure(configuration);
        assertTrue(driver.isConfigured());
        verify(mockedClient).configure(any(VsphereApiSettings.class), any(VsphereProvisioningTemplate.class));
    }

    @Test
    public void minimalDriverConfiguration() throws RemoteException {
        DriverConfig configuration = JsonUtils.toObject(JsonUtils.parseJsonResource(minimalConfigPath), DriverConfig.class);
        assertFalse(driver.isConfigured());
        driver.configure(configuration);
        assertTrue(driver.isConfigured());
        verify(mockedClient).configure(any(VsphereApiSettings.class), any(VsphereProvisioningTemplate.class));
    }

    @Test
    public void reconfigure() throws RemoteException {
        DriverConfig configuration1 = JsonUtils.toObject(JsonUtils.parseJsonResource(minimalConfigPath), DriverConfig.class);
        DriverConfig configuration2 = JsonUtils.toObject(JsonUtils.parseJsonResource(specificConfigPath), DriverConfig.class);

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
            DriverConfig configuration = JsonUtils.toObject(JsonUtils.parseJsonResource(missingUrlConfig), DriverConfig.class);
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
            DriverConfig configuration = JsonUtils.toObject(JsonUtils.parseJsonResource(missingUsernameConfig), DriverConfig.class);
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
            DriverConfig configuration = JsonUtils.toObject(JsonUtils.parseJsonResource(missingPasswordConfig), DriverConfig.class);
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
            DriverConfig configuration = JsonUtils.toObject(JsonUtils.parseJsonResource(missingTemplateConfig), DriverConfig.class);
            driver.configure(configuration);
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

}
