package com.elastisys.scale.cloudadapters.openstack.scalinggroup;

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
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.scalinggroup.ScalingGroupException;
import com.elastisys.scale.cloudadapters.openstack.scalinggroup.client.OpenstackClient;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Verifies the behavior of the {@link OpenStackScalingGroup} with respect to
 * configuration.
 *
 * 
 *
 */
public class TestOpenStackScalingGroupConfiguration {

	private OpenstackClient mockClient = mock(OpenstackClient.class);
	/** Object under test. */
	private OpenStackScalingGroup scalingGroup;

	@Before
	public void onSetup() {
		this.scalingGroup = new OpenStackScalingGroup(this.mockClient);
	}

	@Test
	public void configureWithValidConfig() throws CloudAdapterException {
		assertFalse(this.scalingGroup.isConfigured());
		BaseCloudAdapterConfig config = loadCloudAdapterConfig("config/valid-openstackadapter-config.json");

		this.scalingGroup.configure(config);

		assertTrue(this.scalingGroup.isConfigured());
		OpenStackScalingGroupConfig expectedConfig = new OpenStackScalingGroupConfig(
				"http://nova.host.com:5000/v2.0", "RegionOne", "tenant",
				"clouduser", "cloudpass", true);
		assertThat(this.scalingGroup.config(), is(expectedConfig));
		assertThat(this.scalingGroup.getScalingGroupName(),
				is("my-scaling-group"));

		// verify that config is passed onto cloud client
		verify(this.mockClient).configure(expectedConfig);
	}

	@Test
	public void reconfigure() throws CloudAdapterException {
		// configure
		BaseCloudAdapterConfig config1 = loadCloudAdapterConfig("config/valid-openstackadapter-config.json");
		this.scalingGroup.configure(config1);
		assertThat(this.scalingGroup.config(),
				is(new OpenStackScalingGroupConfig(
						"http://nova.host.com:5000/v2.0", "RegionOne",
						"tenant", "clouduser", "cloudpass", true)));
		assertThat(this.scalingGroup.getScalingGroupName(),
				is("my-scaling-group"));

		// re-configure
		BaseCloudAdapterConfig config2 = loadCloudAdapterConfig("config/valid-openstackadapter-config2.json");
		this.scalingGroup.configure(config2);
		assertThat(this.scalingGroup.config(),
				is(new OpenStackScalingGroupConfig(
						"http://nova.host.com:5000/v2.0", "RegionTwo",
						"otherTenant", "clouduser", "cloudpass", false)));
		assertThat(this.scalingGroup.getScalingGroupName(),
				is("my-other-scaling-group"));
	}

	@Test(expected = IllegalStateException.class)
	public void invokeStartMachineBeforeBeingConfigured()
			throws CloudAdapterException {
		ScaleUpConfig scaleUpConfig = new ScaleUpConfig("size", "image",
				"keyPair", Arrays.asList("web"),
				Arrays.asList("apt-get update"));
		this.scalingGroup.startMachines(2, scaleUpConfig);
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
	public void configureWithConfigMissingKeystoneEndpoint() throws Exception {
		BaseCloudAdapterConfig config = loadCloudAdapterConfig("config/invalid-openstackadapter-config-missing-keystoneendpoint.json");
		this.scalingGroup.configure(config);
	}

	@Test(expected = ScalingGroupException.class)
	public void configureWithConfigMissingRegion() throws Exception {
		BaseCloudAdapterConfig config = loadCloudAdapterConfig("config/invalid-openstackadapter-config-missing-region.json");
		this.scalingGroup.configure(config);
	}

	@Test(expected = ScalingGroupException.class)
	public void configureWithConfigMissingTenant() throws Exception {
		BaseCloudAdapterConfig config = loadCloudAdapterConfig("config/invalid-openstackadapter-config-missing-tenant.json");
		this.scalingGroup.configure(config);
	}

	@Test(expected = ScalingGroupException.class)
	public void configureWithConfigMissingUserName() throws Exception {
		BaseCloudAdapterConfig config = loadCloudAdapterConfig("config/invalid-openstackadapter-config-missing-username.json");
		this.scalingGroup.configure(config);
	}

	@Test(expected = ScalingGroupException.class)
	public void configureWithConfigMissingPassword() throws Exception {
		BaseCloudAdapterConfig config = loadCloudAdapterConfig("config/invalid-openstackadapter-config-missing-password.json");
		this.scalingGroup.configure(config);
	}

	private BaseCloudAdapterConfig loadCloudAdapterConfig(String resourcePath) {
		return JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath),
				BaseCloudAdapterConfig.class);
	}
}
