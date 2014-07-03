package com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.CloudAdapterException;
import com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.client.Ec2Client;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Verifies the behavior of the {@link Ec2ScalingGroup} with respect to
 * configuration.
 *
 * 
 *
 */
public class TestEc2ScalingGroupConfiguration {
	private Ec2Client mockClient = mock(Ec2Client.class);
	/** Object under test. */
	private Ec2ScalingGroup scalingGroup;

	@Before
	public void onSetup() {
		this.scalingGroup = new Ec2ScalingGroup(this.mockClient);
	}

	@Test
	public void configureWithValidConfig() throws CloudAdapterException {
		assertFalse(this.scalingGroup.isConfigured());
		BaseCloudAdapterConfig config = loadCloudAdapterConfig("config/valid-ec2adapter-config.json");
		this.scalingGroup.configure(config);

		assertTrue(this.scalingGroup.isConfigured());
		Ec2ScalingGroupConfig expectedConfig = new Ec2ScalingGroupConfig("ABC",
				"XYZ", "us-west-1");
		assertThat(this.scalingGroup.config(), is(expectedConfig));

		// verify that configuration was passed on to cloud client
		verify(this.mockClient).configure(expectedConfig);
	}

	@Test
	public void reconfigure() throws CloudAdapterException {
		// configure
		BaseCloudAdapterConfig config1 = loadCloudAdapterConfig("config/valid-ec2adapter-config.json");
		this.scalingGroup.configure(config1);
		assertThat(this.scalingGroup.config(), is(new Ec2ScalingGroupConfig(
				"ABC", "XYZ", "us-west-1")));

		// re-configure
		BaseCloudAdapterConfig config2 = loadCloudAdapterConfig("config/valid-ec2adapter-config2.json");
		this.scalingGroup.configure(config2);
		assertThat(this.scalingGroup.config(), is(new Ec2ScalingGroupConfig(
				"DEF", "TUV", "us-east-1")));
	}

	@Test(expected = IllegalStateException.class)
	public void invokeStartMachineBeforeBeingConfigured()
			throws CloudAdapterException {
		ScaleUpConfig scaleUpConfig = new BaseCloudAdapterConfig.ScaleUpConfig(
				"size", "image", "keyPair", Arrays.asList("webserver"),
				Arrays.asList("#!/bin/bash", "apt-get update"));
		this.scalingGroup.startMachines(3, scaleUpConfig);
	}

	@Test(expected = IllegalStateException.class)
	public void invokeListMachinesBeforeBeingConfigured()
			throws CloudAdapterException {
		this.scalingGroup.listMachines();
	}

	@Test(expected = IllegalStateException.class)
	public void invokeTerminateMachineBeforeBeingConfigured() throws Exception {
		this.scalingGroup.terminateMachine("i-1");
	}

	@Test(expected = ScalingGroupException.class)
	public void configureWithConfigMissingAccessKeyId() throws Exception {
		BaseCloudAdapterConfig config = loadCloudAdapterConfig("config/invalid-ec2adapter-config-missing-accesskeyid.json");
		this.scalingGroup.configure(config);
	}

	@Test(expected = ScalingGroupException.class)
	public void configureWithConfigMissingSecretAccessKey() throws Exception {
		BaseCloudAdapterConfig config = loadCloudAdapterConfig("config/invalid-ec2adapter-config-missing-secretaccesskey.json");
		this.scalingGroup.configure(config);
	}

	@Test(expected = ScalingGroupException.class)
	public void configureWithConfigMissingRegion() throws Exception {
		BaseCloudAdapterConfig config = loadCloudAdapterConfig("config/invalid-ec2adapter-config-missing-region.json");
		this.scalingGroup.configure(config);
	}

	private BaseCloudAdapterConfig loadCloudAdapterConfig(String resourcePath) {
		return JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath),
				BaseCloudAdapterConfig.class);
	}
}
