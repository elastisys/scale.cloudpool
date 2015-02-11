package com.elastisys.scale.cloudadapters.aws.commons.client;

import java.io.Closeable;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;

/**
 * An Amazon Elastic Load Balancer client that connects to and operates against
 * a specific AWS region.
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
 *
 */
public class LoadBalancingClient implements Closeable {
	/** The AWS region that this client operates against. */
	private final String region;
	/**
	 * The {@link AmazonElasticLoadBalancing} client through which API
	 * operations can be invoked.
	 */
	private final AmazonElasticLoadBalancing api;

	/**
	 * Constructs a new {@link LoadBalancingClient} that operates against a
	 * given AWS region.
	 *
	 * @param awsCredentials
	 *            The AWS credentials used to connect to the AWS account.
	 * @param region
	 *            The AWS region that this client operates against.
	 */
	public LoadBalancingClient(AWSCredentials awsCredentials, String region) {
		this(awsCredentials, region, new ClientConfiguration());
	}

	/**
	 * Constructs a new {@link LoadBalancingClient} that operates against a
	 * given AWS region.
	 *
	 * @param awsCredentials
	 *            The AWS credentials used to connect to the AWS account.
	 * @param region
	 *            The AWS region that this client operates against.
	 * @param clientConfiguration
	 *            Any HTTP client configuration to customize API invocations.
	 */
	public LoadBalancingClient(AWSCredentials awsCredentials, String region,
			ClientConfiguration clientConfiguration) {
		// limit the time-to-live of the JVM's DNS cache (in seconds)
		java.security.Security.setProperty("networkaddress.cache.ttl", "60");

		this.region = region;
		this.api = new AmazonElasticLoadBalancingClient(awsCredentials,
				clientConfiguration);
		String endpoint = "elasticloadbalancing." + region + ".amazonaws.com";
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
	 * Returns the {@link AmazonElasticLoadBalancing} client through which API
	 * operations can be invoked.
	 *
	 * @return
	 */
	public AmazonElasticLoadBalancing getApi() {
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
