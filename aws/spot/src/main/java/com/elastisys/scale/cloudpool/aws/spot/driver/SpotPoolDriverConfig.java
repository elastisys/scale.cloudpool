package com.elastisys.scale.cloudpool.aws.spot.driver;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * Configuration for a {@link SpotPoolDriver}.
 */
public class SpotPoolDriverConfig {
	/**
	 * Number of decimals in Amazon bid prices. For example, a bid price of
	 * $0.0123456789 gets rounded off to $0.012346 by Amazon (note the
	 * rounding).
	 */
	private static final int AMAZON_BIDPRICE_PRECISION = 6;

	/** Default value for {@link #bidReplacementPeriod} */
	public static final Long DEFAULT_BID_REPLACEMENT_PERIOD = 120L;

	/** Default value for {@link #bidReplacementPeriod} */
	public static final Long DEFAULT_DANGLING_INSTANCE_CLEANUP_PERIOD = 120L;

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
	 * The delay (in seconds) between two successive runs of replacing spot
	 * requests with an out-dated bid price.
	 */
	private final Long bidReplacementPeriod;
	/**
	 * The delay (in seconds) between two successive runs of terminating
	 * instances that are running, but whose spot requests were canceled.
	 */
	private final Long danglingInstanceCleanupPeriod;
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
	 * Creates a new {@link SpotPoolDriverConfig}.
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
	 *            The delay (in seconds) between two successive runs of
	 *            replacing spot requests with an out-dated bid price. May be
	 *            <code>null</code>. Default:
	 *            {@value #DEFAULT_BID_REPLACEMENT_PERIOD}.
	 * @param danglingInstanceCleanupPeriod
	 *            The delay (in seconds) between two successive runs of
	 *            terminating instances that are running, but whose spot
	 *            requests were canceled. May be <code>null</code>. Default:
	 *            {@value #DEFAULT_DANGLING_INSTANCE_CLEANUP_PERIOD}.
	 */
	public SpotPoolDriverConfig(String awsAccessKeyId,
			String awsSecretAccessKey, String region, double bidPrice,
			Long bidReplacementPeriod, Long danglingInstanceCleanupPeriod) {
		this(awsAccessKeyId, awsSecretAccessKey, region, bidPrice,
				bidReplacementPeriod, danglingInstanceCleanupPeriod, null,
				null);
	}

	/**
	 * Creates a new {@link SpotPoolDriverConfig}.
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
	 *            The delay (in seconds) between two successive runs of
	 *            replacing spot requests with an out-dated bid price. May be
	 *            <code>null</code>. Default:
	 *            {@value #DEFAULT_BID_REPLACEMENT_PERIOD}.
	 * @param danglingInstanceCleanupPeriod
	 *            The delay (in seconds) between two successive runs of
	 *            terminating instances that are running, but whose spot
	 *            requests were canceled. May be <code>null</code>. Default:
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
	public SpotPoolDriverConfig(String awsAccessKeyId,
			String awsSecretAccessKey, String region, double bidPrice,
			Long bidReplacementPeriod, Long danglingInstanceCleanupPeriod,
			Integer connectionTimeout, Integer socketTimeout) {
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
	 * The delay (in seconds) between two successive runs of replacing spot
	 * requests with an out-dated bid price.
	 * 
	 * @return
	 */
	public Long getBidReplacementPeriod() {
		return Optional.fromNullable(this.bidReplacementPeriod)
				.or(DEFAULT_BID_REPLACEMENT_PERIOD);
	}

	/**
	 * The delay (in seconds) between two successive runs of terminating
	 * instances that are running, but whose spot requests were canceled.
	 *
	 * @return
	 */
	public Long getDanglingInstanceCleanupPeriod() {
		return Optional.fromNullable(this.danglingInstanceCleanupPeriod)
				.or(DEFAULT_DANGLING_INSTANCE_CLEANUP_PERIOD);
	}

	/**
	 * The timeout in milliseconds until a connection is established.
	 *
	 * @return
	 */
	public Integer getConnectionTimeout() {
		return Optional.fromNullable(this.connectionTimeout)
				.or(DEFAULT_CONNECTION_TIMEOUT);
	}

	/**
	 * The socket timeout ({@code SO_TIMEOUT}) in milliseconds, which is the
	 * timeout for waiting for data or, put differently, a maximum period
	 * inactivity between two consecutive data packets.
	 *
	 * @return
	 */
	public Integer getSocketTimeout() {
		return Optional.fromNullable(this.socketTimeout)
				.or(DEFAULT_SOCKET_TIMEOUT);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SpotPoolDriverConfig) {
			SpotPoolDriverConfig that = (SpotPoolDriverConfig) obj;
			return Objects.equal(this.awsAccessKeyId, that.awsAccessKeyId)
					&& Objects.equal(this.awsSecretAccessKey,
							that.awsSecretAccessKey)
					&& Objects.equal(this.region, that.region)
					&& Objects.equal(this.bidPrice, that.bidPrice)
					&& Objects.equal(this.getBidReplacementPeriod(),
							that.getBidReplacementPeriod())
					&& Objects.equal(this.getDanglingInstanceCleanupPeriod(),
							that.getDanglingInstanceCleanupPeriod())
					&& equal(this.getConnectionTimeout(),
							that.getConnectionTimeout())
					&& equal(this.getSocketTimeout(), that.getSocketTimeout());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.awsAccessKeyId, this.awsSecretAccessKey,
				this.region, this.bidPrice, this.getBidReplacementPeriod(),
				this.getDanglingInstanceCleanupPeriod(), getConnectionTimeout(),
				getSocketTimeout());
	}

	@Override
	public String toString() {
		return JsonUtils.toString(JsonUtils.toJson(this));
	}

	public void validate() throws IllegalArgumentException {
		checkArgument(this.awsAccessKeyId != null,
				"SpotPoolDriver config missing awsAccessKeyId");
		checkArgument(this.awsSecretAccessKey != null,
				"SpotPoolDriver config missing awsSecretAccessKey");
		checkArgument(this.region != null,
				"SpotPoolDriver config missing region");
		checkArgument(this.getBidPrice() > 0,
				"SpotPoolDriver config bidPrice must be > 0, set to %s",
				getBidPrice());
		checkArgument(getBidReplacementPeriod() > 0,
				"SpotPoolDriver config bidReplacementPeriod must be > 0");
		checkArgument(getDanglingInstanceCleanupPeriod() > 0,
				"SpotPoolDriver config danglingInstanceCleanupPeriod must be > 0");
		checkArgument(getConnectionTimeout() > 0,
				"connectionTimeout must be positive");
		checkArgument(getSocketTimeout() > 0, "socketTimeout must be positive");
	}
}
