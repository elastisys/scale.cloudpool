package com.elastisys.scale.cloudpool.openstack.driver.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.openstack.driver.client.OSClientFactory;
import com.elastisys.scale.cloudpool.openstack.driver.client.OSClientFactory.OSClientCreator;
import com.elastisys.scale.cloudpool.openstack.driver.config.AuthConfig;
import com.elastisys.scale.cloudpool.openstack.driver.config.OpenStackPoolDriverConfig;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Tests that verify the behavior of the {@link OSClientFactory} class.
 */
public class TestOSClientFactory {

	private OSClientCreator creatorMock;
	/** Object under test. */
	private OSClientFactory osClientFactory;

	@Before
	public void beforeTestMethod() {
		this.creatorMock = mock(OSClientFactory.OSClientCreator.class);
		this.osClientFactory = new OSClientFactory(this.creatorMock);
	}

	/**
	 * Verify that a driver config that specifies version 2 authentication is
	 * created properly by the {@link OSClientFactory}.
	 */
	@Test
	public void createClientFromV2Auth() {
		this.osClientFactory
				.createAuthenticatedClient(loadAuthConfig("config/openstack-pool-config-authv2.json"));
		verify(this.creatorMock).fromV2Auth("http://nova.host.com:5000/v2.0",
				"tenant", "clouduser", "cloudpass");
		verifyNoMoreInteractions(this.creatorMock);
	}

	/**
	 * Verify that a driver config that specifies domain-scoped version 3
	 * authentication is created properly by the {@link OSClientFactory}.
	 */
	@Test
	public void createClientFromDomainScopedV3Auth() {
		this.osClientFactory
				.createAuthenticatedClient(loadAuthConfig("config/openstack-pool-config-authv3-domain-scoped.json"));
		verify(this.creatorMock).fromDomainScopedV3Auth(
				"http://nova.host.com:5000/v3", "domain_id", "user_id",
				"secret");
		verifyNoMoreInteractions(this.creatorMock);
	}

	/**
	 * Verify that a driver config that specifies project-scoped version 3
	 * authentication is created properly by the {@link OSClientFactory}.
	 */
	@Test
	public void createClientFromProjectScopedV3Auth() {
		this.osClientFactory
				.createAuthenticatedClient(loadAuthConfig("config/openstack-pool-config-authv3-project-scoped.json"));
		verify(this.creatorMock).fromProjectScopedV3Auth(
				"http://nova.host.com:5000/v3", "project_id", "user_id",
				"secret");
		verifyNoMoreInteractions(this.creatorMock);
	}

	/**
	 * Loads the {@link AuthConfig} part of a {@link BaseCloudPoolConfig}.
	 *
	 * @param cloudPoolConfigPath
	 * @return
	 */
	private AuthConfig loadAuthConfig(String cloudPoolConfigPath) {
		BaseCloudPoolConfig cloudPoolConfig = JsonUtils.toObject(
				JsonUtils.parseJsonResource(cloudPoolConfigPath),
				BaseCloudPoolConfig.class);
		OpenStackPoolDriverConfig driverConfig = JsonUtils.toObject(
				cloudPoolConfig.getCloudPool().getDriverConfig(),
				OpenStackPoolDriverConfig.class);
		return driverConfig.getAuth();
	}
}
