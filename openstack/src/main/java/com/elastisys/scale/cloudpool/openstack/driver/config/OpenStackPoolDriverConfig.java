package com.elastisys.scale.cloudpool.openstack.driver.config;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import com.elastisys.scale.cloudpool.openstack.driver.OpenStackPoolDriver;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * Configuration object for an {@link OpenStackPoolDriver}, which declares how
 * to authenticate and what OpenStack region to operate against.
 * <p/>
 * The {@link OpenStackPoolDriver} can be configured to use either use version 2
 * or version 3 of the
 * <a href="http://docs.openstack.org/developer/keystone/http-api.html#history"
 * >identity HTTP API</a>.
 */
public class OpenStackPoolDriverConfig {

	/**
	 * The default timeout in milliseconds until a connection is established.
	 */
	public static final int DEFAULT_CONNECTION_TIMEOUT = 10000;
	/**
	 * The default socket timeout ({@code SO_TIMEOUT}) in milliseconds, which is
	 * the timeout for waiting for data or, put differently, a maximum period
	 * inactivity between two consecutive data packets).
	 */
	public static final int DEFAULT_SOCKET_TIMEOUT = 10000;

	/**
	 * Declares how to authenticate with the OpenStack identity service
	 * (Keystone).
	 */
	private final AuthConfig auth;

	/**
	 * The particular OpenStack region (out of the ones available in Keystone's
	 * service catalog) to connect to. For example, {@code RegionOne}.
	 */
	private final String region;
	/**
	 * The names of the networks to attach launched servers to (for example,
	 * {@code private}). Each network creates a separate network interface
	 * controller (NIC) on a created server. Typically, this option can be left
	 * out, but in rare cases, when an account has more than one network to
	 * choose from, the OpenStack API forces us to be explicit about the
	 * network(s) we want to use.
	 * <p/>
	 * If set to <code>null</code> or left empty, the default behavior is to use
	 * which ever network is configured by the cloud provider for the
	 * user/project. However, if there are multiple choices, this will cause
	 * server boot requests to fail.
	 */
	private final List<String> networks;
	/**
	 * Set to <code>true</code> if a floating IP address should be allocated to
	 * launched servers. Default: <code>true</code>.
	 */
	private final Boolean assignFloatingIp;

	/**
	 * The timeout in milliseconds until a connection is established.
	 */
	private final Integer connectionTimeout;

	/**
	 * The socket timeout ({@code SO_TIMEOUT}) in milliseconds, which is the
	 * timeout for waiting for data or, put differently, a maximum period
	 * inactivity between two consecutive data packets.
	 */
	private final Integer socketTimeout;

	/**
	 * Creates a new {@link OpenStackPoolDriverConfig}.
	 *
	 * @param auth
	 *            Declares how to authenticate with the OpenStack identity
	 *            service (Keystone).
	 * @param region
	 *            The particular OpenStack region (out of the ones available in
	 *            Keystone's service catalog) to connect to. For example,
	 *            {@code RegionOne}.
	 * @param networks
	 *            The names of the networks to attach launched servers to (for
	 *            example, {@code private}). Each network creates a separate
	 *            network interface controller (NIC) on a created server.
	 *            Typically, this option can be left out, but in rare cases,
	 *            when an account has more than one network to choose from, the
	 *            OpenStack API forces us to be explicit about the network(s) we
	 *            want to use.
	 *            <p/>
	 *            If set to <code>null</code> or left empty, the default
	 *            behavior is to use which ever network is configured by the
	 *            cloud provider for the user/project. However, if there are
	 *            multiple choices, this will cause server boot requests to
	 *            fail.
	 * @param assignFloatingIp
	 *            Set to <code>true</code> if a floating IP address should be
	 *            allocated to launched servers. Default: <code>true</code>.
	 */
	public OpenStackPoolDriverConfig(AuthConfig auth, String region, List<String> networks, Boolean assignFloatingIp) {
		this(auth, region, networks, assignFloatingIp, null, null);
	}

