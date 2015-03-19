package com.elastisys.scale.cloudpool.aws.commons.poolclient.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2Client;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.CreateInstance;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.GetInstance;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.GetInstances;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.TagEc2Resource;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.TerminateInstance;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.UntagEc2Resource;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleOutConfig;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.Atomics;

/**
 * Standard {@link Ec2Client} implementation that operates against the EC2 API.
 */
public class AwsEc2Client implements Ec2Client {

	private static Logger LOG = LoggerFactory.getLogger(AwsEc2Client.class);

	private final AtomicReference<String> awsAccessKeyId;
	private final AtomicReference<String> awsSecretAccessKey;
	private final AtomicReference<String> region;

	public AwsEc2Client() {
		this.awsAccessKeyId = Atomics.newReference();
		this.awsSecretAccessKey = Atomics.newReference();
		this.region = Atomics.newReference();
	}

	@Override
	public void configure(String awsAccessKeyId, String awsSecretAccessKey,
			String region) {
		checkArgument(awsAccessKeyId != null, "no awsAccessKeyId given");
		checkArgument(awsSecretAccessKey != null, "no awsSecretAccessKey given");
		checkArgument(region != null, "no region given");
		this.awsAccessKeyId.set(awsAccessKeyId);
		this.awsSecretAccessKey.set(awsSecretAccessKey);
		this.region.set(region);
	}

	@Override
	public List<Instance> getInstances(List<Filter> filters)
			throws AmazonClientException {
		checkArgument(isConfigured(), "can't use client before it's configured");

		List<Instance> instances = new GetInstances(awsCredentials(), region())
				.withFilters(filters).call();
		return instances;
	}

	@Override
	public Instance getInstanceMetadata(String instanceId)
			throws NotFoundException, AmazonClientException {
		checkArgument(isConfigured(), "can't use client before it's configured");

		return new GetInstance(awsCredentials(), region(), instanceId).call();
	}

	@Override
	public Instance launchInstance(ScaleOutConfig provisioningDetails)
			throws AmazonClientException {
		checkArgument(isConfigured(), "can't use client before it's configured");

		// no particular availability zone
		String availabilityZone = null;
		String bootscript = Joiner.on("\n").join(
				provisioningDetails.getBootScript());

		Instance startedInstance = new CreateInstance(awsCredentials(),
				region(), availabilityZone,
				provisioningDetails.getSecurityGroups(),
				provisioningDetails.getKeyPair(),
				provisioningDetails.getSize(), provisioningDetails.getImage(),
				bootscript).call();

		return startedInstance;
	}

	@Override
	public void tagResource(String resourceId, List<Tag> tags)
			throws AmazonClientException {
		checkArgument(isConfigured(), "can't use client before it's configured");

		new TagEc2Resource(awsCredentials(), region(), resourceId, tags).call();
	}

	@Override
	public void untagResource(String resourceId, List<Tag> tags)
			throws AmazonClientException {
		checkArgument(isConfigured(), "can't use client before it's configured");

		new UntagEc2Resource(awsCredentials(), region(), resourceId, tags)
				.call();
	}

	@Override
	public void terminateInstance(String instanceId) throws NotFoundException,
			AmazonClientException {
		checkArgument(isConfigured(), "can't use client before it's configured");

		// verify that instance exists
		getInstanceMetadata(instanceId);

		new TerminateInstance(awsCredentials(), region(), instanceId).call();
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
}
