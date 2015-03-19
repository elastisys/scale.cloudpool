package com.elastisys.scale.cloudpool.aws.commons.client;

import java.io.Closeable;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;

/**
 * An Amazon EC2 Autoscaling client that connects to and operates against a
 * specific AWS region.
 * <p/>
 * Regions are logically isolated from each other, so for example, a client
 * created to connect to region us-east-1 won't be able to see resources created
 * by a client for region us-west-1.
 * <p/>
 * If you need to access multiple AWS regions, a separate client should be
 * instantiated for each region.
 *
 * See <a href="http://docs.aws.amazon.com/general/latest/gr/rande.html">regions
 * and endpoints</a> for an exhaustive list of available regions.
 * <p/>
 * Instances of this class are thread-safe and can therefore be used
 * concurrently from multiple threads.
 *
 *
 */
public class AutoScalingApiClient implements Closeable {

	/** The AWS region that this client operates against. */
	private final String region;
	/**
	 * The {@link AmazonAutoScaling} client through which API operations can be
	 * invoked.
	 */
	private final AmazonAutoScaling api;

	/**
	 * Constructs a new {@link AutoScalingApiClient} that operates against a given
	 * AWS region.
	 *
	 * @param awsCredentials
	 *            The AWS credentials used to connect to the AWS account.
	 * @param region
	 *            The AWS region that this client operates against.
	 */
	public AutoScalingApiClient(AWSCredentials awsCredentials, String region) {
		this(awsCredentials, region, new ClientConfiguration());
	}

	/**
	 * Constructs a new {@link AutoScalingApiClient} that operates against a given
	 * AWS region.
	 *
	 * @param awsCredentials
	 *            The AWS credentials used to connect to the AWS account.
	 * @param region
	 *            The AWS region that this client operates against.
	 * @param clientConfiguration
	 *            Any HTTP client configuration to customize API invocations.
	 */
	public AutoScalingApiClient(AWSCredentials awsCredentials, String region,
			ClientConfiguration clientConfiguration) {
		// limit the time-to-live of the JVM's DNS cache (in seconds)
		java.security.Security.setProperty("networkaddress.cache.ttl", "60");

		this.region = region;
		this.api = new AmazonAutoScalingClient(awsCredentials,
				clientConfiguration);
		String endpoint = "autoscaling." + region + ".amazonaws.com";
		this.api.setEndpoint(endpoint);
	}

	/**
	 * Returns the AWS region that this client operates against.
	 *
	 * @return
	 */
	public String getRegion() {
		return this.region;
	}

	/**
	 * Returns the {@link AmazonAutoScaling} client through which API operations
	 * can be invoked.
	 *
	 * @return
	 */
	public AmazonAutoScaling getApi() {
		return this.api;
	}

	/**
	 * Closes the client, releasing any held resources.
	 */
	@Override
	public void close() {
		this.api.shutdown();
	}
}
