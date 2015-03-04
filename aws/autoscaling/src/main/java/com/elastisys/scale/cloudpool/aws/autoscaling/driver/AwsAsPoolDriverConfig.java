package com.elastisys.scale.cloudpool.aws.autoscaling.driver;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;

/**
 * Configuration object for an {@link AwsAsPoolDriver}.
 */
public class AwsAsPoolDriverConfig {

	/** The access key id of the AWS account. */
	private final String awsAccessKeyId;
	/** The secret access key of the AWS account. */
	private final String awsSecretAccessKey;
	/** The particular AWS region to connect to. For example, {@code us-east-1}. */
	private final String region;

	/**
	 * Creates a new {@link AwsAsPoolDriverConfig}.
	 *
	 * @param awsAccessKeyId
	 *            The access key id of the AWS account.
	 * @param awsSecretAccessKey
	 *            The secret access key of the AWS account.
	 * @param region
	 *            The particular AWS region to connect to. For example,
	 *            {@code us-east-1}.
	 */
	public AwsAsPoolDriverConfig(String awsAccessKeyId,
			String awsSecretAccessKey, String region) {
		this.awsAccessKeyId = awsAccessKeyId;
		this.awsSecretAccessKey = awsSecretAccessKey;
		this.region = region;
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
	 * Performs basic validation of this configuration.
	 *
	 * @throws CloudPoolException
	 */
	public void validate() throws CloudPoolException {
		try {
			checkNotNull(this.awsAccessKeyId, "missing awsAccessKeyId");
			checkNotNull(this.awsSecretAccessKey, "missing awsSecretAccessKey");
			checkNotNull(this.region, "missing region");
		} catch (Exception e) {
			// no need to wrap further if already a config exception
			Throwables.propagateIfInstanceOf(e, CloudPoolException.class);
			throw new CloudPoolException(format(
					"failed to validate cloud client configuration: %s",
					e.getMessage()), e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.awsAccessKeyId, this.awsSecretAccessKey,
				this.region);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AwsAsPoolDriverConfig) {
			AwsAsPoolDriverConfig that = (AwsAsPoolDriverConfig) obj;
			return equal(this.awsAccessKeyId, that.awsAccessKeyId)
					&& equal(this.awsSecretAccessKey, that.awsSecretAccessKey)
					&& equal(this.region, that.region);
		}
		return false;
	}
}