	/**
	 * Creates a new {@link OpenStackPoolDriverConfig}.
	 *
	 * @param auth
	 *            Declares how to authenticate with the OpenStack identity
	 *            service (Keystone).
	 * @param region
	 *            The particular OpenStack region (out of the ones available in
	 *            Keystone's service catalog) to connect to. For example,
	 *            {@code RegionOne}.
	 * @param networks
	 *            The names of the networks to attach launched servers to (for
	 *            example, {@code private}). Each network creates a separate
	 *            network interface controller (NIC) on a created server.
	 *            Typically, this option can be left out, but in rare cases,
	 *            when an account has more than one network to choose from, the
	 *            OpenStack API forces us to be explicit about the network(s) we
	 *            want to use.
	 *            <p/>
	 *            If set to <code>null</code> or left empty, the default
	 *            behavior is to use which ever network is configured by the
	 *            cloud provider for the user/project. However, if there are
	 *            multiple choices, this will cause server boot requests to
	 *            fail.
	 * @param assignFloatingIp
	 *            Set to <code>true</code> if a floating IP address should be
	 *            allocated to launched servers. Default: <code>true</code>.
	 * @param connectionTimeout
	 *            The timeout in milliseconds until a connection is established.
	 *            May be <code>null</code>. Default:
	 *            {@value #DEFAULT_CONNECTION_TIMEOUT} ms.
	 * @param socketTimeout
	 *            The socket timeout ({@code SO_TIMEOUT}) in milliseconds, which
	 *            is the timeout for waiting for data or, put differently, a
	 *            maximum period inactivity between two consecutive data
	 *            packets. May be <code>null</code>. Default:
	 *            {@value #DEFAULT_SOCKET_TIMEOUT} ms.
	 */
	public OpenStackPoolDriverConfig(AuthConfig auth, String region, List<String> networks, Boolean assignFloatingIp,
			Integer connectionTimeout, Integer socketTimeout) {
		this.auth = auth;
		this.region = region;
		this.networks = networks;
		this.assignFloatingIp = assignFloatingIp;
		this.connectionTimeout = connectionTimeout;
		this.socketTimeout = socketTimeout;
		validate();
	}

	/**
	 * Returns a description of how to authenticate with the OpenStack identity
	 * service (Keystone).
	 *
	 * @return
	 */
	public AuthConfig getAuth() {
		return this.auth;
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
	 * The names of the networks to attach launched servers to (for example,
	 * {@code private}). Each network creates a separate network interface
	 * controller (NIC) on a created server. Typically, this option can be left
	 * out, but in rare cases, when an account has more than one network to
	 * choose from, the OpenStack API forces us to be explicit about the
	 * network(s) we want to use.
	 * <p/>
	 * If set to <code>null</code> or left empty, the default behavior is to use
	 * which ever network is configured by the cloud provider for the
	 * user/project. However, if there are multiple choices, this will cause
	 * server boot requests to fail.
	 *
	 * @return
	 */
	public List<String> getNetworks() {
		return this.networks;
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
	 * The timeout in milliseconds until a connection is established.
	 *
	 * @return
	 */
	public Integer getConnectionTimeout() {
		return Optional.fromNullable(this.connectionTimeout).or(DEFAULT_CONNECTION_TIMEOUT);
	}

	/**
	 * The socket timeout ({@code SO_TIMEOUT}) in milliseconds, which is the
	 * timeout for waiting for data or, put differently, a maximum period
	 * inactivity between two consecutive data packets.
	 *
	 * @return
	 */
	public Integer getSocketTimeout() {
		return Optional.fromNullable(this.socketTimeout).or(DEFAULT_SOCKET_TIMEOUT);
	}

	/**
	 * Performs basic validation of this configuration. Throws an
	 * {@link IllegalArgumentException} on failure to validate the
	 * configuration.
	 *
	 * @throws IllegalArgumentException
	 */
	public void validate() throws IllegalArgumentException {
		checkArgument(this.auth != null, "no auth method specified");
		checkArgument(this.region != null, "missing region");
		this.auth.validate();
		checkArgument(getConnectionTimeout() > 0, "connectionTimeout must be positive");
		checkArgument(getSocketTimeout() > 0, "socketTimeout must be positive");
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.auth, this.region, this.networks, isAssignFloatingIp(), getConnectionTimeout(),
				getSocketTimeout());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof OpenStackPoolDriverConfig) {
			OpenStackPoolDriverConfig that = (OpenStackPoolDriverConfig) obj;
			return equal(this.auth, that.auth) && equal(this.region, that.region) && equal(this.networks, that.networks)
					&& equal(isAssignFloatingIp(), that.isAssignFloatingIp())
					&& equal(getConnectionTimeout(), that.getConnectionTimeout())
					&& equal(getSocketTimeout(), that.getSocketTimeout());
		}
		return false;
	}

	@Override
	public String toString() {
		return JsonUtils.toString(JsonUtils.toJson(this));
	}
}
