package com.elastisys.scale.cloudpool.openstack.driver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.types.CloudProviders;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.openstack.driver.client.OpenstackClient;
import com.elastisys.scale.cloudpool.openstack.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.openstack.driver.config.ProvisioningTemplate;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.openstack.AuthConfig;
import com.elastisys.scale.commons.openstack.AuthV2Credentials;
import com.elastisys.scale.commons.openstack.AuthV3Credentials;

/**
 * Verifies the behavior of the {@link OpenStackPoolDriver} with respect to
 * configuration.
 */
public class TestOpenStackPoolDriverConfiguration {

    private OpenstackClient mockClient = mock(OpenstackClient.class);
    /** Object under test. */
    private OpenStackPoolDriver driver;

    @Before
    public void onSetup() {
        this.driver = new OpenStackPoolDriver(this.mockClient, CloudProviders.OPENSTACK);
    }

    /**
     * {@link OpenStackPoolDriver} configured with version 2 authentication.
     */
    @Test
    public void configureWithV2Auth() {
        BaseCloudPoolConfig config = loadCloudPoolConfig("config/openstack-pool-config-authv2.json");
        this.driver.configure(driverConfig(config));

        CloudApiSettings expectedCloudApiSettings = new CloudApiSettings(
                new AuthConfig("http://nova.host.com:5000/v2.0",
                        new AuthV2Credentials("tenant", "clouduser", "cloudpass"), null),
                "RegionTwo");

        assertTrue(this.driver.isConfigured());
        assertThat(this.driver.getPoolName(), is("my-openstack-pool"));
        assertThat(this.driver.config(), is(driverConfig(config)));
        assertThat(this.driver.cloudApiSettings(), is(expectedCloudApiSettings));

        // verify that cloudApiSettings is passed onto cloud client
        verify(this.mockClient).configure(expectedCloudApiSettings);
    }

    /**
     * Validate that {@link OpenStackPoolDriver} can properly parse out its
     * {@link ProvisioningTemplate}.
     */
    @Test
    public void parseProvisioningTemplate() {
        BaseCloudPoolConfig config = loadCloudPoolConfig("config/openstack-pool-config-authv2.json");
        this.driver.configure(driverConfig(config));

        ProvisioningTemplate expectedVmTemplate = new ProvisioningTemplate("m1.small", "Ubuntu Server 16.04",
                "login-key", Arrays.asList("web"), "YXB0LWdldCB1cGRhdGUgLXF5ICYmIGFwdC1nZXQgaW5zdGFsbCBhcGFjaGUyCg==",
                Arrays.asList("private-net"), true);

        assertTrue(this.driver.isConfigured());
        assertThat(this.driver.provisioningTemplate(), is(expectedVmTemplate));
    }

    /**
     * {@link OpenStackPoolDriver} configured with version 3 authentication with
     * user and project being given by id.
     */
    @Test
    public void configureWithV3AuthById() {
        BaseCloudPoolConfig config = loadCloudPoolConfig("config/openstack-pool-config-authv3-by-id.json");
        this.driver.configure(driverConfig(config));

        AuthV3Credentials expectedCreds = new AuthV3Credentials("userId", null, null, null, "password", "projectId",
                null, null, null);
        CloudApiSettings expectedCloudApiSettings = new CloudApiSettings(
                new AuthConfig("http://nova.host.com:5000/v3", null, expectedCreds), "RegionTwo");

        assertTrue(this.driver.isConfigured());
        assertThat(this.driver.getPoolName(), is("my-openstack-pool2"));
        assertThat(this.driver.cloudApiSettings(), is(expectedCloudApiSettings));

        // verify that config is passed onto cloud client
        verify(this.mockClient).configure(expectedCloudApiSettings);
    }

    /**
     * {@link OpenStackPoolDriver} configured with version 3 authentication with
     * user and project being given by name.
     */
    @Test
    public void configureWithV3AuthByName() {
        BaseCloudPoolConfig config = loadCloudPoolConfig("config/openstack-pool-config-authv3-by-name.json");
        this.driver.configure(driverConfig(config));

        AuthV3Credentials expectedCreds = new AuthV3Credentials(null, "userName", "domainId", null, "password", null,
                "projectName", "domainName", null);
        CloudApiSettings expectedCloudApiSettings = new CloudApiSettings(
                new AuthConfig("http://nova.host.com:5000/v3", null, expectedCreds), "RegionTwo");

        assertTrue(this.driver.isConfigured());
        assertThat(this.driver.getPoolName(), is("my-openstack-pool3"));
        assertThat(this.driver.cloudApiSettings(), is(expectedCloudApiSettings));

        // verify that config is passed onto cloud client
        verify(this.mockClient).configure(expectedCloudApiSettings);
    }

