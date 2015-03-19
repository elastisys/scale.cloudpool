package com.elastisys.scale.cloudpool.aws.commons.requests.cloudwatch;

import com.amazonaws.auth.AWSCredentials;
import com.elastisys.scale.cloudpool.aws.commons.client.CloudWatchApiClient;
import com.elastisys.scale.cloudpool.aws.commons.requests.AmazonRequest;

/**
 * An abstract base class for AWS CloudWatch request clients.
 * 
 * 
 * 
 * @param <R>
 *            The response type.
 */
public abstract class AmazonCloudWatchRequest<R> extends AmazonRequest<R> {

	/** AWS CloudWatch API client. */
	private final CloudWatchApiClient client;

	/**
	 * Constructs a new {@link AmazonCloudWatchRequest}.
	 * 
	 * @param awsCredentials
	 *            AWS security credentials for the account to be used.
	 * @param region
	 *            The AWS region that the request will be sent to.
	 */
	public AmazonCloudWatchRequest(AWSCredentials awsCredentials, String region) {
		super(awsCredentials, region);
		this.client = new CloudWatchApiClient(getAwsCredentials(), getRegion());
	}

	/**
	 * Returns the AWS CloudWatch API client.
	 * 
	 * @return
	 */
	public CloudWatchApiClient getClient() {
		return this.client;
	}

}
