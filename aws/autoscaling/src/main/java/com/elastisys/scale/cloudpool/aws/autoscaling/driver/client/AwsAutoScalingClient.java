package com.elastisys.scale.cloudpool.aws.autoscaling.driver.client;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.AwsAsPoolDriverConfig;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.AttachAutoScalingGroupInstance;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.DetachAutoScalingGroupInstance;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.GetAutoScalingGroup;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.GetAutoScalingGroupInstances;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.GetLaunchConfiguration;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.SetDesiredAutoScalingGroupSize;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.TerminateAutoScalingGroupInstance;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.GetInstance;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.TagEc2Resources;
import com.google.common.util.concurrent.Atomics;

/**
 * Standard implementation of the {@link AutoScalingClient} interface.
 *
 *
 *
 */
public class AwsAutoScalingClient implements AutoScalingClient {
	private final AtomicReference<AwsAsPoolDriverConfig> config;

	public AwsAutoScalingClient() {
		this.config = Atomics.newReference();
	}

	@Override
	public void configure(AwsAsPoolDriverConfig configuration) {
		checkArgument(configuration != null, "null configuration");
		this.config.set(configuration);
	}

	@Override
	public AutoScalingGroup getAutoScalingGroup(String autoScalingGroupName) {
		checkArgument(isConfigured(),
				"can't use client before it's configured");

		return new GetAutoScalingGroup(awsCredentials(), region(),
				autoScalingGroupName).call();
	}

	@Override
	public LaunchConfiguration getLaunchConfiguration(
			String launchConfigurationName) {
		checkArgument(isConfigured(),
				"can't use client before it's configured");

		return new GetLaunchConfiguration(awsCredentials(), region(),
				launchConfigurationName).call();
	}

	@Override
	public List<Instance> getAutoScalingGroupMembers(
			String autoScalingGroupName) {
		checkArgument(isConfigured(),
				"can't use client before it's configured");

		return new GetAutoScalingGroupInstances(awsCredentials(), region(),
				autoScalingGroupName).call();
	}

	@Override
	public void setDesiredSize(String autoScalingGroupName, int desiredSize) {
		checkArgument(isConfigured(),
				"can't use client before it's configured");

		new SetDesiredAutoScalingGroupSize(awsCredentials(),
				config().getRegion(), autoScalingGroupName, desiredSize).call();
	}

	@Override
	public void terminateInstance(String autoScalingGroupName,
			String instanceId) throws NotFoundException {
		checkArgument(isConfigured(),
				"can't use client before it's configured");

		// verify that instance exists
		getInstanceOrFail(instanceId);

		new TerminateAutoScalingGroupInstance(awsCredentials(), region(),
				instanceId).call();
	}

	@Override
	public void attachInstance(String autoScalingGroupName, String instanceId)
			throws NotFoundException {
		checkArgument(isConfigured(),
				"can't use client before it's configured");

		// verify that instance exists
		getInstanceOrFail(instanceId);

		new AttachAutoScalingGroupInstance(awsCredentials(), region(),
				autoScalingGroupName, instanceId).call();
	}

	@Override
	public void detachInstance(String autoScalingGroupName, String instanceId)
			throws NotFoundException {
		checkArgument(isConfigured(),
				"can't use client before it's configured");

		// verify that instance exists
		getInstanceOrFail(instanceId);

		new DetachAutoScalingGroupInstance(awsCredentials(), region(),
				autoScalingGroupName, instanceId).call();
	}

	@Override
	public void tagInstance(String instanceId, List<Tag> tags)
			throws NotFoundException {
		checkArgument(isConfigured(),
				"can't use client before it's configured");

		// verify that instance exists
		getInstanceOrFail(instanceId);

		new TagEc2Resources(awsCredentials(), region(),
				Arrays.asList(instanceId), tags).call();
	}

	private Instance getInstanceOrFail(String instanceId)
			throws NotFoundException {
		checkArgument(isConfigured(),
				"can't use client before it's configured");

		return new GetInstance(awsCredentials(), region(), instanceId).call();
	}

	private boolean isConfigured() {
		return this.config.get() != null;
	}

	private AwsAsPoolDriverConfig config() {
		return this.config.get();
	}

	private AWSCredentials awsCredentials() {
		return new BasicAWSCredentials(config().getAwsAccessKeyId(),
				config().getAwsSecretAccessKey());
	}

	private String region() {
		return config().getRegion();
	}
}
