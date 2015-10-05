package com.elastisys.scale.cloudpool.aws.spot.driver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.SpotClient;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
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
		assertThat(this.driver.driverConfig(), is(nullValue()));

		BaseCloudPoolConfig config = loadCloudPoolConfig("config/complete-driver-config.json");
		this.driver.configure(config);
		assertThat(this.driver.poolConfig(), is(config));

		assertThat(this.driver.driverConfig().getAwsAccessKeyId(), is("ABC"));
		assertThat(this.driver.driverConfig().getAwsSecretAccessKey(),
				is("XYZ"));
		assertThat(this.driver.driverConfig().getRegion(), is("us-west-1"));
		assertThat(this.driver.driverConfig().getBidPrice(), is(0.0070));
		assertThat(this.driver.driverConfig().getBidReplacementPeriod(),
				is(35L));
		assertThat(this.driver.driverConfig()
				.getDanglingInstanceCleanupPeriod(), is(45L));

	}

	/**
	 * Verify that a default value is used.
	 */
	@Test
	public void configureWithMissingBidReplacementPeriod() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/valid-config-relying-on-defaults.json");
		this.driver.configure(config);
		assertThat(this.driver.driverConfig().getBidReplacementPeriod(),
				is(SpotPoolDriverConfig.DEFAULT_BID_REPLACEMENT_PERIOD));
	}

	/**
	 * Verify that a default value is used.
	 */
	@Test
	public void configureWithMissingDanglingInstanceCleanupPeriod() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/valid-config-relying-on-defaults.json");
		this.driver.configure(config);
		assertThat(
				this.driver.driverConfig().getDanglingInstanceCleanupPeriod(),
				is(SpotPoolDriverConfig.DEFAULT_DANGLING_INSTANCE_CLEANUP_PERIOD));
	}

	/**
	 * Make sure nothing brakes when applying a new configuration.
	 */
	@Test
	public void reconfigure() throws CloudPoolException {
		// configure
		BaseCloudPoolConfig config1 = loadCloudPoolConfig("config/valid-config-relying-on-defaults.json");
		this.driver.configure(config1);
		assertThat(this.driver.poolConfig(), is(config1));

		// re-configure
		BaseCloudPoolConfig config2 = loadCloudPoolConfig("config/complete-driver-config.json");
		this.driver.configure(config2);
		assertThat(this.driver.poolConfig(), is(config2));
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithConfigMissingAccessKeyId() throws Exception {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/invalid-config-missing-accesskeyid.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithConfigMissingSecretAccessKey() throws Exception {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/invalid-config-missing-secretaccesskey.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithConfigMissingRegion() throws Exception {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/invalid-config-missing-region.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithConfigMissingbidPrice() throws Exception {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/invalid-config-missing-bidprice.json");
		this.driver.configure(config);
	}

	@Test(expected = IllegalStateException.class)
	public void invokeStartMachineBeforeBeingConfigured()
			throws CloudPoolException {
		ScaleOutConfig scaleUpConfig = new ScaleOutConfig("size", "image",
				"keyPair", Arrays.asList("webserver"), Arrays.asList(
						"#!/bin/bash", "apt-get update"));
		this.driver.startMachines(3, scaleUpConfig);
	}

	@Test(expected = IllegalStateException.class)
	public void invokeListMachinesBeforeBeingConfigured()
			throws CloudPoolException {
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
	public void invokeSetMembershipStatusBeforeBeingConfigured()
			throws Exception {
		this.driver.setMembershipStatus("sir-1", MembershipStatus.blessed());
	}

	private BaseCloudPoolConfig loadCloudPoolConfig(String resourcePath) {
		return JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath),
				BaseCloudPoolConfig.class);
	}

}
