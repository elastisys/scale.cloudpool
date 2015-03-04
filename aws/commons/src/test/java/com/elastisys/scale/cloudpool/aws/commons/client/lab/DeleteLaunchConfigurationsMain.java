package com.elastisys.scale.cloudpool.aws.commons.client.lab;

import java.util.List;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.DeleteLaunchConfiguration;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.GetLaunchConfigurations;

public class DeleteLaunchConfigurationsMain extends AbstractClient {

	// TODO: set to region where launch configurations are hosted
	private static final String region = "us-east-1";

	public static void main(String[] args) throws Exception {
		logger.info("deleting launch configurations in region " + region);

		PropertiesCredentials awsCredentials = new PropertiesCredentials(
				credentialsFile);
		List<LaunchConfiguration> launchConfigurations = new GetLaunchConfigurations(
				awsCredentials, region).call();
		for (LaunchConfiguration launchConfiguration : launchConfigurations) {
			logger.info("deleting launch configuration: " + launchConfiguration);
			new DeleteLaunchConfiguration(awsCredentials, region,
					launchConfiguration.getLaunchConfigurationName()).call();
		}
	}
}
