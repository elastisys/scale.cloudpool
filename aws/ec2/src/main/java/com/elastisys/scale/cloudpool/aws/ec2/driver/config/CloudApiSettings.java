package com.elastisys.scale.cloudpool.aws.ec2.driver.config;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;

import com.elastisys.scale.cloudpool.aws.ec2.driver.Ec2PoolDriver;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * Configuration object for an {@link Ec2PoolDriver}.
 */
public class CloudApiSettings {
    /**
     * The default timeout in milliseconds until a connection is established.
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 60000;
    /**
     * The default socket timeout ({@code SO_TIMEOUT}) in milliseconds, which is
     * the timeout for waiting for data or, put differently, a maximum period
     * inactivity between two consecutive data packets).
     */
    public static final int DEFAULT_SOCKET_TIMEOUT = 60000;

    /** The access key id of the AWS account. */
    private final String awsAccessKeyId;
    /** The secret access key of the AWS account. */
    private final String awsSecretAccessKey;
    /**
     * The particular AWS region to connect to. For example, {@code us-east-1}.
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
     * Creates a new {@link CloudApiSettings}.
     *
     * @param awsAccessKeyId
     *            The access key id of the AWS account.
     * @param awsSecretAccessKey
     *            The secret access key of the AWS account.
     * @param region
     *            The particular AWS region to connect to. For example,
     *            {@code us-east-1}.
     */
    public CloudApiSettings(String awsAccessKeyId, String awsSecretAccessKey, String region) {
        this(awsAccessKeyId, awsSecretAccessKey, region, null, null);
    }

    /**
     * Creates a new {@link CloudApiSettings}.
     *
     * @param awsAccessKeyId
     *            The access key id of the AWS account.
     * @param awsSecretAccessKey
     *            The secret access key of the AWS account.
     * @param region
     *            The particular AWS region to connect to. For example,
     *            {@code us-east-1}.
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
    public CloudApiSettings(String awsAccessKeyId, String awsSecretAccessKey, String region, Integer connectionTimeout,
            Integer socketTimeout) {
        this.awsAccessKeyId = awsAccessKeyId;
        this.awsSecretAccessKey = awsSecretAccessKey;
        this.region = region;
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
    }

    /**
     * Returns the access key id of the AWS account.
     *
     * @return
     */
    public String getAwsAccessKeyId() {
        return this.awsAccessKeyId;
    }

    /**
     * Returns the secret access key of the AWS account.
     *
     * @return
     */
    public String getAwsSecretAccessKey() {
        return this.awsSecretAccessKey;
    }

    /**
     * Returns the particular AWS region to connect to.
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
     * Performs basic validation of this configuration.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.awsAccessKeyId != null, "missing awsAccessKeyId");
        checkArgument(this.awsSecretAccessKey != null, "missing awsSecretAccessKey");
        checkArgument(this.region != null, "missing region");
        checkArgument(getConnectionTimeout() > 0, "connectionTimeout must be positive");
        checkArgument(getSocketTimeout() > 0, "socketTimeout must be positive");
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.awsAccessKeyId, this.awsSecretAccessKey, this.region, getConnectionTimeout(),
                getSocketTimeout());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CloudApiSettings) {
            CloudApiSettings that = (CloudApiSettings) obj;
            return equal(this.awsAccessKeyId, that.awsAccessKeyId)
                    && equal(this.awsSecretAccessKey, that.awsSecretAccessKey) && equal(this.region, that.region)
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
