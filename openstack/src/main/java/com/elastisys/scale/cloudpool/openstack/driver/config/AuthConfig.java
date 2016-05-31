package com.elastisys.scale.cloudpool.openstack.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import com.elastisys.scale.cloudpool.openstack.driver.OpenStackPoolDriver;
import com.google.common.base.Objects;

/**
 * A configuration that declares how to authenticate the user to OpenStack's
 * identity service (Keystone).
 * <p/>
 * The {@link OpenStackPoolDriver} can be configured to use either use version 2
 * or version 3 of the
 * <a href="http://docs.openstack.org/developer/keystone/http-api.html#history"
 * >identity HTTP API</a>.
 *
 * @see OpenStackPoolDriverConfig
 */
public class AuthConfig {

	/**
	 * Endpoint URL of the OpenStack authentication service (Keystone). For
	 * example, {@code http://172.16.0.1:5000/v2.0/}.
	 */
	private final String keystoneUrl;

	/** Version 2 type authentication credentials. */
	private final AuthV2Credentials v2Credentials;
	/** Version 3 type authentication credentials. */
	private final AuthV3Credentials v3Credentials;

	/**
	 * Creates a new {@link AuthConfig}. Either {@link AuthV2Credentials} or
	 * {@link AuthV3Credentials} need to be specified, but not both.
	 *
	 * @param keystoneUrl
	 *                Endpoint URL of the OpenStack authentication service
	 *                (Keystone).
	 * @param v2Credentials
	 *                Version 2 type authentication credentials. May be
	 *                <code>null</code> if version 3 credentials are given.
	 * @param v3Credentials
	 *                Version 3 type authentication credentials. May be
	 *                <code>null</code> if version 2 credentials are given.
	 */
	public AuthConfig(String keystoneUrl, AuthV2Credentials v2Credentials,
			AuthV3Credentials v3Credentials) {
		this.keystoneUrl = keystoneUrl;
		this.v2Credentials = v2Credentials;
		this.v3Credentials = v3Credentials;
		validate();
	}

	/**
	 * @return the {@link #keystoneUrl}
	 */
	public String getKeystoneUrl() {
		return this.keystoneUrl;
	}

	/**
	 * @return the {@link #v2Credentials}
	 */
	public AuthV2Credentials getV2Credentials() {
		return this.v2Credentials;
	}

	/**
	 * @return the {@link #v3Credentials}
	 */
	public AuthV3Credentials getV3Credentials() {
		return this.v3Credentials;
	}

	/**
	 * Returns <code>true</code> if version 2 type authentication
	 * credentials are used.
	 *
	 * @return
	 */
	public boolean isV2Auth() {
		return this.v2Credentials != null;
	}

	/**
	 * Returns <code>true</code> if version 3 type authentication
	 * credentials are used.
	 *
	 * @return
	 */
	public boolean isV3Auth() {
		return this.v3Credentials != null;
	}

	/**
	 * Validates the configuration. On illegal value combination(s), an
	 * {@link IllegalArgumentException} is thrown.
	 */
	public void validate() throws IllegalArgumentException {
		checkArgument(this.keystoneUrl != null, "no keystoneUrl given");
		checkArgument(this.v2Credentials != null ^ this.v3Credentials != null,
				"*either* v2 or v3 auth credentials must be given");
		if (this.v2Credentials != null) {
			this.v2Credentials.validate();
		}
		if (this.v3Credentials != null) {
			this.v3Credentials.validate();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AuthConfig) {
			AuthConfig that = (AuthConfig) obj;
			return Objects.equal(this.keystoneUrl, that.keystoneUrl)
					&& Objects.equal(this.v2Credentials, that.v2Credentials)
					&& Objects.equal(this.v3Credentials, that.v3Credentials);
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(keystoneUrl, v2Credentials, v3Credentials);
	}
}
