package com.elastisys.scale.cloudpool.aws.commons.client.lab;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.elastisys.scale.cloudpool.aws.commons.requests.autoscaling.SetDesiredAutoScalingGroupSize;

/**
 * Changes the size of an auto-scaling group.
 * <p/>
 * In case of shrinking the size, the termination victim selection is carried
 * out like <a href=
 * "http://aws.amazon.com/ec2/faqs/#How_does_Auto_Scaling_decide_which_Amazon_EC2_instance"
 * >this</a>. Instances close to the end of the billing-hour are chosen first,
 * but nothing is done to keep them as long as possible.
 * 
 * 
 * 
 */
public class ResizeAutoScalingGroupMain extends AbstractClient {

	// TODO: set to region where auto-scaling group is hosted
	private static final String region = "us-east-1";
	// TODO: set to the name the auto-scaling group
	private static final String autoScalingGroup = "end2end-scalinggroup";
	// TODO: set to the desired capacity of the auto-scaling group
	private static final int desiredCapacity = 1;

	public static void main(String[] args) throws Exception {
		logger.info("Setting desired capacity for Auto Scaling group {} to {}",
				autoScalingGroup, desiredCapacity);
		new SetDesiredAutoScalingGroupSize(new PropertiesCredentials(
				credentialsFile), region, autoScalingGroup, desiredCapacity)
				.call();
		logger.info("Group " + autoScalingGroup + " reached size "
				+ desiredCapacity);

	}

	/**
	 * Updates the Auto Scaling group size by (re-)setting MinSize, MaxSize and
	 * DesiredCapacity to the desired capacity.
	 * 
	 * @param api
	 * @param newCapacity
	 * @return
	 */
	private static UpdateAutoScalingGroupRequest updateGroupSize(
			AmazonAutoScalingClient api, int newCapacity) {
		logger.info("Updating auto-scaling group to capacity: " + newCapacity);
		UpdateAutoScalingGroupRequest request = new UpdateAutoScalingGroupRequest()
				.withAutoScalingGroupName(autoScalingGroup)
				.withMinSize(newCapacity).withMaxSize(newCapacity)
				.withDesiredCapacity(newCapacity);
		api.updateAutoScalingGroup(request);
		return request;
	}

}
