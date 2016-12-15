package com.elastisys.scale.cloudpool.openstack.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.cloudpool.commons.basepool.config.BaseCloudPoolConfig;
import com.elastisys.scale.cloudpool.openstack.driver.OpenStackPoolDriver;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.openstack.ApiAccessConfig;
import com.elastisys.scale.commons.openstack.AuthConfig;
import com.elastisys.scale.commons.openstack.OSClientFactory;

/**
 * Cloud API settings for an {@link OpenStackPoolDriver}.
 *
 * @see BaseCloudPoolConfig#getCloudApiSettings()
 */
public class CloudApiSettings {

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
     * Creates {@link CloudApiSettings} with default timeouts.
     *
     * @param auth
     *            Declares how to authenticate with the OpenStack identity
     *            service (Keystone).
     * @param region
     *            The particular OpenStack region (out of the ones available in
     *            Keystone's service catalog) to connect to. For example,
     *            {@code RegionOne}.
     */
    public CloudApiSettings(AuthConfig auth, String region) {
        this(auth, region, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_SOCKET_TIMEOUT);
    }

    /**
     * Creates {@link CloudApiSettings}.
     *
     * @param auth
     *            Declares how to authenticate with the OpenStack identity
     *            service (Keystone).
     * @param region
     *            The particular OpenStack region (out of the ones available in
     *            Keystone's service catalog) to connect to. For example,
     *            {@code RegionOne}.
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
    public CloudApiSettings(AuthConfig auth, String region, Integer connectionTimeout, Integer socketTimeout) {
        this.auth = auth;
        this.region = region;
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
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
     * The timeout in milliseconds until a connection is established.
     *
     * @return
     */
    public Integer getConnectionTimeout() {
        return Optional.ofNullable(this.connectionTimeout).orElse(DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * The socket timeout ({@code SO_TIMEOUT}) in milliseconds, which is the
     * timeout for waiting for data or, put differently, a maximum period
     * inactivity between two consecutive data packets.
     *
     * @return
     */
    public Integer getSocketTimeout() {
        return Optional.ofNullable(this.socketTimeout).orElse(DEFAULT_SOCKET_TIMEOUT);
    }

    /**
     * Returns an {@link ApiAccessConfig} that can be used with an
     * {@link OSClientFactory} and corresponds to this {@link CloudApiSettings}.
     *
     * @return
     */
    public ApiAccessConfig toApiAccessConfig() {
        return new ApiAccessConfig(this.auth, this.region);
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
        return Objects.hash(this.auth, this.region, getConnectionTimeout(), getSocketTimeout());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CloudApiSettings) {
            CloudApiSettings that = (CloudApiSettings) obj;
            return Objects.equals(this.auth, that.auth) //
                    && Objects.equals(this.region, that.region) //
                    && Objects.equals(getConnectionTimeout(), that.getConnectionTimeout()) //
                    && Objects.equals(getSocketTimeout(), that.getSocketTimeout());
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
