package com.elastisys.scale.cloudpool.openstack.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.scale.cloudpool.openstack.driver.config.Scope;

public class TestScope {

	/**
	 * Create a domain-scoped authentication {@link Scope}.
	 */
	@Test
	public void domainScopedAuth() {
		Scope scope = new Scope("domain_id", null);
		assertThat(scope.isDomainScoped(), is(true));
		assertThat(scope.isProjectScoped(), is(false));
		assertThat(scope.getDomainId(), is("domain_id"));
		assertThat(scope.getProjectId(), is(nullValue()));
	}

	/**
	 * Create a project-scoped authentication {@link Scope}.
	 */
	@Test
	public void projectScopedAuth() {
		Scope scope = new Scope(null, "project_id");
		assertThat(scope.isDomainScoped(), is(false));
		assertThat(scope.isProjectScoped(), is(true));
		assertThat(scope.getDomainId(), is(nullValue()));
		assertThat(scope.getProjectId(), is("project_id"));
	}

	/**
	 * Must specify either scope-type.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void noScopeSpecified() {
		new Scope(null, null);
	}

	/**
	 * Must specify either scope-type (not both).
	 */
	@Test(expected = IllegalArgumentException.class)
	public void bothScopesSpecified() {
		new Scope("domain_id", "project_id");
	}

}