    @Test
    public void reconfigure() throws CloudPoolException {
        assertFalse(this.driver.isConfigured());

        // configure
        BaseCloudPoolConfig config1 = loadCloudPoolConfig("config/openstack-pool-config-authv3-by-id.json");
        this.driver.configure(driverConfig(config1));
        assertThat(this.driver.getPoolName(), is("my-openstack-pool2"));
        assertThat(this.driver.config(), is(driverConfig(config1)));

        // re-configure
        BaseCloudPoolConfig config2 = loadCloudPoolConfig("config/openstack-pool-config-authv2.json");
        this.driver.configure(driverConfig(config2));
        assertThat(this.driver.getPoolName(), is("my-openstack-pool"));
        assertThat(this.driver.config(), is(driverConfig(config2)));
    }

    @Test(expected = IllegalStateException.class)
    public void invokeStartMachineBeforeBeingConfigured() throws CloudPoolException {
        this.driver.startMachines(2);
    }

    @Test(expected = IllegalStateException.class)
    public void invokeListMachinesBeforeBeingConfigured() throws CloudPoolException {
        this.driver.listMachines();
    }

    @Test(expected = IllegalStateException.class)
    public void invokeTerminateMachineBeforeBeingConfigured() throws Exception {
        this.driver.terminateMachines(Arrays.asList("i-1"));
    }

    @Test(expected = IllegalStateException.class)
    public void invokeAttachMachineBeforeBeingConfigured() throws Exception {
        this.driver.attachMachine("i-1");
    }

    @Test(expected = IllegalStateException.class)
    public void invokeDetachMachineBeforeBeingConfigured() throws Exception {
        this.driver.detachMachine("i-1");
    }

    @Test(expected = IllegalStateException.class)
    public void invokeSetServiceStateBeforeBeingConfigured() throws Exception {
        this.driver.setServiceState("i-1", ServiceState.IN_SERVICE);
    }

    @Test(expected = IllegalStateException.class)
    public void invokeSetMembershipStatusStateBeforeBeingConfigured() throws Exception {
        this.driver.setMembershipStatus("i-1", MembershipStatus.defaultStatus());
    }

    @Test(expected = IllegalStateException.class)
    public void invokeGetPoolNameBeforeBeingConfigured() throws Exception {
        this.driver.getPoolName();
    }

    @Test
    public void configureMissingAuth() {
        try {
            configureDriver("config/config-missing-auth.json");
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("auth"));
        }
    }

    @Test
    public void configureWithMissingAuthCredentials() {
        try {
            configureDriver("config/authv2-missing-credentials.json");
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("neither v2Credentials nor v3Credentials were given"));
        }
    }

    @Test
    public void configureWithAuthV2MissingKeystoneUrl() {
        try {
            configureDriver("config/authv2-missing-keystone.json");
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("keystoneUrl"));
        }
    }

    @Test
    public void configureWithAuthV2MissingUser() {
        try {
            configureDriver("config/authv2-missing-user.json");
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("userName"));
        }
    }

    @Test
    public void configureWithAuthV2MissingTenant() {
        try {
            configureDriver("config/authv2-missing-tenant.json");
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("tenantName"));
        }
    }

    @Test
    public void configureWithAuthV2MissingPassword() {
        try {
            configureDriver("config/authv2-missing-password.json");
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("password"));
        }
    }

    @Test
    public void configureWithAuthV3MissingKeystoneUrl() {
        try {
            configureDriver("config/authv3-missing-keystone.json");
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("keystoneUrl"));
        }
    }

    @Test
    public void configureWithAuthV3MissingPassword() {
        try {
            configureDriver("config/authv3-missing-password.json");
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("password"));
        }
    }

    @Test
    public void configureWithAuthV3MissingProject() {
        try {
            configureDriver("config/authv3-missing-project.json");
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("v3Credentials: one of projectId and projectName required"));
        }
    }

    @Test
    public void configureWithAuthV3MissingProjectDomain() {
        try {
            configureDriver("config/authv3-missing-project-domain.json");
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage()
                    .contains("when projectName is given, one of projectDomainId and projectDomainName is required"));
        }
    }

    @Test
    public void configureWithAuthV3MissingUser() {
        try {
            configureDriver("config/authv3-missing-user.json");
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("one of userId and userName required"));
        }
    }

    @Test
    public void configureWithAuthV3MissingUserDomain() {
        try {
            configureDriver("config/authv3-missing-user-domain.json");
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage()
                    .contains("when userName is given, one of userDomainId and userDomainName is required"));
        }
    }

    void configureDriver(String configResourcePath) {
        this.driver.configure(driverConfig(loadCloudPoolConfig(configResourcePath)));
    }

    private BaseCloudPoolConfig loadCloudPoolConfig(String resourcePath) {
        return JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath), BaseCloudPoolConfig.class);
    }

    /**
     * Creates a {@link DriverConfig} from a given {@link BaseCloudPoolConfig}.
     *
     * @param config
     * @return
     */
    private DriverConfig driverConfig(BaseCloudPoolConfig config) {
        return new DriverConfig(config.getName(), config.getCloudApiSettings(), config.getProvisioningTemplate());
    }

}
