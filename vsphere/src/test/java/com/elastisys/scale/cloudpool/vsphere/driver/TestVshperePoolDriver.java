package com.elastisys.scale.cloudpool.vsphere.driver;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.any;

import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

public class TestVshperePoolDriver {

    private VspherePoolDriver driver;
    private VsphereClient mockedClient = mock(VsphereClient.class);
    private String minimalConfigPath = "config/minimal-valid-vsphere-config.json";
    private String specificConfigPath = "config/valid-vsphere-config.json";
    private String missingUrlConfig = "config/missing-url-vsphere-config.json";
    private String missingUsernameConfig = "config/missing-username-vsphere-config.json";
    private String missingPasswordConfig = "config/missing-password-vsphere-config.json";
    private String missingTemplateConfig = "config/missing-template-vsphere-config.json";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        driver = new VspherePoolDriver(mockedClient);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testVspherePoolDriver() {
        fail("Not yet implemented");
    }

    @Test
    public void configuredDriverShouldBeConfigured() {
        DriverConfig configuration = loadDriverConfig(specificConfigPath);
        driver.configure(configuration);
        assertTrue(this.driver.isConfigured());
        verify(this.mockedClient).configure(any(VsphereApiSettings.class), any(VsphereProvisioningTemplate.class));
    }

    @Test
    public void minimalDriverConfiguration() {
        DriverConfig configuration = loadDriverConfig(minimalConfigPath);
        driver.configure(configuration);
        assertTrue(this.driver.isConfigured());
        verify(this.mockedClient).configure(any(VsphereApiSettings.class), any(VsphereProvisioningTemplate.class));
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
