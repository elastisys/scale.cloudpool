package com.elastisys.scale.cloudpool.openstack.driver;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * Configuration object for an {@link OpenStackPoolDriver}.
 *
 * 
 */
public class OpenStackPoolDriverConfig {

	/**
	 * Endpoint URL of the OpenStack authentication service (Keystone). For
	 * example, {@code http://172.16.0.1:5000/v2.0/}.
	 */
	private final String keystoneEndpoint;
	/**
	 * The particular OpenStack region (out of the ones available in Keystone's
	 * service catalog) to connect to. For example, {@code RegionOne}.
	 */
	private final String region;
	/** OpenStack account tenant name. */
	private final String tenantName;
	/** OpenStack account user. */
	private final String userName;
	/** OpenStack account password. */
	private final String password;

	/**
	 * Set to <code>true</code> if a floating IP address should be allocated to
	 * launched servers. Default: <code>true</code>.
	 */
	private final Boolean assignFloatingIp;

	/**
	 * Creates a new {@link OpenStackPoolDriverConfig}.
	 *
	 * @param keystoneEndpoint
	 *            Endpoint URL of the OpenStack authentication service
	 *            (Keystone). For example, {@code http://172.16.0.1:5000/v2.0/}.
	 * @param region
	 *            The particular OpenStack region (out of the ones available in
	 *            Keystone's service catalog) to connect to. For example,
	 *            {@code RegionOne}.
	 * @param tenantName
	 *            OpenStack account tenant name.
	 * @param userName
	 *            OpenStack account user.
	 * @param password
	 *            OpenStack account password.
	 */
	public OpenStackPoolDriverConfig(String keystoneEndpoint, String region,
			String tenantName, String userName, String password) {
		this(keystoneEndpoint, region, tenantName, userName, password, true);
	}

	/**
	 * Creates a new {@link OpenStackPoolDriverConfig}.
	 *
	 * @param keystoneEndpoint
	 *            Endpoint URL of the OpenStack authentication service
	 *            (Keystone). For example, {@code http://172.16.0.1:5000/v2.0/}.
	 * @param region
	 *            The particular OpenStack region (out of the ones available in
	 *            Keystone's service catalog) to connect to. For example,
	 *            {@code RegionOne}.
	 * @param tenantName
	 *            OpenStack account tenant name.
	 * @param userName
	 *            OpenStack account user.
	 * @param password
	 *            OpenStack account password.
	 * @param assignFloatingIp
	 *            Set to <code>true</code> if a floating IP address should be
	 *            allocated to launched servers. If <code>null</code> or
	 *            <code>false</code> no floating IP address will be allocated to
	 *            new servers.
	 */
	public OpenStackPoolDriverConfig(String keystoneEndpoint, String region,
			String tenantName, String userName, String password,
			Boolean assignFloatingIp) {
		this.keystoneEndpoint = keystoneEndpoint;
		this.region = region;
		this.userName = userName;
		this.password = password;
		this.tenantName = tenantName;
		this.assignFloatingIp = assignFloatingIp;
	}

	/**
	 * Returns the endpoint URL of the OpenStack authentication service
	 * (Keystone). For example, {@code http://172.16.0.1:5000/v2.0/}.
	 *
	 * @return
	 */
	public String getKeystoneEndpoint() {
		return this.keystoneEndpoint;
	}

	/**
	 * Returns the particular OpenStack region (out of the ones available in
	 * Keystone's service catalog) to connect to. For example, {@code RegionOne}
	 * .
	 *
	 * @return
	 */
	public String getRegion() {
		return this.region;
	}

	/**
	 * Returns the OpenStack account tenant name.
	 *
	 * @return
	 */
	public String getTenantName() {
		return this.tenantName;
	}

	/**
	 * Returns the OpenStack account user name.
	 *
	 * @return
	 */
	public String getUserName() {
		return this.userName;
	}

	/**
	 * Returns the OpenStack account password.
	 *
	 * @return
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Returns <code>true</code> if a floating IP address should be allocated to
	 * launched servers.
	 *
	 * @return
	 */
	public Boolean isAssignFloatingIp() {
		return Optional.fromNullable(this.assignFloatingIp).or(true);
	}

	/**
	 * Performs basic validation of this configuration.
	 *
	 * @throws CloudPoolDriverException
	 */
	public void validate() throws CloudPoolDriverException {
		try {
			checkNotNull(this.keystoneEndpoint, "missing keystoneEndpoint");
			checkNotNull(this.region, "missing region");
			checkNotNull(this.tenantName, "missing tenantName");
			checkNotNull(this.userName, "missing userName");
			checkNotNull(this.password, "missing password");
		} catch (Exception e) {
			// no need to wrap further if already a config exception
			throw new CloudPoolDriverException(format(
					"failed to validate cloud client configuration: %s",
					e.getMessage()), e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.keystoneEndpoint, this.region,
				this.tenantName, this.userName, this.password,
				this.isAssignFloatingIp());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof OpenStackPoolDriverConfig) {
			OpenStackPoolDriverConfig that = (OpenStackPoolDriverConfig) obj;
			return equal(this.keystoneEndpoint, that.keystoneEndpoint)
					&& equal(this.region, that.region)
					&& equal(this.tenantName, that.tenantName)
					&& equal(this.userName, that.userName)
					&& equal(this.password, that.password)
					&& equal(this.isAssignFloatingIp(),
							that.isAssignFloatingIp());
		}
		return false;
	}

	@Override
	public String toString() {
		return JsonUtils.toString(JsonUtils.toJson(this));
	}
}
