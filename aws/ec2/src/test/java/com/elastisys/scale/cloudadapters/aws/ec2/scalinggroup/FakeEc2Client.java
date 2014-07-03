package com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup;

import java.util.List;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudadapters.aws.ec2.scalinggroup.client.Ec2Client;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapterConfig.ScaleUpConfig;
import com.google.common.collect.Lists;

/**
 * Fake {@link Ec2Client} that manages instances for a phony AWS account.
 *
 * 
 *
 */
public class FakeEc2Client implements Ec2Client {
	private int idSequencer = 0;

	private List<Instance> instances;

	public FakeEc2Client(List<Instance> instances) {
		this.instances = instances;
		this.idSequencer = this.instances.size();
	}

	@Override
	public void configure(Ec2ScalingGroupConfig configuration) {
	}

	@Override
	public List<Instance> getInstances(List<Filter> filters) {
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
				"no instance with id %s exists", instanceId));
	}

	@Override
	public Instance launchInstance(ScaleUpConfig provisioningDetails) {
		int idNum = ++this.idSequencer;
		Instance launchedInstance = new Instance().withInstanceId("i-" + idNum)
				.withState(new InstanceState().withName("pending"))
				.withPublicIpAddress("1.2.3." + idNum)
				.withImageId(provisioningDetails.getImage())
				.withInstanceType(provisioningDetails.getSize());
		this.instances.add(launchedInstance);
		return launchedInstance;
	}

	@Override
	public void tagInstance(String instanceId, List<Tag> tags) {
		Instance instance = getInstanceMetadata(instanceId);
		instance.withTags(tags);
	}

	@Override
	public void terminateInstance(String instanceId) {
		Instance instance = getInstanceMetadata(instanceId);
		this.instances.remove(instance);
	}

}
