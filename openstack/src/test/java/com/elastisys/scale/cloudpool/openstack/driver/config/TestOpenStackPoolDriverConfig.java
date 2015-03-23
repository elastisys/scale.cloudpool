package com.elastisys.scale.cloudpool.openstack.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.cloudpool.openstack.driver.config.AuthConfig;
import com.elastisys.scale.cloudpool.openstack.driver.config.AuthV3Credentials;
import com.elastisys.scale.cloudpool.openstack.driver.config.OpenStackPoolDriverConfig;
import com.elastisys.scale.cloudpool.openstack.driver.config.Scope;

public class TestOpenStackPoolDriverConfig {

	@Test
	public void creation() {
		// explicit floating IP assignment
		AuthConfig auth = new AuthConfig("https://keystone.host:5000/v3/",
				null, new AuthV3Credentials(new Scope("domain_id", null),
						"user_id", "pass"));
		String region = "RegionOne";
		boolean assignFloatingIp = false;
		OpenStackPoolDriverConfig config = new OpenStackPoolDriverConfig(auth,
				region, assignFloatingIp);
		assertThat(config.getAuth(), is(auth));
		assertThat(config.getRegion(), is(region));
		assertThat(config.isAssignFloatingIp(), is(assignFloatingIp));

		// default floating IP assignment (true)
		config = new OpenStackPoolDriverConfig(auth, region, null);
		assertThat(config.getAuth(), is(auth));
		assertThat(config.getRegion(), is(region));
		assertThat(config.isAssignFloatingIp(), is(true));
	}

	/** Config must specify authentication details. */
	@Test(expected = IllegalArgumentException.class)
	public void missingAuth() {
		AuthConfig auth = null;
		String region = "RegionOne";
		new OpenStackPoolDriverConfig(auth, region, null);
	}

	/** Config must specify region to operate against. */
	@Test(expected = IllegalArgumentException.class)
	public void missingRegion() {
		AuthConfig auth = new AuthConfig("https://keystone.host:5000/v3/",
				null, new AuthV3Credentials(new Scope("domain_id", null),
						"user_id", "pass"));
		String region = null;
		new OpenStackPoolDriverConfig(auth, region, null);
	}
}
