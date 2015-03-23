package com.elastisys.scale.cloudpool.openstack.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.cloudpool.openstack.driver.config.AuthV3Credentials;
import com.elastisys.scale.cloudpool.openstack.driver.config.Scope;

public class TestAuthV3Credentials {
	@Test
	public void creation() {
		// create domain scoped credentials
		AuthV3Credentials domainScopedCredentials = new AuthV3Credentials(
				new Scope("domain_id", null), "user_id", "pass");
		assertThat(domainScopedCredentials.getScope().getDomainId(),
				is("domain_id"));
		assertThat(domainScopedCredentials.getUserId(), is("user_id"));
		assertThat(domainScopedCredentials.getPassword(), is("pass"));
		assertThat(domainScopedCredentials.isDomainScoped(), is(true));
		assertThat(domainScopedCredentials.isProjectScoped(), is(false));
		domainScopedCredentials.validate();

		// create project scoped credentials
		AuthV3Credentials projectScopedCredentials = new AuthV3Credentials(
				new Scope(null, "project_id"), "user_id", "pass");
		assertThat(projectScopedCredentials.getScope().getProjectId(),
				is("project_id"));
		assertThat(projectScopedCredentials.getUserId(), is("user_id"));
		assertThat(projectScopedCredentials.getPassword(), is("pass"));
		assertThat(projectScopedCredentials.isDomainScoped(), is(false));
		assertThat(projectScopedCredentials.isProjectScoped(), is(true));
		projectScopedCredentials.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingScope() {
		new AuthV3Credentials(null, "user_id", "pass");
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingUserId() {
		new AuthV3Credentials(new Scope(null, "project_id"), null, "pass");
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingPassword() {
		new AuthV3Credentials(new Scope(null, "project_id"), "user_id", null);
	}

}
