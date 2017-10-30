package com.elastisys.scale.cloudpool.aws.autoscaling.driver;

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
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.client.AutoScalingClient;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.config.ProvisioningTemplate;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Tests the behavior of the {@link AwsAsPoolDriver} with respect to
 * configuration.
 */
public class TestAwsAsDriverConfiguration {

    /** Object under test. */
    private AwsAsPoolDriver driver;

    private AutoScalingClient awsClientMock = mock(AutoScalingClient.class);

    @Before
    public void onSetup() {
        this.driver = new AwsAsPoolDriver(this.awsClientMock);
    }

    @Test
    public void configureWithValidConfig() throws CloudPoolException {
        assertFalse(this.driver.isConfigured());
        DriverConfig driverConfig = driverConfig(loadCloudPoolConfig("config/valid-awsasdriver-config.json"));
        this.driver.configure(driverConfig);

        assertTrue(this.driver.isConfigured());
        CloudApiSettings expectedApiSettings = new CloudApiSettings("ABC", "XYZ", "us-west-1");
        assertThat(this.driver.cloudApiSettings(), is(expectedApiSettings));
        assertThat(JsonUtils.toJson(this.driver.provisioningTemplate()), is(driverConfig.getProvisioningTemplate()));

        // verify that config is passed on to cloud client
        verify(this.awsClientMock).configure(expectedApiSettings);
    }

    @Test
    public void reconfigure() throws CloudPoolException {
        // configure
        DriverConfig config1 = driverConfig(loadCloudPoolConfig("config/valid-awsasdriver-config.json"));
        this.driver.configure(config1);
        assertThat(this.driver.cloudApiSettings(), is(new CloudApiSettings("ABC", "XYZ", "us-west-1")));

        // re-configure
        DriverConfig config2 = driverConfig(loadCloudPoolConfig("config/valid-awsasdriver-config2.json"));
        this.driver.configure(config2);
        assertThat(this.driver.cloudApiSettings(), is(new CloudApiSettings("DEF", "TUV", "us-east-1", 5000, 7000)));

    }

    /**
     * An explicit Auto Scaling Group to manage can be specified in the
     * {@link ProvisioningTemplate}.
     */
    @Test
    public void withExplicitAutoScalingGroup() {
        DriverConfig driverConfig = driverConfig(loadCloudPoolConfig("config/valid-awsasdriver-config.json"));
        this.driver.configure(driverConfig);
        assertThat(this.driver.provisioningTemplate().getAutoScalingGroup().get(), is("mygroup"));
        assertThat(this.driver.scalingGroupName(), is("mygroup"));
    }

    /**
     * If no explicit Auto Scaling Group to manage is specified in the
     * {@link ProvisioningTemplate} the default behavior is to use the cloud
     * pool name.
     */
    @Test
    public void withDefaultAutoScalingGroup() {
        DriverConfig driverConfig = driverConfig(loadCloudPoolConfig("config/valid-awsasdriver-config2.json"));
        this.driver.configure(driverConfig);
        assertThat(this.driver.provisioningTemplate().getAutoScalingGroup().isPresent(), is(false));
        assertThat(this.driver.scalingGroupName(), is("ec2-cluster"));
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
        this.driver.terminateMachines(Arrays.asList("i-1"));
    }

    @Test
    public void configureWithConfigMissingAccessKeyId() throws Exception {
        try {
            DriverConfig driverConfig = driverConfig(
                    loadCloudPoolConfig("config/invalid-awsasdriver-config-missing-accesskeyid.json"));
            this.driver.configure(driverConfig);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("awsAccessKeyId"));
        }
    }

    @Test
    public void configureWithConfigMissingSecretAccessKey() throws Exception {
        try {
            DriverConfig driverConfig = driverConfig(
                    loadCloudPoolConfig("config/invalid-awsasdriver-config-missing-secretaccesskey.json"));
            this.driver.configure(driverConfig);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("awsSecretAccessKey"));
        }
    }

    @Test
    public void configureWithConfigMissingRegion() throws Exception {
        try {
            DriverConfig driverConfig = driverConfig(
                    loadCloudPoolConfig("config/invalid-awsasdriver-config-missing-region.json"));
            this.driver.configure(driverConfig);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {

            assertTrue(e.getMessage().contains("region"));
        }
    }

    private DriverConfig driverConfig(BaseCloudPoolConfig config) {
        return new DriverConfig(config.getName(), config.getCloudApiSettings(), config.getProvisioningTemplate());
    }

    private BaseCloudPoolConfig loadCloudPoolConfig(String resourcePath) {
        return JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath), BaseCloudPoolConfig.class);
    }
}
