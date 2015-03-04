package com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling;

import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;

/**
 * A {@link Callable} task that, when executed, requests the deletion of a AWS
 * Launch Configuration.
 */
public class DeleteLaunchConfiguration extends AmazonAutoScalingRequest<Void> {

	/** The launch configuration to delete. */
	private final String launchConfigurationName;

	public DeleteLaunchConfiguration(AWSCredentials awsCredentials,
			String region, String launchConfigurationName) {
		super(awsCredentials, region);
		this.launchConfigurationName = launchConfigurationName;
	}

	@Override
	public Void call() {
		DeleteLaunchConfigurationRequest request = new DeleteLaunchConfigurationRequest()
				.withLaunchConfigurationName(this.launchConfigurationName);
		getClient().getApi().deleteLaunchConfiguration(request);
		return null;
	}
}
