package com.elastisys.scale.cloudadapters.aws.commons.requests.ec2;

import com.amazonaws.auth.AWSCredentials;
import com.elastisys.scale.cloudadapters.aws.commons.client.Ec2Client;
import com.elastisys.scale.cloudadapters.aws.commons.requests.AmazonRequest;

/**
 * An abstract base class for AWS EC2 request clients.
 * 
 * 
 * 
 * @param <R>
 */
public abstract class AmazonEc2Request<R> extends AmazonRequest<R> {

	/** AWS Elastic Compute Cloud (EC2) API client. */
	private final Ec2Client client;

	/**
	 * Constructs a new {@link AmazonEc2Request}.
	 * 
	 * @param awsCredentials
	 *            AWS security credentials for the account to be used.
	 * @param region
	 *            The AWS region that the request will be sent to.
	 */
	public AmazonEc2Request(AWSCredentials awsCredentials, String region) {
		super(awsCredentials, region);
		this.client = new Ec2Client(awsCredentials, region);
	}

	/**
	 * Returns an AWS Elastic Compute Cloud (EC2) API client.
	 * 
	 * @return
	 */
	public Ec2Client getClient() {
		return this.client;
	}
}
