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
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleOutConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.openstack.driver.OpenStackPoolDriver;
import com.elastisys.scale.cloudpool.openstack.driver.client.OpenstackClient;
import com.elastisys.scale.cloudpool.openstack.driver.config.AuthConfig;
import com.elastisys.scale.cloudpool.openstack.driver.config.AuthV2Credentials;
import com.elastisys.scale.cloudpool.openstack.driver.config.AuthV3Credentials;
import com.elastisys.scale.cloudpool.openstack.driver.config.OpenStackPoolDriverConfig;
import com.elastisys.scale.cloudpool.openstack.driver.config.Scope;
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

	/**
	 * {@link OpenStackPoolDriver} configured with version 2 authentication.
	 */
	@Test
	public void configureWithV2Auth() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/openstack-pool-config-authv2.json");
		this.driver.configure(config);

		OpenStackPoolDriverConfig expectedDriverConfig = new OpenStackPoolDriverConfig(
				new AuthConfig("http://nova.host.com:5000/v2.0",
						new AuthV2Credentials("tenant", "clouduser",
								"cloudpass"), null), "RegionTwo", false);
		assertTrue(this.driver.isConfigured());
		assertThat(this.driver.getPoolName(), is("my-scaling-pool"));
		assertThat(this.driver.config(), is(expectedDriverConfig));

		// verify that config is passed onto cloud client
		verify(this.mockClient).configure(expectedDriverConfig);
	}

	/**
	 * {@link OpenStackPoolDriver} configured with version 3 domain-scoped
	 * authentication.
	 */
	@Test
	public void configureWithDomainScopedV3Auth() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/openstack-pool-config-authv3-domain-scoped.json");
		this.driver.configure(config);

		OpenStackPoolDriverConfig expectedDriverConfig = new OpenStackPoolDriverConfig(
				new AuthConfig("http://nova.host.com:5000/v3", null,
						new AuthV3Credentials(new Scope("domain_id", null),
								"user_id", "secret")), "RegionTwo", false);
		assertTrue(this.driver.isConfigured());
		assertThat(this.driver.getPoolName(), is("my-scaling-pool2"));
		assertThat(this.driver.config(), is(expectedDriverConfig));

		// verify that config is passed onto cloud client
		verify(this.mockClient).configure(expectedDriverConfig);
	}

	/**
	 * {@link OpenStackPoolDriver} configured with version 3 project-scoped
	 * authentication.
	 */
	@Test
	public void configureWithProjectScopedV3Auth() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/openstack-pool-config-authv3-project-scoped.json");
		this.driver.configure(config);

		OpenStackPoolDriverConfig expectedDriverConfig = new OpenStackPoolDriverConfig(
				new AuthConfig("http://nova.host.com:5000/v3", null,
						new AuthV3Credentials(new Scope(null, "project_id"),
								"user_id", "secret")), "RegionTwo", false);
		assertTrue(this.driver.isConfigured());
		assertThat(this.driver.getPoolName(), is("my-scaling-pool3"));
		assertThat(this.driver.config(), is(expectedDriverConfig));

		// verify that config is passed onto cloud client
		verify(this.mockClient).configure(expectedDriverConfig);
	}

	@Test
	public void reconfigure() throws CloudPoolException {
		assertFalse(this.driver.isConfigured());

		// configure
		BaseCloudPoolConfig config1 = loadCloudPoolConfig("config/openstack-pool-config-authv3-project-scoped.json");
		this.driver.configure(config1);
		OpenStackPoolDriverConfig expectedDriverConfig = JsonUtils.toObject(
				config1.getCloudPool().getDriverConfig(),
				OpenStackPoolDriverConfig.class);
		assertThat(this.driver.getPoolName(), is("my-scaling-pool3"));
		assertThat(this.driver.config(), is(expectedDriverConfig));

		// re-configure
		BaseCloudPoolConfig config2 = loadCloudPoolConfig("config/openstack-pool-config-authv2.json");
		this.driver.configure(config2);
		expectedDriverConfig = JsonUtils.toObject(config2.getCloudPool()
				.getDriverConfig(), OpenStackPoolDriverConfig.class);
		assertThat(this.driver.getPoolName(), is("my-scaling-pool"));
		assertThat(this.driver.config(), is(expectedDriverConfig));
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

	@Test(expected = IllegalStateException.class)
	public void invokeAttachMachineBeforeBeingConfigured() throws Exception {
		this.driver.attachMachine("i-1");
	}

	@Test(expected = IllegalStateException.class)
	public void invokeDetachMachineBeforeBeingConfigured() throws Exception {
		this.driver.detachMachine("i-1");
	}

	@Test(expected = IllegalStateException.class)
	public void invokeSetServiceStateBeforeBeingConfigured() throws Exception {
		this.driver.setServiceState("i-1", ServiceState.IN_SERVICE);
	}

	@Test(expected = IllegalStateException.class)
	public void invokeSetMembershipStatusStateBeforeBeingConfigured()
			throws Exception {
		this.driver
				.setMembershipStatus("i-1", MembershipStatus.defaultStatus());
	}

	@Test(expected = IllegalStateException.class)
	public void invokeGetPoolNameBeforeBeingConfigured() throws Exception {
		this.driver.getPoolName();
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureMissingAuth() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/config-missing-auth.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithAuthV2Credentials() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/authv2-missing-v2credentials.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithAuthV2MissingKeystoneUrl() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/authv2-missing-keystone.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithAuthV2MissingUser() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/authv2-missing-user.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithAuthV2MissingTenant() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/authv2-missing-tenant.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithAuthV2MissingPassword() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/authv2-missing-password.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithAuthV3MissingKeystoneUrl() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/authv3-missing-keystone.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithAuthV3MissingPassword() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/authv3-missing-password.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithAuthV3MissingScopeSpecifier() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/authv3-missing-scope-specifier.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithAuthV3MissingScope() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/authv3-missing-scope.json");
		this.driver.configure(config);
	}

	@Test(expected = CloudPoolDriverException.class)
	public void configureWithAuthV3MissingUser() {
		BaseCloudPoolConfig config = loadCloudPoolConfig("config/authv3-missing-user.json");
		this.driver.configure(config);
	}

	private BaseCloudPoolConfig loadCloudPoolConfig(String resourcePath) {
		return JsonUtils.toObject(JsonUtils.parseJsonResource(resourcePath),
				BaseCloudPoolConfig.class);
	}
}
