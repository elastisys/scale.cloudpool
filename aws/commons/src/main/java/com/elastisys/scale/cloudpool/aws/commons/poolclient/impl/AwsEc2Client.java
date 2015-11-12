package com.elastisys.scale.cloudpool.aws.commons.poolclient.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2Client;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.CreateInstances;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.GetInstance;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.GetInstances;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.TagEc2Resources;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.TerminateInstances;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.UntagEc2Resource;
import com.elastisys.scale.cloudpool.commons.basepool.config.ScaleOutConfig;
import com.google.common.util.concurrent.Atomics;

/**
 * Standard {@link Ec2Client} implementation that operates against the EC2 API.
 */
public class AwsEc2Client implements Ec2Client {

	private static Logger LOG = LoggerFactory.getLogger(AwsEc2Client.class);

	/** The access key id part of the AWS credentials. */
	private final AtomicReference<String> awsAccessKeyId;
	/** The secret access key part of the AWS credentials. */
	private final AtomicReference<String> awsSecretAccessKey;
	/** The AWS region to operate against. */
	private final AtomicReference<String> region;
	/** Client configuration options such as connection timeout, etc. */
	private final AtomicReference<ClientConfiguration> clientConfig;

	public AwsEc2Client() {
		this.awsAccessKeyId = Atomics.newReference();
		this.awsSecretAccessKey = Atomics.newReference();
		this.region = Atomics.newReference();
		this.clientConfig = Atomics.newReference();
	}

	@Override
	public void configure(String awsAccessKeyId, String awsSecretAccessKey,
			String region, ClientConfiguration clientConfig) {
		checkArgument(awsAccessKeyId != null, "no awsAccessKeyId given");
		checkArgument(awsSecretAccessKey != null,
				"no awsSecretAccessKey given");
		checkArgument(region != null, "no region given");
		checkArgument(clientConfig != null, "no clientConfig given");
		this.awsAccessKeyId.set(awsAccessKeyId);
		this.awsSecretAccessKey.set(awsSecretAccessKey);
		this.region.set(region);
		this.clientConfig.set(clientConfig);
	}

	@Override
	public List<Instance> getInstances(List<Filter> filters)
			throws AmazonClientException {
		checkArgument(isConfigured(),
				"can't use client before it's configured");

		List<Instance> instances = new GetInstances(awsCredentials(), region(),
				clientConfig()).withFilters(filters).call();
		return instances;
	}

	@Override
	public Instance getInstanceMetadata(String instanceId)
			throws NotFoundException, AmazonClientException {
		checkArgument(isConfigured(),
				"can't use client before it's configured");

		return new GetInstance(awsCredentials(), region(), clientConfig(),
				instanceId).call();
	}

	@Override
	public List<Instance> launchInstances(ScaleOutConfig provisioningDetails,
			int count, List<Tag> tags) throws AmazonClientException {
		checkArgument(isConfigured(),
				"can't use client before it's configured");

		// no particular availability zone
		String availabilityZone = null;

		List<Instance> startedInstances = new CreateInstances(awsCredentials(),
				region(), clientConfig(), availabilityZone,
				provisioningDetails.getSecurityGroups(),
				provisioningDetails.getKeyPair(), provisioningDetails.getSize(),
				provisioningDetails.getImage(),
				provisioningDetails.getEncodedUserData(), count, tags).call();

		return startedInstances;
	}

	@Override
	public void tagResource(String resourceId, List<Tag> tags)
			throws AmazonClientException {
		checkArgument(isConfigured(),
				"can't use client before it's configured");

		new TagEc2Resources(awsCredentials(), region(), clientConfig(),
				Arrays.asList(resourceId), tags).call();
	}

	@Override
	public void untagResource(String resourceId, List<Tag> tags)
			throws AmazonClientException {
		checkArgument(isConfigured(),
				"can't use client before it's configured");

		new UntagEc2Resource(awsCredentials(), region(), clientConfig(),
				resourceId, tags).call();
	}

	@Override
	public void terminateInstances(List<String> instanceIds)
			throws NotFoundException, AmazonClientException {
		checkArgument(isConfigured(),
				"can't use client before it's configured");

		new TerminateInstances(awsCredentials(), region(), clientConfig(),
				instanceIds).call();
	}

	private boolean isConfigured() {
		return this.awsAccessKeyId.get() != null
				&& this.awsSecretAccessKey.get() != null && this.region != null;
	}

	/**
	 * Returns the {@link AWSCredentials} that this client is configured to use.
	 *
	 * @return
	 */
	protected AWSCredentials awsCredentials() {
		return new BasicAWSCredentials(this.awsAccessKeyId.get(),
				this.awsSecretAccessKey.get());
	}

	/**
	 * Returns the AWS region that this client is configured to operate against.
	 *
	 * @return
	 */
	protected String region() {
		return this.region.get();
	}

	/**
	 * Client configuration options such as connection timeout, etc.
	 *
	 * @return
	 */
	protected ClientConfiguration clientConfig() {
		return this.clientConfig.get();
	}
}
