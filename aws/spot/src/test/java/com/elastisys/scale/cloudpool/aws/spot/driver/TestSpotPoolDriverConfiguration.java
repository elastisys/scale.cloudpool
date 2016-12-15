package com.elastisys.scale.cloudpool.aws.spot.driver;

import static com.elastisys.scale.cloudpool.aws.spot.driver.IsClientConfigMatcher.isClientConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
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
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2ProvisioningTemplate;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.SpotClient;
import com.elastisys.scale.cloudpool.aws.spot.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.eventbus.EventBus;

/**
 * Verifies the behavior of the {@link SpotPoolDriver} with respect to
 * configuration.
 */
public class TestSpotPoolDriverConfiguration {

    /** Mocked EC2 client. */
    private SpotClient mockClient = mock(SpotClient.class);
    /** Mocked event bus. */
    private EventBus mockEventBus = mock(EventBus.class);
    /** Object under test. */
    private SpotPoolDriver driver;

    /**
     *
     */
    @Before
    public void onSetup() {
        this.driver = new SpotPoolDriver(this.mockClient, this.mockEventBus);
    }

    @Test
    public void configureWithCompleteDriverConfig() throws CloudPoolException {
        assertThat(this.driver.config(), is(nullValue()));

        DriverConfig config = driverConfig(loadCloudPoolConfig("config/complete-driver-config.json"));
        this.driver.configure(config);
        assertThat(this.driver.getPoolName(), is(config.getPoolName()));

        CloudApiSettings expectedApiSettings = new CloudApiSettings("ABC", "XYZ", "us-west-1", 0.0070, 35L, 45L, 7000,
                5000);
        assertThat(this.driver.cloudApiSettings(), is(expectedApiSettings));

        // check the provisioning template
        Ec2ProvisioningTemplate expectedProvisioningTemplate = new Ec2ProvisioningTemplate("m1.small", "ami-018c9568",
                "instancekey", Arrays.asList("webserver"),
                "IyEvYmluL2Jhc2gKCnN1ZG8gYXB0LWdldCB1cGRhdGUgLXF5CnN1ZG8gYXB0LWdldCBpbnN0YWxsIC1xeSBhcGFjaGUyCg==");
        assertThat(this.driver.provisioningTemplate(), is(expectedProvisioningTemplate));

        // verify that driver calls through to configure spot client
        // appropriately
        verify(this.mockClient).configure(argThat(is("ABC")), argThat(is("XYZ")), argThat(is("us-west-1")),
                argThat(is(isClientConfig(7000, 5000))));
    }

    /**
     * Verify that a default value is used.
     */
    @Test
    public void configureWithMissingBidReplacementPeriod() {
        DriverConfig config = driverConfig(loadCloudPoolConfig("config/valid-config-relying-on-defaults.json"));
        this.driver.configure(config);
        assertThat(this.driver.cloudApiSettings().getBidReplacementPeriod(),
                is(CloudApiSettings.DEFAULT_BID_REPLACEMENT_PERIOD));
    }

    /**
     * Verify that a default value is used.
     */
    @Test
    public void configureWithMissingDanglingInstanceCleanupPeriod() {
        DriverConfig config = driverConfig(loadCloudPoolConfig("config/valid-config-relying-on-defaults.json"));
        this.driver.configure(config);
        assertThat(this.driver.cloudApiSettings().getDanglingInstanceCleanupPeriod(),
                is(CloudApiSettings.DEFAULT_DANGLING_INSTANCE_CLEANUP_PERIOD));
    }

    /**
     * Make sure nothing brakes when applying a new configuration.
     */
    @Test
    public void reconfigure() throws CloudPoolException {
        // configure
        DriverConfig config1 = driverConfig(loadCloudPoolConfig("config/valid-config-relying-on-defaults.json"));
        this.driver.configure(config1);
        assertThat(this.driver.cloudApiSettings(),
                is(new CloudApiSettings("ABC", "XYZ", "us-west-1", 0.0070, null, null)));

        // re-configure
        DriverConfig config2 = driverConfig(loadCloudPoolConfig("config/complete-driver-config.json"));
        this.driver.configure(config2);
        assertThat(this.driver.cloudApiSettings(),
                is(new CloudApiSettings("ABC", "XYZ", "us-west-1", 0.0070, 35L, 45L, 7000, 5000)));
    }

    @Test
    public void configureWithConfigMissingAccessKeyId() throws Exception {
        try {
            DriverConfig config = driverConfig(loadCloudPoolConfig("config/invalid-config-missing-accesskeyid.json"));
            this.driver.configure(config);
            fail("expected failure");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("awsAccessKeyId"));
        }

    }

    @Test
    public void configureWithConfigMissingSecretAccessKey() throws Exception {
        try {
            DriverConfig config = driverConfig(
                    loadCloudPoolConfig("config/invalid-config-missing-secretaccesskey.json"));
            this.driver.configure(config);
            fail("expected failure");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("awsSecretAccessKey"));
        }
    }

    @Test
    public void configureWithConfigMissingRegion() throws Exception {
        try {
            DriverConfig config = driverConfig(loadCloudPoolConfig("config/invalid-config-missing-region.json"));
            this.driver.configure(config);
            fail("expected failure");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("region"));
        }
    }

    @Test
    public void configureWithConfigMissingbidPrice() throws Exception {
        try {
            DriverConfig config = driverConfig(loadCloudPoolConfig("config/invalid-config-missing-bidprice.json"));
            this.driver.configure(config);
            fail("expected failure");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("bidPrice"));
        }
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
    public void invokeAttachMachineBeforeBeingConfigured() throws Exception {
        this.driver.attachMachine("sir-1");
    }

    @Test(expected = IllegalStateException.class)
    public void invokeDetachMachineBeforeBeingConfigured() throws Exception {
        this.driver.detachMachine("sir-1");
    }

    @Test(expected = IllegalStateException.class)
    public void invokeSetServiceStateBeforeBeingConfigured() throws Exception {
        this.driver.setServiceState("sir-1", ServiceState.BOOTING);
    }

    @Test(expected = IllegalStateException.class)
    public void invokeSetMembershipStatusBeforeBeingConfigured() throws Exception {
        this.driver.setMembershipStatus("sir-1", MembershipStatus.blessed());
    }

    /**
     * Creates a {@link DriverConfig} from a {@link BaseCloudPoolConfig}.
     *
     * @param config
     * @return
     */
    private DriverConfig driverConfig(BaseCloudPoolConfig config) {
        return new DriverConfig(config.getName(), config.getCloudApiSettings(), config.getProvisioningTemplate());
    }

    private BaseCloudPoolConfig loadCloudPoolConfig(String resourcePath) {
        return JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath), BaseCloudPoolConfig.class);
    }

}
