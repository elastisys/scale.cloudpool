package com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.google.common.base.Optional;

/**
 * A {@link Callable} task that, when executed, requests the listing of all (or
 * a particular collcetion of) AWS Launch Configurations in a region.
 */
public class GetLaunchConfigurations extends
		AmazonAutoScalingRequest<List<LaunchConfiguration>> {

	/**
	 * The (optional) collection of launch configuration to collect details
	 * about.
	 */
	private final Optional<List<String>> launchConfigurations;

	/**
	 * Constructs a new {@link GetLaunchConfigurations} task that fetches all
	 * launch configurations.
	 *
	 * @param awsCredentials
	 * @param region
	 */
	public GetLaunchConfigurations(AWSCredentials awsCredentials, String region) {
		this(awsCredentials, region, new ArrayList<String>(0));
	}

	/**
	 * Constructs a new {@link GetLaunchConfigurations} task that fetches a
	 * particular set of launch configurations.
	 *
	 * @param awsCredentials
	 * @param region
	 * @param launchConfigurationNames
	 *            The launch configurations to fetch. An empty list is
	 *            interpreted as 'all launch configurations'.
	 */
	public GetLaunchConfigurations(AWSCredentials awsCredentials,
			String region, List<String> launchConfigurationNames) {
		super(awsCredentials, region);

		if (launchConfigurationNames.isEmpty()) {
			this.launchConfigurations = Optional.absent();
		} else {
			this.launchConfigurations = Optional.of(launchConfigurationNames);
		}
	}

	@Override
	public List<LaunchConfiguration> call() {
		DescribeLaunchConfigurationsRequest request = new DescribeLaunchConfigurationsRequest();
		if (this.launchConfigurations.isPresent()) {
			request.withLaunchConfigurationNames(this.launchConfigurations
					.get());
		}
		DescribeLaunchConfigurationsResult result = getClient().getApi()
				.describeLaunchConfigurations(request);
		return result.getLaunchConfigurations();
	}
}
