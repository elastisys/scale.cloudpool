package com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling;

import com.amazonaws.auth.AWSCredentials;
import com.elastisys.scale.cloudpool.aws.commons.client.AutoScalingClient;
import com.elastisys.scale.cloudpool.aws.commons.requests.AmazonRequest;
import com.elastisys.scale.cloudpool.aws.commons.requests.elb.AmazonElbRequest;

/**
 * An abstract base class for AWS Auto Scaling request clients.
 * 
 * 
 * 
 * @param <R>
 *            The response type.
 */
public abstract class AmazonAutoScalingRequest<R> extends AmazonRequest<R> {
	/** AWS Auto Scaling API client. */
	private final AutoScalingClient client;

	/**
	 * Constructs a new {@link AmazonElbRequest}.
	 * 
	 * @param awsCredentials
	 *            AWS security credentials for the account to be used.
	 * @param region
	 *            The AWS region that the request will be sent to.
	 */
	public AmazonAutoScalingRequest(AWSCredentials awsCredentials, String region) {
		super(awsCredentials, region);
		this.client = new AutoScalingClient(getAwsCredentials(), getRegion());
	}

	/**
	 * Returns the AWS Auto Scaling API client.
	 * 
	 * @return
	 */
	public AutoScalingClient getClient() {
		return this.client;
	}

}
