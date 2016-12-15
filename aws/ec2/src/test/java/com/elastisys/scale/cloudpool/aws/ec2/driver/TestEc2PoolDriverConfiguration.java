package com.elastisys.scale.cloudpool.aws.ec2.driver;

import static com.elastisys.scale.cloudpool.aws.ec2.driver.IsClientConfigMatcher.isClientConfig;
import static com.elastisys.scale.cloudpool.aws.ec2.driver.TestUtils.driverConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2Client;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2ProvisioningTemplate;
import com.elastisys.scale.cloudpool.aws.ec2.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Verifies the behavior of the {@link Ec2PoolDriver} with respect to
 * configuration.
 */
public class TestEc2PoolDriverConfiguration {
    private Ec2Client mockClient = mock(Ec2Client.class);
    /** Object under test. */
    private Ec2PoolDriver driver;

    @Before
    public void onSetup() {
        this.driver = new Ec2PoolDriver(this.mockClient);
    }

    @Test
    public void configureWithValidConfig() throws CloudPoolException {
        assertFalse(this.driver.isConfigured());
        DriverConfig config = driverConfig(loadCloudPoolConfig("config/valid-ec2pool-config.json"));
        this.driver.configure(config);

        CloudApiSettings expectedApiSettings = new CloudApiSettings("ABC", "XYZ", "us-west-1");
        Ec2ProvisioningTemplate expectedProvisioningTemplate = new Ec2ProvisioningTemplate("m1.small", "ami-018c9568",
                "instancekey", Arrays.asList("webserver"),
                "IyEvYmluL2Jhc2gKCnN1ZG8gYXB0LWdldCB1cGRhdGUgLXF5CnN1ZG8gYXB0LWdldCBpbnN0YWxsIC1xeSBhcGFjaGUyCg==");

        assertTrue(this.driver.isConfigured());
        assertThat(this.driver.cloudApiSettings(), is(expectedApiSettings));

        assertThat(this.driver.provisioningTemplate(), is(expectedProvisioningTemplate));

        // verify that configuration was passed on to cloud client
        verify(this.mockClient).configure(argThat(is("ABC")), argThat(is("XYZ")), argThat(is("us-west-1")), argThat(
                isClientConfig(CloudApiSettings.DEFAULT_CONNECTION_TIMEOUT, CloudApiSettings.DEFAULT_SOCKET_TIMEOUT)));

    }

    @Test
    public void reconfigure() throws CloudPoolException {
        // configure
        DriverConfig config1 = driverConfig(loadCloudPoolConfig("config/valid-ec2pool-config.json"));
        this.driver.configure(config1);
        assertThat(this.driver.cloudApiSettings(), is(new CloudApiSettings("ABC", "XYZ", "us-west-1")));

        // re-configure
        DriverConfig config2 = driverConfig(loadCloudPoolConfig("config/valid-ec2pool-config2.json"));
        this.driver.configure(config2);
        assertThat(this.driver.cloudApiSettings(), is(new CloudApiSettings("DEF", "TUV", "us-east-1", 7000, 5000)));
    }

    @Test(expected = IllegalStateException.class)
    public void invokeStartMachineBeforeBeingConfigured() throws CloudPoolException {
        this.driver.startMachines(3);
    }

    @Test(expected = IllegalStateException.class)
    public void invokeListMachinesBeforeBeingConfigured() throws CloudPoolException {
        this.driver.listMachines();
    }

    @Test(expected = IllegalStateException.class)
    public void invokeTerminateMachineBeforeBeingConfigured() throws Exception {
        this.driver.terminateMachine("i-1");
    }

    @Test
    public void configureWithConfigMissingAccessKeyId() throws Exception {
        try {
            DriverConfig config = driverConfig(
                    loadCloudPoolConfig("config/invalid-ec2pool-config-missing-accesskeyid.json"));
            this.driver.configure(config);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("awsAccessKeyId"));
        }
    }

    @Test
    public void configureWithConfigMissingSecretAccessKey() throws Exception {
        try {
            DriverConfig config = driverConfig(
                    loadCloudPoolConfig("config/invalid-ec2pool-config-missing-secretaccesskey.json"));
            this.driver.configure(config);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("awsSecretAccessKey"));
        }
    }

    @Test
    public void configureWithConfigMissingRegion() throws Exception {
        try {
            DriverConfig config = driverConfig(
                    loadCloudPoolConfig("config/invalid-ec2pool-config-missing-region.json"));
            this.driver.configure(config);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("region"));
        }
    }

    private BaseCloudPoolConfig loadCloudPoolConfig(String resourcePath) {
        return JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath), BaseCloudPoolConfig.class);
    }
}
