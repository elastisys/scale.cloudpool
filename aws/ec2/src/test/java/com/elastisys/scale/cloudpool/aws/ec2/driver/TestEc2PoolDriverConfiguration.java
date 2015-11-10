package com.elastisys.scale.cloudpool.aws.ec2.driver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2Client;
import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.base64.Base64Utils;

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
		BaseCloudPoolConfig config = loadCloudPoolConfig(
				"config/valid-ec2pool-config.json");
		this.driver.configure(config);

		assertTrue(this.driver.isConfigured());
		Ec2PoolDriverConfig expectedConfig = new Ec2PoolDriverConfig("ABC",
				"XYZ", "us-west-1");
		assertThat(this.driver.config(), is(expectedConfig));

		// verify that configuration was passed on to cloud client
		verify(this.mockClient).configure("ABC", "XYZ", "us-west-1");
	}

	@Test
	public void reconfigure() throws CloudPoolException {
		// configure
		BaseCloudPoolConfig config1 = loadCloudPoolConfig(
				"config/valid-ec2pool-config.json");
		this.driver.configure(config1);
		assertThat(this.driver.config(),
				is(new Ec2PoolDriverConfig("ABC", "XYZ", "us-west-1")));

		// re-configure
		BaseCloudPoolConfig config2 = loadCloudPoolConfig(
				"config/valid-ec2pool-config2.json");
		this.driver.configure(config2);
		assertThat(this.driver.config(),
				is(new Ec2PoolDriverConfig("DEF", "TUV", "us-east-1")));
	}

	@Test(expected = IllegalStateException.class)
	public void invokeStartMachineBeforeBeingConfigured()
			throws CloudPoolException {
		String encodedUserData = Base64Utils.toBase64("#!/bin/bash",
				"sudo apt-get update -qy", "sudo apt-get install -qy apache2");
		ScaleOutConfig scaleUpConfig = new ScaleOutConfig("size", "image",
				"keyPair", Arrays.asList("webserver"), encodedUserData);
		this.driver.startMachines(3, scaleUpConfig);
	}

	@Test(expected = IllegalStateException.class)
	public void invokeListMachinesBeforeBeingConfigured()
			throws CloudPoolException {
		this.driver.listMachines();
	}

	@Test(expected = IllegalStateException.class)
	public void invokeTerminateMachineBeforeBeingConfigured() throws Exception {
		this.driver.terminateMachine("i-1");
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithConfigMissingAccessKeyId() throws Exception {
		BaseCloudPoolConfig config = loadCloudPoolConfig(
				"config/invalid-ec2pool-config-missing-accesskeyid.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithConfigMissingSecretAccessKey() throws Exception {
		BaseCloudPoolConfig config = loadCloudPoolConfig(
				"config/invalid-ec2pool-config-missing-secretaccesskey.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithConfigMissingRegion() throws Exception {
		BaseCloudPoolConfig config = loadCloudPoolConfig(
				"config/invalid-ec2pool-config-missing-region.json");
		this.driver.configure(config);
	}

	private BaseCloudPoolConfig loadCloudPoolConfig(String resourcePath) {
		return JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath),
				BaseCloudPoolConfig.class);
	}
}
