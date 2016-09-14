package com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.lang.String.format;

import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;

/**
 * A {@link Callable} task that, when executed, requests details about a
 * particular AWS Auto Scaling Launch Configuration in a region.
 */
public class GetLaunchConfiguration extends AmazonAutoScalingRequest<LaunchConfiguration> {

    /** The name of the {@link LaunchConfiguration} of interest. */
    private final String launchConfigurationName;

    /**
     * Constructs a new {@link GetLaunchConfiguration} task.
     *
     * @param awsCredentials
     * @param region
     * @param clientConfig
     *            Client configuration options such as connection timeout, etc.
     * @param launchConfigurationName
     */
    public GetLaunchConfiguration(AWSCredentials awsCredentials, String region, ClientConfiguration clientConfig,
            String launchConfigurationName) {
        super(awsCredentials, region, clientConfig);
        this.launchConfigurationName = launchConfigurationName;
    }

    @Override
    public LaunchConfiguration call() {
        DescribeLaunchConfigurationsRequest request = new DescribeLaunchConfigurationsRequest()
                .withLaunchConfigurationNames(this.launchConfigurationName);
        DescribeLaunchConfigurationsResult result = getClient().getApi().describeLaunchConfigurations(request);
        List<LaunchConfiguration> launchConfigurations = result.getLaunchConfigurations();
        if (launchConfigurations.isEmpty()) {
            throw new IllegalArgumentException(format("Launch Configuration '%s' doesn't exist in region '%s'.",
                    this.launchConfigurationName, getClient().getRegion()));
        }
        return getOnlyElement(launchConfigurations);
    }

}
