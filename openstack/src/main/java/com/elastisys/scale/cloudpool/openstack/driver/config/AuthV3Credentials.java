package com.elastisys.scale.cloudpool.openstack.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Objects;

/**
 * Password-credentials for authenticating using version 3 of the <a
 * href="http://docs.openstack.org/developer/keystone/http-api.html#">Keystone
 * HTTP API</a>.
 *
 * @see AuthConfig
 */
public class AuthV3Credentials {

	/**
	 * The scope of the authentication (may either be domain-scoped or
	 * project-scoped).
	 */
	private final Scope scope;

	/**
	 * The identifier of the user to authenticate (note: the UUID, not the
	 * name).
	 */
	private final String userId;

	/** The password of the user to authenticate.. */
	private final String password;

	public AuthV3Credentials(Scope scope, String userId, String password) {
		this.scope = scope;
		this.userId = userId;
		this.password = password;
		validate();
	}

	/**
	 * @return the {@link #scope}
	 */
	public Scope getScope() {
		return this.scope;
	}

	/**
	 * @return the {@link #userId}
	 */
	public String getUserId() {
		return this.userId;
	}

	/**
	 * @return the {@link #password}
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Returns <code>true</code> if these credentials are domain-scoped.
	 *
	 * @return
	 */
	public boolean isDomainScoped() {
		return this.scope.isDomainScoped();
	}

	/**
	 * Returns <code>true</code> if these credentials are project-scoped.
	 * 
	 * @return
	 */
	public boolean isProjectScoped() {
		return this.scope.isProjectScoped();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AuthV3Credentials) {
			AuthV3Credentials that = (AuthV3Credentials) obj;
			return Objects.equal(this.scope, that.scope)
					&& Objects.equal(this.userId, that.userId)
					&& Objects.equal(this.password, that.password);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.scope, this.userId, this.password);
	}

	/**
	 * Validates the configuration. On illegal value combination(s), an
	 * {@link IllegalArgumentException} is thrown.
	 */
	public void validate() throws IllegalArgumentException {
		checkArgument(this.scope != null, "no auth scope given");
		checkArgument(this.userId != null, "no userId given");
		checkArgument(this.password != null, "no password given");
		this.scope.validate();
	}

}
