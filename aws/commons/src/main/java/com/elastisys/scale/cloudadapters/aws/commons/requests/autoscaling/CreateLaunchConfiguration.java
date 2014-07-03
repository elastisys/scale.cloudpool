package com.elastisys.scale.cloudadapters.aws.commons.requests.autoscaling;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.InstanceMonitoring;
import com.elastisys.scale.cloudadapters.aws.commons.client.AmazonApiUtils;
import com.elastisys.scale.commons.util.io.IoUtils;
import com.google.common.base.Charsets;

/**
 * A {@link Callable} task that, when executed, requests the creation of an AWS
 * Launch Configuration.
 * 
 * 
 * 
 */
public class CreateLaunchConfiguration extends AmazonAutoScalingRequest<Void> {
	/** The name of the auto-scaling launch configuration create. */
	private final String launchConfigurationName;

	/**
	 * The AWS security group(s) to use for instances created from the launch
	 * configuration.
	 */
	private final List<String> securityGroups;
	/**
	 * The EC2 key pair to use for instances created from the launch
	 * configuration.
	 */
	private final String keyPair;

	/**
	 * The EC2 instance type to use for instances created from the launch
	 * configuration.
	 */
	private final String instanceType;
	/**
	 * The AMI (amazon machine image) id to use for instances created from the
	 * launch configuration.
	 */
	private final String imageId;

	/**
	 * The user data boot script to use for instances created from the launch
	 * configuration.
	 */
	private final String bootScript;

	/**
	 * <code>true</code> for detailed (one-minute) CloudWatch monitoring,
	 * <code>false</code> for basic (five-minute) monitoring.
	 */
	private final boolean detailedMonitoring;

	/**
	 * Constructs a new {@link CreateLaunchConfiguration} task with boot script
	 * passed as a {@link String}.
	 * 
	 * @param awsCredentials
	 * @param region
	 * @param launchConfigurationName
	 * @param securityGroups
	 * @param keyPair
	 * @param instanceType
	 * @param imageId
	 * @param bootScript
	 * @param detailedMonitoring
	 */
	public CreateLaunchConfiguration(AWSCredentials awsCredentials,
			String region, String launchConfigurationName,
			List<String> securityGroups, String keyPair, String instanceType,
			String imageId, String bootScript, boolean detailedMonitoring) {
		super(awsCredentials, region);
		this.launchConfigurationName = launchConfigurationName;
		this.securityGroups = securityGroups;
		this.keyPair = keyPair;
		this.instanceType = instanceType;
		this.imageId = imageId;
		this.bootScript = bootScript;
		this.detailedMonitoring = detailedMonitoring;
	}

	/**
	 * Constructs a new {@link CreateLaunchConfiguration} task with boot script
	 * passed as a {@link File} reference.
	 * 
	 * @param awsCredentials
	 * @param region
	 * @param launchConfigurationName
	 * @param securityGroups
	 * @param keyPair
	 * @param instanceType
	 * @param imageId
	 * @param bootScriptFile
	 * @param detailedMonitoring
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public CreateLaunchConfiguration(AWSCredentials awsCredentials,
			String region, String launchConfigurationName,
			List<String> securityGroups, String keyPair, String instanceType,
			String imageId, File bootScriptFile, boolean detailedMonitoring)
			throws FileNotFoundException, IOException {
		this(awsCredentials, region, launchConfigurationName, securityGroups,
				keyPair, instanceType, imageId, IoUtils.toString(
						new FileInputStream(bootScriptFile), Charsets.UTF_8),
				detailedMonitoring);
	}

	@Override
	public Void call() {
		CreateLaunchConfigurationRequest request = new CreateLaunchConfigurationRequest()
				.withLaunchConfigurationName(this.launchConfigurationName)
				.withImageId(this.imageId)
				.withInstanceType(this.instanceType)
				.withUserData(AmazonApiUtils.base64Encode(this.bootScript))
				.withSecurityGroups(this.securityGroups)
				.withKeyName(this.keyPair)
				.withInstanceMonitoring(
						new InstanceMonitoring()
								.withEnabled(this.detailedMonitoring));
		getClient().getApi().createLaunchConfiguration(request);
		return null;
	}

}
