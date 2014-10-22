package com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup;

import static com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.TestUtils.group;

import java.util.List;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.client.AutoScalingClient;
import com.google.common.collect.Lists;

/**
 * Fake {@link AutoScalingClient} that manages an Auto Scaling Group for a phony
 * AWS account.
 *
 *
 *
 */
public class FakeAutoScalingClient implements AutoScalingClient {
	protected int idSequencer = 0;

	protected String autoScalingGroupName;
	protected int desiredCapacity;
	protected List<Instance> instances;

	public FakeAutoScalingClient(String autoScalingGroupName,
			int desiredCapacity, List<Instance> instances) {
		this.autoScalingGroupName = autoScalingGroupName;
		this.desiredCapacity = desiredCapacity;
		this.instances = instances;

		this.idSequencer = this.instances.size();
	}

	@Override
	public void configure(AwsAsScalingGroupConfig configuration) {
	}

	@Override
	public AutoScalingGroup getAutoScalingGroup(String autoScalingGroupName) {
		return group(autoScalingGroupName, this.desiredCapacity, this.instances);
	}

	@Override
	public List<Instance> getAutoScalingGroupMembers(String autoScalingGroupName) {
		return Lists.newArrayList(this.instances);
	}

	@Override
	public Instance getInstanceMetadata(String instanceId) {
		for (Instance instance : this.instances) {
			if (instanceId.equals(instance.getInstanceId())) {
				return instance;
			}
		}
		throw new IllegalArgumentException(String.format(
				"no instance with id %s in Auto Scaling Group", instanceId));
	}

	@Override
	public void setDesiredSize(String autoScalingGroupName, int desiredSize) {
		int delta = desiredSize - this.desiredCapacity;
		this.desiredCapacity = desiredSize;
		if (delta > 0) {
			// add instance(s) to group
			for (int i = 0; i < delta; i++) {
				int idNum = ++this.idSequencer;
				this.instances.add(new Instance().withInstanceId("i-" + idNum)
						.withState(new InstanceState().withName("pending"))
						.withPublicIpAddress("1.2.3." + idNum));
			}
		} else if (delta < 0) {
			// remove instance(s) from group
			int toTerminate = -delta;
			for (int i = 0; i < toTerminate; i++) {
				terminateInstance(autoScalingGroupName, this.instances.get(0)
						.getInstanceId());
			}
		}
	}

	@Override
	public void terminateInstance(String autoScalingGroupName, String instanceId) {
		Instance instance = getInstanceMetadata(instanceId);
		if (this.instances.remove(instance)) {
			this.desiredCapacity--;
		}
	}
}
