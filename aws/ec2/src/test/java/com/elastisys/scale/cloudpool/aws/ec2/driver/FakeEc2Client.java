package com.elastisys.scale.cloudpool.aws.ec2.driver;

import java.util.List;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.aws.ec2.driver.Ec2PoolDriverConfig;
import com.elastisys.scale.cloudpool.aws.ec2.driver.client.Ec2Client;
import com.elastisys.scale.cloudpool.commons.basepool.BaseCloudPoolConfig.ScaleOutConfig;
import com.google.common.collect.Lists;

/**
 * Fake {@link Ec2Client} that manages instances for a phony AWS account.
 */
public class FakeEc2Client implements Ec2Client {
	private int idSequencer = 0;

	private List<Instance> instances;

	public FakeEc2Client(List<Instance> instances) {
		this.instances = instances;
		this.idSequencer = this.instances.size();
	}

	@Override
	public void configure(Ec2PoolDriverConfig configuration) {
	}

	@Override
	public List<Instance> getInstances(List<Filter> filters) {
		return Lists.newArrayList(this.instances);
	}

	@Override
	public Instance getInstanceMetadata(String instanceId)
			throws NotFoundException {
		for (Instance instance : this.instances) {
			if (instanceId.equals(instance.getInstanceId())) {
				return instance;
			}
		}
		throw new NotFoundException(String.format(
				"no instance with id %s exists", instanceId));
	}

	@Override
	public Instance launchInstance(ScaleOutConfig provisioningDetails) {
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
	public void tagInstance(String instanceId, List<Tag> tags)
			throws NotFoundException {
		Instance instance = getInstanceMetadata(instanceId);
		instance.withTags(tags);
	}

	@Override
	public void untagInstance(String instanceId, List<Tag> tags)
			throws NotFoundException {
		Instance instance = getInstanceMetadata(instanceId);
		instance.getTags().removeAll(tags);
	}

	@Override
	public void terminateInstance(String instanceId) throws NotFoundException {
		Instance instance = getInstanceMetadata(instanceId);
		this.instances.remove(instance);
	}

}
