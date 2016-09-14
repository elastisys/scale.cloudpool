package com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.elastisys.scale.cloudpool.aws.commons.client.AutoScalingApiClient;
import com.elastisys.scale.cloudpool.aws.commons.requests.AmazonRequest;

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
    private final AutoScalingApiClient client;

    /**
     * Constructs a new {@link AmazonAutoScalingRequest}.
     *
     * @param awsCredentials
     *            AWS security credentials for the account to be used.
     * @param region
     *            The AWS region that the request will be sent to.
     * @param clientConfig
     *            Client configuration options such as connection timeout, etc.
     */
    public AmazonAutoScalingRequest(AWSCredentials awsCredentials, String region, ClientConfiguration clientConfig) {
        super(awsCredentials, region, clientConfig);
        this.client = new AutoScalingApiClient(getAwsCredentials(), getRegion(), clientConfig);
    }

    /**
     * Returns the AWS Auto Scaling API client.
     *
     * @return
     */
    public AutoScalingApiClient getClient() {
        return this.client;
    }

}
