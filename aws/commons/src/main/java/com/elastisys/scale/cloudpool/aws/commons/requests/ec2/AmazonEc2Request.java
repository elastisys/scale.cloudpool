package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.elastisys.scale.cloudpool.aws.commons.client.Ec2ApiClient;
import com.elastisys.scale.cloudpool.aws.commons.requests.AmazonRequest;

/**
 * An abstract base class for AWS EC2 request clients.
 *
 * @param <R>
 *            Response type.
 */
public abstract class AmazonEc2Request<R> extends AmazonRequest<R> {

    /** AWS Elastic Compute Cloud (EC2) API client. */
    private final Ec2ApiClient client;

    /**
     * Constructs a new {@link AmazonEc2Request}.
     *
     * @param awsCredentials
     *            AWS security credentials for the account to be used.
     * @param region
     *            The AWS region that the request will be sent to.
     * @param clientConfig
     *            Client configuration options such as connection timeout, etc.
     */
    public AmazonEc2Request(AWSCredentials awsCredentials, String region, ClientConfiguration clientConfig) {
        super(awsCredentials, region, clientConfig);
        this.client = new Ec2ApiClient(awsCredentials, region, clientConfig);
    }

    /**
     * Returns an AWS Elastic Compute Cloud (EC2) API client.
     *
     * @return
     */
    public Ec2ApiClient getClient() {
        return this.client;
    }
}
