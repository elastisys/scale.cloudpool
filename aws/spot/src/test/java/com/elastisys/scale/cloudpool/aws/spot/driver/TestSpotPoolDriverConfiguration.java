package com.elastisys.scale.cloudpool.aws.spot.driver;

import static com.elastisys.scale.cloudpool.aws.spot.driver.IsClientConfigMatcher.isClientConfig;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;

/**
 * Verifies the behavior of the {@link SpotPoolDriver} with respect to
 * configuration.
 */
public class TestSpotPoolDriverConfiguration {

    /** Mocked EC2 client. */
    private SpotClient mockClient = mock(SpotClient.class);
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
    /** Mocked event bus. */
    private EventBus mockEventBus = mock(EventBus.class);
    /** Object under test. */
    private SpotPoolDriver driver;

    /**
     *
     */
    @Before
    public void onSetup() {
        this.driver = new SpotPoolDriver(this.mockClient, this.executor, this.mockEventBus);
    }

    @Test
    public void configureWithCompleteDriverConfig() throws CloudPoolException {
        assertThat(this.driver.config(), is(nullValue()));

        DriverConfig config = driverConfig(loadCloudPoolConfig("config/complete-driver-config.json"));
        this.driver.configure(config);
        assertThat(this.driver.getPoolName(), is(config.getPoolName()));

        CloudApiSettings expectedApiSettings = new CloudApiSettings("ABC", "XYZ", "us-west-1", 0.0070,
                new TimeInterval(35L, TimeUnit.SECONDS), new TimeInterval(45L, TimeUnit.SECONDS), 7000, 5000);
        assertThat(this.driver.cloudApiSettings(), is(expectedApiSettings));

        // check the provisioning template
        Ec2ProvisioningTemplate expectedTemplate = new Ec2ProvisioningTemplate("m1.small", "ami-018c9568",
                asList("subnet-44b5786b", "subnet-dcd15f97"), true, "instancekey",
                "arn:aws:iam::123456789012:instance-profile/my-iam-profile", asList("sg-12345678"),
                "IyEvYmluL2Jhc2gKCnN1ZG8gYXB0LWdldCB1cGRhdGUgLXF5CnN1ZG8gYXB0LWdldCBpbnN0YWxsIC1xeSBhcGFjaGUyCg==",
                true, ImmutableMap.of("Cluster", "my-cluster"));
        assertThat(this.driver.provisioningTemplate(), is(expectedTemplate));

        // verify that driver calls through to configure spot client
        // appropriately
        verify(this.mockClient).configure(argThat(is("ABC")), argThat(is("XYZ")), argThat(is("us-west-1")),
                argThat(is(isClientConfig(7000, 5000))));
    }

    @Test
    public void configureWithDefaults() throws CloudPoolException {
        DriverConfig config = driverConfig(loadCloudPoolConfig("config/config-relying-on-defaults.json"));
        this.driver.configure(config);

        CloudApiSettings expectedApiSettings = new CloudApiSettings("ABC", "XYZ", "us-west-1", 0.0070,
                CloudApiSettings.DEFAULT_BID_REPLACEMENT_PERIOD,
                CloudApiSettings.DEFAULT_DANGLING_INSTANCE_CLEANUP_PERIOD, CloudApiSettings.DEFAULT_CONNECTION_TIMEOUT,
                CloudApiSettings.DEFAULT_SOCKET_TIMEOUT);
        Ec2ProvisioningTemplate expectedTemplate = new Ec2ProvisioningTemplate("m1.small", "ami-018c9568",
                asList("subnet-44b5786b", "subnet-dcd15f97"), null, null, null, null, null, null, null);

        assertThat(this.driver.cloudApiSettings(), is(expectedApiSettings));
        assertThat(this.driver.provisioningTemplate(), is(expectedTemplate));

        // verify that configuration was passed on to cloud client
        verify(this.mockClient).configure(argThat(is("ABC")), argThat(is("XYZ")), argThat(is("us-west-1")), argThat(
                isClientConfig(CloudApiSettings.DEFAULT_CONNECTION_TIMEOUT, CloudApiSettings.DEFAULT_SOCKET_TIMEOUT)));
    }

    /**
     * Make sure nothing brakes when applying a new configuration.
     */
    @Test
    public void reconfigure() throws CloudPoolException {
        // configure
        DriverConfig config1 = driverConfig(loadCloudPoolConfig("config/config-relying-on-defaults.json"));
        this.driver.configure(config1);
        CloudApiSettings expectedApiSettings = new CloudApiSettings("ABC", "XYZ", "us-west-1", 0.0070,
                CloudApiSettings.DEFAULT_BID_REPLACEMENT_PERIOD,
                CloudApiSettings.DEFAULT_DANGLING_INSTANCE_CLEANUP_PERIOD, CloudApiSettings.DEFAULT_CONNECTION_TIMEOUT,
                CloudApiSettings.DEFAULT_SOCKET_TIMEOUT);
        assertThat(this.driver.cloudApiSettings(), is(expectedApiSettings));

        // re-configure
        DriverConfig config2 = driverConfig(loadCloudPoolConfig("config/complete-driver-config.json"));
        this.driver.configure(config2);
        expectedApiSettings = new CloudApiSettings("ABC", "XYZ", "us-west-1", 0.0070,
                new TimeInterval(35L, TimeUnit.SECONDS), new TimeInterval(45L, TimeUnit.SECONDS), 7000, 5000);
        assertThat(this.driver.cloudApiSettings(), is(expectedApiSettings));
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
    public void configureWithConfigMissingBidPrice() throws Exception {
        try {
            DriverConfig config = driverConfig(loadCloudPoolConfig("config/invalid-config-missing-bidprice.json"));
            this.driver.configure(config);
            fail("expected failure");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("bidPrice"));
        }
    }

    @Test
    public void configureWithConfigMissingInstanceType() throws Exception {
        try {
            DriverConfig config = driverConfig(loadCloudPoolConfig("config/invalid-config-missing-instancetype.json"));
            this.driver.configure(config);
            fail("expected failure");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("instanceType"));
        }
    }

    @Test
    public void configureWithConfigMissingAmiId() throws Exception {
        try {
            DriverConfig config = driverConfig(loadCloudPoolConfig("config/invalid-config-missing-ami.json"));
            this.driver.configure(config);
            fail("expected failure");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("amiId"));
        }
    }

    @Test
    public void configureWithConfigMissingSubnets() throws Exception {
        try {
            DriverConfig config = driverConfig(loadCloudPoolConfig("config/invalid-config-missing-subnets.json"));
            this.driver.configure(config);
            fail("expected failure");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("subnetId"));
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
