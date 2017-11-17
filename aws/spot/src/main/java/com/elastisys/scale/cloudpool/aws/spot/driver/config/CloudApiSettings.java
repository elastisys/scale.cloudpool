package com.elastisys.scale.cloudpool.aws.spot.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.elastisys.scale.cloudpool.aws.spot.driver.SpotPoolDriver;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Configuration for a {@link SpotPoolDriver}.
 */
public class CloudApiSettings {
    /**
     * Number of decimals in Amazon bid prices. For example, a bid price of
     * $0.0123456789 gets rounded off to $0.012346 by Amazon (note the
     * rounding).
     */
    private static final int AMAZON_BIDPRICE_PRECISION = 6;

    /** Default value for {@link #bidReplacementPeriod} */
    public static final TimeInterval DEFAULT_BID_REPLACEMENT_PERIOD = new TimeInterval(2L, TimeUnit.MINUTES);

    /** Default value for {@link #danglingInstanceCleanupPeriod} */
    public static final TimeInterval DEFAULT_DANGLING_INSTANCE_CLEANUP_PERIOD = new TimeInterval(2L, TimeUnit.MINUTES);

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

    /** The access key id of the AWS account. */
    private final String awsAccessKeyId;
    /** The secret access key of the AWS account. */
    private final String awsSecretAccessKey;
    /** The particular AWS region to connect to. For example, 'us-east-1'. */
    private final String region;
    /**
     * The bid price (maximum price to pay for an instance hour in dollars) to
     * use when requesting spot instances.
     */
    private final double bidPrice;
    /**
     * The delay between two successive bid replacement runs (replacing spot
     * requests with an out-dated bid price).
     */
    private final TimeInterval bidReplacementPeriod;
    /**
     * The delay between two successive dangling instance cleanup runs (where
     * instances whose spot requests have been canceled are terminated).
     */
    private final TimeInterval danglingInstanceCleanupPeriod;
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
     * @param bidPrice
     *            The bid price (maximum price to pay for an instance hour in
     *            dollars) to use when requesting spot instances.
     * @param bidReplacementPeriod
     *            The delay between two successive bid replacement runs
     *            (replacing spot requests with an out-dated bid price). May be
     *            <code>null</code>. Default:
     *            {@value #DEFAULT_BID_REPLACEMENT_PERIOD}.
     * @param danglingInstanceCleanupPeriod
     *            The delay between two successive dangling instance cleanup
     *            runs (where instances whose spot requests have been canceled
     *            are terminated). May be <code>null</code>. Default:
     *            {@value #DEFAULT_DANGLING_INSTANCE_CLEANUP_PERIOD}.
     */
    public CloudApiSettings(String awsAccessKeyId, String awsSecretAccessKey, String region, double bidPrice,
            TimeInterval bidReplacementPeriod, TimeInterval danglingInstanceCleanupPeriod) {
        this(awsAccessKeyId, awsSecretAccessKey, region, bidPrice, bidReplacementPeriod, danglingInstanceCleanupPeriod,
                null, null);
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
     * @param bidPrice
     *            The bid price (maximum price to pay for an instance hour in
     *            dollars) to use when requesting spot instances.
     * @param bidReplacementPeriod
     *            The delay between two successive bid replacement runs
     *            (replacing spot requests with an out-dated bid price). May be
     *            <code>null</code>. Default:
     *            {@value #DEFAULT_BID_REPLACEMENT_PERIOD}.
     * @param danglingInstanceCleanupPeriod
     *            The delay between two successive dangling instance cleanup
     *            runs (where instances whose spot requests have been canceled
     *            are terminated). May be <code>null</code>. Default:
     *            {@value #DEFAULT_DANGLING_INSTANCE_CLEANUP_PERIOD}.
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
    public CloudApiSettings(String awsAccessKeyId, String awsSecretAccessKey, String region, double bidPrice,
            TimeInterval bidReplacementPeriod, TimeInterval danglingInstanceCleanupPeriod, Integer connectionTimeout,
            Integer socketTimeout) {
        this.awsAccessKeyId = awsAccessKeyId;
        this.awsSecretAccessKey = awsSecretAccessKey;
        this.region = region;
        this.bidPrice = bidPrice;
        this.bidReplacementPeriod = bidReplacementPeriod;
        this.danglingInstanceCleanupPeriod = danglingInstanceCleanupPeriod;
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
        validate();
    }

    /**
     * The access key id of the AWS account.
     *
     * @return
     */
    public String getAwsAccessKeyId() {
        return this.awsAccessKeyId;
    }

    /**
     * The secret access key of the AWS account.
     *
     * @return
     */
    public String getAwsSecretAccessKey() {
        return this.awsSecretAccessKey;
    }

    /**
     * The particular AWS region to connect to. For example, {@code us-east-1}.
     *
     * @return
     */
    public String getRegion() {
        return this.region;
    }

    /**
     * The bid price (maximum price to pay for an instance hour in dollars) to
     * use when requesting spot instances.
     *
     * @return
     */
    public double getBidPrice() {
        return round(this.bidPrice, AMAZON_BIDPRICE_PRECISION);
    }

    /**
     * Round off value to the specified number of decimals.
     *
     * @param value
     *            The value to be rounded.
     * @param precision
     *            Number of decimals to keep.
     * @return
     */
    private double round(double value, int precision) {
        double shift = Math.pow(10, precision);
        return Math.round(value * shift) / shift;
    }

    /**
     * The delay between two successive bid replacement runs (replacing spot
     * requests with an out-dated bid price).
     *
     * @return
     */
    public TimeInterval getBidReplacementPeriod() {
        return Optional.ofNullable(this.bidReplacementPeriod).orElse(DEFAULT_BID_REPLACEMENT_PERIOD);
    }

    /**
     * The delay between two successive dangling instance cleanup runs (where
     * instances whose spot requests have been canceled are terminated).
     *
     * @return
     */
    public TimeInterval getDanglingInstanceCleanupPeriod() {
        return Optional.ofNullable(this.danglingInstanceCleanupPeriod).orElse(DEFAULT_DANGLING_INSTANCE_CLEANUP_PERIOD);
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CloudApiSettings) {
            CloudApiSettings that = (CloudApiSettings) obj;
            return Objects.equals(this.awsAccessKeyId, that.awsAccessKeyId) //
                    && Objects.equals(this.awsSecretAccessKey, that.awsSecretAccessKey) //
                    && Objects.equals(this.region, that.region) //
                    && Objects.equals(this.bidPrice, that.bidPrice) //
                    && Objects.equals(getBidReplacementPeriod(), that.getBidReplacementPeriod()) //
                    && Objects.equals(getDanglingInstanceCleanupPeriod(), that.getDanglingInstanceCleanupPeriod()) //
                    && Objects.equals(getConnectionTimeout(), that.getConnectionTimeout()) //
                    && Objects.equals(getSocketTimeout(), that.getSocketTimeout());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.awsAccessKeyId, this.awsSecretAccessKey, this.region, this.bidPrice,
                getBidReplacementPeriod(), getDanglingInstanceCleanupPeriod(), getConnectionTimeout(),
                getSocketTimeout());
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.awsAccessKeyId != null, "cloudApiSettings: missing awsAccessKeyId");
        checkArgument(this.awsSecretAccessKey != null, "cloudApiSettings: missing awsSecretAccessKey");
        checkArgument(this.region != null, "cloudApiSettings: missing region");
        checkArgument(getBidPrice() > 0, "cloudApiSettings: bidPrice must be > 0, set to %s", getBidPrice());
        try {
            getBidReplacementPeriod().validate();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("cloudApiSettings: bidReplacementPeriod: " + e.getMessage(), e);
        }

        try {
            getDanglingInstanceCleanupPeriod().validate();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("cloudApiSettings: danglingInstanceCleanupPeriod: " + e.getMessage(), e);
        }

        checkArgument(getConnectionTimeout() > 0, "cloudApiSettings: connectionTimeout must be positive");
        checkArgument(getSocketTimeout() > 0, "cloudApiSettings: socketTimeout must be positive");
    }
}
