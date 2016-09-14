package com.elastisys.scale.cloudpool.aws.commons.requests;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;

/**
 * An abstract base class for AWS request clients.
 *
 * @param <R>
 *            The response type.
 */
public abstract class AmazonRequest<R> implements Callable<R> {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** AWS security credentials for the account to be used. */
    private final AWSCredentials awsCredentials;
    /** The AWS region that the request will be sent to. */
    private final String region;
    /** Client configuration options such as connection timeout, etc. */
    private final ClientConfiguration clientConfig;

    /**
     * Constructs a new {@link AmazonRequest} instance.
     *
     * @param awsCredentials
     *            AWS security credentials for the account to be used.
     * @param region
     *            The AWS region that the request will be sent to.
     * @param clientConfig
     *            Client configuration options such as connection timeout, etc.
     */
    public AmazonRequest(AWSCredentials awsCredentials, String region, ClientConfiguration clientConfig) {
        this.awsCredentials = awsCredentials;
        this.clientConfig = clientConfig;
        this.region = region;
    }

    public AWSCredentials getAwsCredentials() {
        return this.awsCredentials;
    }

    public String getRegion() {
        return this.region;
    }

    public ClientConfiguration getClientConfig() {
        return this.clientConfig;
    }
}
