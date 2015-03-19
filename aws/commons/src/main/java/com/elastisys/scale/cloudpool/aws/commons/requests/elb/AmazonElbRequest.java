package com.elastisys.scale.cloudpool.aws.commons.requests.elb;

import com.amazonaws.auth.AWSCredentials;
import com.elastisys.scale.cloudpool.aws.commons.client.LoadBalancingApiClient;
import com.elastisys.scale.cloudpool.aws.commons.requests.AmazonRequest;

/**
 * An abstract base class for AWS Elastic Load Balancer request clients.
 * 
 * 
 * 
 * @param <R>
 *            The response type.
 */
public abstract class AmazonElbRequest<R> extends AmazonRequest<R> {

	/** AWS Elastic Load Balancer API client. */
	private final LoadBalancingApiClient client;

	/**
	 * Constructs a new {@link AmazonElbRequest}.
	 * 
	 * @param awsCredentials
	 *            AWS security credentials for the account to be used.
	 * @param region
	 *            The AWS region that the request will be sent to.
	 */
	public AmazonElbRequest(AWSCredentials awsCredentials, String region) {
		super(awsCredentials, region);
		this.client = new LoadBalancingApiClient(getAwsCredentials(), getRegion());
	}

	/**
	 * Returns the AWS Elastic Load Balancer API client.
	 * 
	 * @return
	 */
	public LoadBalancingApiClient getClient() {
		return this.client;
	}
}
