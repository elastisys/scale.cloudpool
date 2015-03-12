package com.elastisys.scale.cloudpool.openstack.driver;

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
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.openstack.driver.client.OpenstackClient;
import com.elastisys.scale.commons.json.JsonUtils;

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
		this.driver = new OpenStackPoolDriver(this.mockClient);
	}

	@Test
	public void configureWithValidConfig() throws CloudPoolException {
		assertFalse(this.driver.isConfigured());
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/valid-openstackpool-config.json");

		this.driver.configure(config);

		assertTrue(this.driver.isConfigured());
		OpenStackPoolDriverConfig expectedConfig = new OpenStackPoolDriverConfig(
				"http://nova.host.com:5000/v2.0", "RegionOne", "tenant",
				"clouduser", "cloudpass", true);
		assertThat(this.driver.config(), is(expectedConfig));
		assertThat(this.driver.getPoolName(), is("my-scaling-group"));

		// verify that config is passed onto cloud client
		verify(this.mockClient).configure(expectedConfig);
	}

	@Test
	public void reconfigure() throws CloudPoolException {
		// configure
		BaseCloudPoolConfig config1 = loadCloudPoolConfig("config/valid-openstackpool-config.json");
		this.driver.configure(config1);
		assertThat(this.driver.config(), is(new OpenStackPoolDriverConfig(
				"http://nova.host.com:5000/v2.0", "RegionOne", "tenant",
				"clouduser", "cloudpass", true)));
		assertThat(this.driver.getPoolName(), is("my-scaling-group"));

		// re-configure
		BaseCloudPoolConfig config2 = loadCloudPoolConfig("config/valid-openstackpool-config2.json");
		this.driver.configure(config2);
		assertThat(this.driver.config(), is(new OpenStackPoolDriverConfig(
				"http://nova.host.com:5000/v2.0", "RegionTwo", "otherTenant",
				"clouduser", "cloudpass", false)));
		assertThat(this.driver.getPoolName(), is("my-other-scaling-group"));
	}

	@Test(expected = IllegalStateException.class)
	public void invokeStartMachineBeforeBeingConfigured()
			throws CloudPoolException {
		ScaleOutConfig scaleUpConfig = new ScaleOutConfig("size", "image",
				"keyPair", Arrays.asList("web"),
				Arrays.asList("apt-get update"));
		this.driver.startMachines(2, scaleUpConfig);
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
	public void configureWithConfigMissingKeystoneEndpoint() throws Exception {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/invalid-openstackpool-config-missing-keystoneendpoint.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithConfigMissingRegion() throws Exception {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/invalid-openstackpool-config-missing-region.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithConfigMissingTenant() throws Exception {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/invalid-openstackpool-config-missing-tenant.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithConfigMissingUserName() throws Exception {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/invalid-openstackpool-config-missing-username.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithConfigMissingPassword() throws Exception {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/invalid-openstackpool-config-missing-password.json");
		this.driver.configure(config);
	}

	private BaseCloudPoolConfig loadCloudPoolConfig(String resourcePath) {
		return JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath),
				BaseCloudPoolConfig.class);
	}
}
