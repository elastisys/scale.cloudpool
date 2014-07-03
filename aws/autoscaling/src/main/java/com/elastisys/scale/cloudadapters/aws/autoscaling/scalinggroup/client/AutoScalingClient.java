package com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.client;

import java.util.List;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.ec2.model.Instance;
import com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.AwsAsScalingGroup;
import com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.AwsAsScalingGroupConfig;

/**
 * A simplified client interface towards the AWS Auto Scaling API that only
 * provides the functionality needed by the {@link AwsAsScalingGroup}.
 * <p/>
 * The {@link #configure} method must be called before calling any other
 * methods.
 *
 * 
 *
 */
public interface AutoScalingClient {

	/**
	 * Configures this {@link AutoScalingClient} with credentials to allow it to
	 * access the AWS Auto Scaling API.
	 *
	 * @param configuration
	 *            A client configuration.
	 */
	void configure(AwsAsScalingGroupConfig configuration);

	/**
	 * Retrieves a particular {@link AutoScalingGroup}.
	 *
	 * @param autoScalingGroupName
	 *            The name of the Auto Scaling Group.
	 * @return
	 */
	AutoScalingGroup getAutoScalingGroup(String autoScalingGroupName);

	/**
	 * Retrieves all members of a particular {@link AutoScalingGroup}.
	 *
	 * @param autoScalingGroupName
	 *            The name of the Auto Scaling Group.
	 * @return
	 */
	List<Instance> getAutoScalingGroupMembers(String autoScalingGroupName);

	/**
	 * Retrieves instance meta data about a particular EC2 {@link Instance}.
	 *
	 * @param instanceId
	 *            An instance identifier.
	 * @return
	 */
	Instance getInstanceMetadata(String instanceId);

	/**
	 * Sets the desired capacity of a particular Auto Scaling Group.
	 *
	 * @param autoScalingGroupName
	 *            The name of the Auto Scaling Group.
	 * @param desiredSize
	 *            The desired capacity of the group to set.
	 */
	void setDesiredSize(String autoScalingGroupName, int desiredSize);

	/**
	 * Terminates a particular Auto Scaling Group member instance.
	 *
	 * @param autoScalingGroupName
	 *            The name of the Auto Scaling Group.
	 * @param instanceId
	 *            An instance identifier of a member in the Auto Scaling Group.
	 */
	void terminateInstance(String autoScalingGroupName, String instanceId);
}
