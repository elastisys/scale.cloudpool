package com.elastisys.scale.cloudpool.aws.spot.driver;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.MoreObjects;
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

	public SpotPoolDriverConfig(String awsAccessKeyId,
			String awsSecretAccessKey, String region, double bidPrice,
			Long bidReplacementPeriod, Long danglingInstanceCleanupPeriod) {
		this.awsAccessKeyId = awsAccessKeyId;
		this.awsSecretAccessKey = awsSecretAccessKey;
		this.region = region;
		this.bidPrice = bidPrice;
		this.bidReplacementPeriod = bidReplacementPeriod;
		this.danglingInstanceCleanupPeriod = danglingInstanceCleanupPeriod;
		validate();
	}

	/**
	 * @return the {@link #awsAccessKeyId}
	 */
	public String getAwsAccessKeyId() {
		return this.awsAccessKeyId;
	}

	/**
	 * @return the {@link #awsSecretAccessKey}
	 */
	public String getAwsSecretAccessKey() {
		return this.awsSecretAccessKey;
	}

	/**
	 * @return the {@link #region}
	 */
	public String getRegion() {
		return this.region;
	}

	/**
	 * @return the {@link #bidPrice}
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
	 * @return the {@link #bidReplacementPeriod}
	 */
	public Long getBidReplacementPeriod() {
		return Optional.fromNullable(this.bidReplacementPeriod).or(
				DEFAULT_BID_REPLACEMENT_PERIOD);
	}

	/**
	 * @return the {@link #danglingInstanceCleanupPeriod}
	 */
	public Long getDanglingInstanceCleanupPeriod() {
		return Optional.fromNullable(this.danglingInstanceCleanupPeriod).or(
				DEFAULT_DANGLING_INSTANCE_CLEANUP_PERIOD);

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
							that.getDanglingInstanceCleanupPeriod());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.awsAccessKeyId, this.awsSecretAccessKey,
				this.region, this.bidPrice, this.getBidReplacementPeriod(),
				this.getDanglingInstanceCleanupPeriod());
	}

	@Override
	public String toString() {
		return MoreObjects
				.toStringHelper("")
				.add("awsAccessKeyId", this.awsAccessKeyId)
				.add("awsSecretAccessKey", this.awsSecretAccessKey)
				.add("region", this.region)
				.add("bidPrice", this.getBidPrice())
				.add("bidReplacementPeriod", this.getBidReplacementPeriod())
				.add("danglingInstanceCleanupPeriod",
						this.getDanglingInstanceCleanupPeriod()).toString();
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
	}
}
