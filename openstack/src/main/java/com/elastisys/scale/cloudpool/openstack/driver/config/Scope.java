package com.elastisys.scale.cloudpool.openstack.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Objects;

/**
 * Authentication scope to use for authentication against version 3 of the <a
 * href="http://docs.openstack.org/developer/keystone/http-api.html#">Keystone
 * HTTP API</a>.
 * <p/>
 * Version 3-type authentication can be done using either project scope or
 * domain scope.
 *
 * @see AuthV3Credentials
 */
public class Scope {

	/**
	 * The domain id in case domain-scoped authentication is used,
	 * <code>null</code> otherwise.
	 */
	private final String domainId;

	/**
	 * The project id in case project-scoped authentication is used,
	 * <code>null</code> otherwise.
	 */
	private final String projectId;

	/**
	 * Constructs an authentication {@link Scope} for {@link AuthV3Credentials}.
	 *
	 * @param domainId
	 *            The domain id in case domain-scoped authentication is used,
	 *            <code>null</code> otherwise.
	 * @param projectId
	 *            The project id in case project-scoped authentication is used,
	 *            <code>null</code> otherwise.
	 */
	public Scope(String domainId, String projectId) {
		this.domainId = domainId;
		this.projectId = projectId;
		validate();
	}

	/**
	 * @return the {@link #domainId}
	 */
	public String getDomainId() {
		return this.domainId;
	}

	/**
	 * @return the {@link #projectId}
	 */
	public String getProjectId() {
		return this.projectId;
	}

	/**
	 * Returns <code>true</code> if domain-scoped authentication is specified.
	 *
	 * @return
	 */
	public boolean isDomainScoped() {
		return this.domainId != null;
	}

	/**
	 * Returns <code>true</code> if project-scoped authentication is specified.
	 *
	 * @return
	 */
	public boolean isProjectScoped() {
		return this.projectId != null;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.domainId, this.projectId);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Scope) {
			Scope that = (Scope) obj;
			return Objects.equal(this.domainId, that.domainId)
					&& Objects.equal(this.projectId, that.projectId);
		}
		return false;
	}

	/**
	 * Validates the configuration. On illegal value combination(s), an
	 * {@link IllegalArgumentException} is thrown.
	 */
	public void validate() throws IllegalArgumentException {
		checkArgument(this.domainId != null ^ this.projectId != null,
				"either domain-scoped or project-scoped authentication must be specified");
	}

}
