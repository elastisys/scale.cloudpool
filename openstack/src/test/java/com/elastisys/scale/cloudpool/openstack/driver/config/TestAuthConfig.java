package com.elastisys.scale.cloudpool.openstack.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.cloudpool.openstack.driver.config.AuthConfig;
import com.elastisys.scale.cloudpool.openstack.driver.config.AuthV2Credentials;
import com.elastisys.scale.cloudpool.openstack.driver.config.AuthV3Credentials;
import com.elastisys.scale.cloudpool.openstack.driver.config.Scope;

public class TestAuthConfig {

	@Test
	public void creation() {
		// v2 credential creation.
		AuthConfig v2Auth = new AuthConfig("https://keystone.host:5000/v2.0",
				new AuthV2Credentials("tenant", "user", "pass"), null);
		v2Auth.validate();
		assertThat(v2Auth.isV2Auth(), is(true));
		assertThat(v2Auth.isV3Auth(), is(false));
		assertThat(v2Auth.getKeystoneUrl(),
				is("https://keystone.host:5000/v2.0"));
		assertThat(v2Auth.getV2Credentials(), is(new AuthV2Credentials(
				"tenant", "user", "pass")));
		assertThat(v2Auth.getV3Credentials(), is(nullValue()));

		// v3 credential creation.
		AuthConfig v3Auth = new AuthConfig("https://keystone.host:5000/v3/",
				null, new AuthV3Credentials(new Scope(null, "project_id"),
						"user_id", "pass"));
		v3Auth.validate();
		assertThat(v3Auth.isV2Auth(), is(false));
		assertThat(v3Auth.isV3Auth(), is(true));
		assertThat(v3Auth.getKeystoneUrl(),
				is("https://keystone.host:5000/v3/"));
		assertThat(v3Auth.getV2Credentials(), is(nullValue()));
		assertThat(v3Auth.getV3Credentials(), is(new AuthV3Credentials(
				new Scope(null, "project_id"), "user_id", "pass")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingKeystoneUrl() {
		new AuthConfig(null, null, new AuthV3Credentials(new Scope(null,
				"project_id"), "user_id", "pass"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingBothV2AndV3Credentials() {
		new AuthConfig("https://keystone.host:5000/v2.0", null, null);
	}

	/**
	 * Must specify either v2 or v3 credentials, not both.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void specifyingBothV2AndV3Credentials() {
		new AuthConfig("https://keystone.host:5000/v3/", new AuthV2Credentials(
				"tenant", "user", "pass"), new AuthV3Credentials(new Scope(
				null, "project_id"), "user_id", "pass"));
	}
}
