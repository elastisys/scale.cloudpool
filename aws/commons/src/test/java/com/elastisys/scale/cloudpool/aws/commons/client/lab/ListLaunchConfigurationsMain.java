package com.elastisys.scale.cloudpool.aws.commons.client.lab;

import java.util.List;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.GetLaunchConfigurations;

public class ListLaunchConfigurationsMain extends AbstractClient {

	// TODO: set to region where launch configurations are hosted
	private static final String region = "us-east-1";

	public static void main(String[] args) throws Exception {
		List<LaunchConfiguration> launchConfigurations = new GetLaunchConfigurations(
				new PropertiesCredentials(credentialsFile), region).call();
		for (LaunchConfiguration launchConfiguration : launchConfigurations) {
			logger.info("launch configuration: " + launchConfiguration);
		}
	}

}
