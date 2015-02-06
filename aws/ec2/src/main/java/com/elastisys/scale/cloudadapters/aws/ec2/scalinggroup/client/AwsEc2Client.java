package com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.client;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudadapters.aws.commons.requests.ec2.CreateInstance;
import com.elastisys.scale.cloudadapters.aws.commons.requests.ec2.GetInstances;
import com.elastisys.scale.cloudadapters.aws.commons.requests.ec2.TagEc2Resource;
import com.elastisys.scale.cloudadapters.aws.commons.requests.ec2.TerminateInstance;
import com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.Ec2ScalingGroup;
import com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.Ec2ScalingGroupConfig;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Atomics;

/**
 * Standard {@link Ec2Client} implementation that operates against the EC2 API.
 *
 *
 *
 */
public class AwsEc2Client implements Ec2Client {

	static Logger LOG = LoggerFactory.getLogger(Ec2ScalingGroup.class);

	/** Scaling Group configuration. */
	private final AtomicReference<Ec2ScalingGroupConfig> config;

	public AwsEc2Client() {
		this.config = Atomics.newReference();
	}

	@Override
	public void configure(Ec2ScalingGroupConfig configuration) {
		checkArgument(configuration != null, "null configuration");
		this.config.set(configuration);

	}

	@Override
	public List<Instance> getInstances(List<Filter> filters) {
		checkArgument(isConfigured(), "can't use client before it's configured");

		List<Instance> instances = new GetInstances(awsCredentials(), region())
				.withFilters(filters).call();
		return instances;
	}

	@Override
	public Instance getInstanceMetadata(String instanceId) {
		checkArgument(isConfigured(), "can't use client before it's configured");

		List<Instance> instances = new GetInstances(awsCredentials(), region(),
				Arrays.asList(instanceId)).call();
		if (instances.isEmpty()) {
			throw new IllegalArgumentException(String.format(
					"failed to get instance metadata: "
							+ "no instance matching id '%s' was found",
					instanceId));
		}
		return Iterables.getOnlyElement(instances);
	}

	@Override
	public Instance launchInstance(ScaleUpConfig provisioningDetails) {
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
	public void tagInstance(String instanceId, List<Tag> tags) {
		checkArgument(isConfigured(), "can't use client before it's configured");

		new TagEc2Resource(awsCredentials(), region(), instanceId, tags).call();
	}

	@Override
	public void terminateInstance(String instanceId) {
		checkArgument(isConfigured(), "can't use client before it's configured");

		new TerminateInstance(awsCredentials(), region(), instanceId).call();
	}

	private Ec2ScalingGroupConfig config() {
		return this.config.get();
	}

	private boolean isConfigured() {
		return config() != null;
	}

	private AWSCredentials awsCredentials() {
		return new BasicAWSCredentials(config().getAwsAccessKeyId(), config()
				.getAwsSecretAccessKey());
	}

	private String region() {
		return config().getRegion();
	}
}
