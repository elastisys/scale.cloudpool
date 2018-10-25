package com.elastisys.scale.cloudpool.aws.autoscaling.driver.client;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.config.CloudApiSettings;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.AttachAutoScalingGroupInstance;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.DetachAutoScalingGroupInstance;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.GetAutoScalingGroup;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.GetAutoScalingGroupInstances;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.GetLaunchConfiguration;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.SetDesiredAutoScalingGroupSize;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.TerminateAutoScalingGroupInstance;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.GetInstance;
import com.elastisys.scale.cloudpool.aws.commons.requests.ec2.TagEc2Resources;

/**
 * Standard implementation of the {@link AutoScalingClient} interface.
 *
 *
 *
 */
public class AwsAutoScalingClient implements AutoScalingClient {
    private CloudApiSettings config;

    public AwsAutoScalingClient() {
        this.config = null;
    }

    @Override
    public void configure(CloudApiSettings configuration) {
        checkArgument(configuration != null, "null configuration");
        this.config = configuration;
    }

    @Override
    public AutoScalingGroup getAutoScalingGroup(String autoScalingGroupName) throws AmazonClientException {
        checkArgument(isConfigured(), "can't use client before it's configured");

        return new GetAutoScalingGroup(awsCredentials(), region(), clientConfig(), autoScalingGroupName).call();
    }

    @Override
    public LaunchConfiguration getLaunchConfiguration(String launchConfigurationName) throws AmazonClientException {
        checkArgument(isConfigured(), "can't use client before it's configured");

        return new GetLaunchConfiguration(awsCredentials(), region(), clientConfig(), launchConfigurationName).call();
    }

    @Override
    public List<Instance> getAutoScalingGroupMembers(String autoScalingGroupName) throws AmazonClientException {
        checkArgument(isConfigured(), "can't use client before it's configured");

        return new GetAutoScalingGroupInstances(awsCredentials(), region(), clientConfig(), autoScalingGroupName)
                .call();
    }

    @Override
    public void setDesiredSize(String autoScalingGroupName, int desiredSize) throws AmazonClientException {
        checkArgument(isConfigured(), "can't use client before it's configured");

        new SetDesiredAutoScalingGroupSize(awsCredentials(), region(), clientConfig(), autoScalingGroupName,
                desiredSize).call();
    }

    @Override
    public void terminateInstance(String autoScalingGroupName, String instanceId)
            throws NotFoundException, AmazonClientException {
        checkArgument(isConfigured(), "can't use client before it's configured");

        // verify that instance exists
        getInstanceOrFail(instanceId);

        new TerminateAutoScalingGroupInstance(awsCredentials(), region(), clientConfig(), instanceId).call();
    }

    @Override
    public void attachInstance(String autoScalingGroupName, String instanceId)
            throws NotFoundException, AmazonClientException {
        checkArgument(isConfigured(), "can't use client before it's configured");

        // verify that instance exists
        getInstanceOrFail(instanceId);

        new AttachAutoScalingGroupInstance(awsCredentials(), region(), clientConfig(), autoScalingGroupName, instanceId)
                .call();
    }

    @Override
    public void detachInstance(String autoScalingGroupName, String instanceId)
            throws NotFoundException, AmazonClientException {
        checkArgument(isConfigured(), "can't use client before it's configured");

        // verify that instance exists
        getInstanceOrFail(instanceId);

        new DetachAutoScalingGroupInstance(awsCredentials(), region(), clientConfig(), autoScalingGroupName, instanceId)
                .call();
    }

    @Override
    public void tagInstance(String instanceId, List<Tag> tags) throws NotFoundException, AmazonClientException {
        checkArgument(isConfigured(), "can't use client before it's configured");

        // verify that instance exists
        getInstanceOrFail(instanceId);

        new TagEc2Resources(awsCredentials(), region(), clientConfig(), Arrays.asList(instanceId), tags).call();
    }

    private Instance getInstanceOrFail(String instanceId) throws NotFoundException, AmazonClientException {
        checkArgument(isConfigured(), "can't use client before it's configured");

        return new GetInstance(awsCredentials(), region(), clientConfig(), instanceId).call();
    }

    private boolean isConfigured() {
        return this.config != null;
    }

    private CloudApiSettings config() {
        return this.config;
    }

    /**
     * The AWS credentials that the client has been configured to use.
     *
     * @return
     */
    private AWSCredentials awsCredentials() {
        return new BasicAWSCredentials(config().getAwsAccessKeyId(), config().getAwsSecretAccessKey());
    }

    /**
     * The region that the client has been configured to operate against.
     *
     * @return
     */
    private String region() {
        return config().getRegion();
    }

    /**
     * Returns a {@link ClientConfiguration} that captures the connection
     * settings that the client has been configured to use.
     *
     * @return
     */
    private ClientConfiguration clientConfig() {
        return new ClientConfiguration().withConnectionTimeout(config().getConnectionTimeout())
                .withSocketTimeout(config().getSocketTimeout());
    }
}
