package com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup;

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
import com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.client.AutoScalingClient;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Tests the behavior of the {@link AwsAsScalingGroup} with respect to
 * configuration.
 *
 * 
 *
 */
public class TestAwsAsScalingGroupConfiguration {

	/** Object under test. */
	private AwsAsScalingGroup scalingGroup;

	private AutoScalingClient awsClientMock = mock(AutoScalingClient.class);

	@Before
	public void onSetup() {
		this.scalingGroup = new AwsAsScalingGroup(this.awsClientMock);
	}

	@Test
	public void configureWithValidConfig() throws CloudAdapterException {
		assertFalse(this.scalingGroup.isConfigured());
		BaseCloudAdapterConfig config = loadCloudAdapterConfig("config/valid-awsasadapter-config.json");
		this.scalingGroup.configure(config);

		assertTrue(this.scalingGroup.isConfigured());
		AwsAsScalingGroupConfig expectedConfig = new AwsAsScalingGroupConfig(
				"ABC", "XYZ", "us-west-1");
		assertThat(this.scalingGroup.config(), is(expectedConfig));

		// verify that config is passed on to cloud client
		verify(this.awsClientMock).configure(expectedConfig);
	}

	@Test
	public void reconfigure() throws CloudAdapterException {
		// configure
		BaseCloudAdapterConfig config1 = loadCloudAdapterConfig("config/valid-awsasadapter-config.json");
		this.scalingGroup.configure(config1);
		assertThat(this.scalingGroup.config(), is(new AwsAsScalingGroupConfig(
				"ABC", "XYZ", "us-west-1")));

		// re-configure
		BaseCloudAdapterConfig config2 = loadCloudAdapterConfig("config/valid-awsasadapter-config2.json");
		this.scalingGroup.configure(config2);
		assertThat(this.scalingGroup.config(), is(new AwsAsScalingGroupConfig(
				"DEF", "TUV", "us-east-1")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void configureWithNull() throws CloudAdapterException {
		this.scalingGroup.configure(null);
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
		BaseCloudAdapterConfig config = loadCloudAdapterConfig("config/invalid-awsasadapter-config-missing-accesskeyid.json");
		this.scalingGroup.configure(config);
	}

	@Test(expected = ScalingGroupException.class)
	public void configureWithConfigMissingSecretAccessKey() throws Exception {
		BaseCloudAdapterConfig config = loadCloudAdapterConfig("config/invalid-awsasadapter-config-missing-secretaccesskey.json");
		this.scalingGroup.configure(config);
	}

	@Test(expected = ScalingGroupException.class)
	public void configureWithConfigMissingRegion() throws Exception {
		BaseCloudAdapterConfig config = loadCloudAdapterConfig("config/invalid-awsasadapter-config-missing-region.json");
		this.scalingGroup.configure(config);
	}

	private BaseCloudAdapterConfig loadCloudAdapterConfig(String resourcePath) {
		return JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath),
				BaseCloudAdapterConfig.class);
	}
}
