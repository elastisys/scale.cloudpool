package com.elastisys.scale.cloudpool.aws.autoscaling.driver.client;

import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.AwsAsPoolDriver;
import com.elastisys.scale.cloudpool.aws.autoscaling.driver.config.CloudApiSettings;

/**
 * A simplified client interface towards the AWS Auto Scaling API that only
 * provides the functionality needed by the {@link AwsAsPoolDriver}.
 * <p/>
 * The {@link #configure} method must be called before calling any other
 * methods.
 *
 *
 *
 */
public interface AutoScalingClient {

    /**
     * Configures this {@link AutoScalingClient} with API access credentials and
     * settings to allow it to access the AWS Auto Scaling API.
     *
     * @param configuration
     *            API access credentials and settings.
     */
    void configure(CloudApiSettings configuration);

    /**
     * Retrieves a particular {@link AutoScalingGroup}.
     *
     * @param autoScalingGroupName
     *            The name of the Auto Scaling Group.
     * @return
     * @throws AmazonClientException
     */
    AutoScalingGroup getAutoScalingGroup(String autoScalingGroupName) throws AmazonClientException;

    /**
     * Retrieves a particular {@link LaunchConfiguration} (describing how to
     * launch on-demand or spot instances for an Auto Scaling Group).
     *
     * @param launchConfigurationName
     *            The name of the Launch configuration.
     * @return
     * @throws AmazonClientException
     */
    LaunchConfiguration getLaunchConfiguration(String launchConfigurationName) throws AmazonClientException;

    /**
     * Retrieves all members of a particular {@link AutoScalingGroup}.
     *
     * @param autoScalingGroupName
     *            The name of the Auto Scaling Group.
     * @return
     * @throws AmazonClientException
     */
    List<Instance> getAutoScalingGroupMembers(String autoScalingGroupName) throws AmazonClientException;

    /**
     * Sets the desired capacity of a particular Auto Scaling Group.
     * <p/>
     * Note that this method will not block until the group has reached the
     * desired size, which may be a time-consuming process.
     *
     * @param autoScalingGroupName
     *            The name of the Auto Scaling Group.
     * @param desiredSize
     *            The desired capacity of the group to set.
     * @throws AmazonClientException
     */
    void setDesiredSize(String autoScalingGroupName, int desiredSize) throws AmazonClientException;

    /**
     * Terminates a particular Auto Scaling Group member instance. As a
     * side-effect, the desired capacity of the Auto Scaling Group is
     * decremented.
     *
     * @param autoScalingGroupName
     *            The name of the Auto Scaling Group.
     * @param instanceId
     *            An instance identifier of a member in the Auto Scaling Group.
     * @throws NotFoundException
     *             if the instance does not exist
     * @throws AmazonClientException
     */
    void terminateInstance(String autoScalingGroupName, String instanceId)
            throws NotFoundException, AmazonClientException;

    /**
     * Adds a machine instance to the scaling group. As a side-effect, the
     * desired capacity of the Auto Scaling Group is incremented.
     *
     * @param machineId
     *            The identifier of the instance to attach to the pool.
     * @throws NotFoundException
     *             if the instance does not exist
     * @throws AmazonClientException
     */
    void attachInstance(String autoScalingGroupName, String instanceId) throws NotFoundException, AmazonClientException;

    /**
     * Removes a member from the scaling group without terminating it. As a
     * side-effect, the desired capacity of the Auto Scaling Group is
     * decremented.
     *
     * @param machineId
     *            The identifier of the instance to detach from the pool.
     * @throws NotFoundException
     *             if the instance does not exist
     * @throws AmazonClientException
     */
    void detachInstance(String autoScalingGroupName, String instanceId) throws NotFoundException, AmazonClientException;

    /**
     * Sets a collection of tags on an EC2 instance.
     *
     * @param instanceId
     *            An instance identifier.
     * @param tags
     *            The {@link Tag}s to set on the {@link Instance}.
     * @throws NotFoundException
     *             if the instance does not exist
     * @throws AmazonClientException
     */
    void tagInstance(String instanceId, List<Tag> tags) throws NotFoundException, AmazonClientException;

}
