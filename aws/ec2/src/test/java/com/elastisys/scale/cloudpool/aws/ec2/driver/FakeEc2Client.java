package com.elastisys.scale.cloudpool.aws.ec2.driver;

import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2Client;
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
	public void configure(String awsAccessKeyId, String awsSecretAccessKey,
			String region) {
	}

	@Override
	public List<Instance> getInstances(List<Filter> filters)
			throws AmazonClientException {
		return Lists.newArrayList(this.instances);
	}

	@Override
	public Instance getInstanceMetadata(String instanceId)
			throws NotFoundException, AmazonClientException {
		for (Instance instance : this.instances) {
			if (instanceId.equals(instance.getInstanceId())) {
				return instance;
			}
		}
		throw new NotFoundException(String.format(
				"no instance with id %s exists", instanceId));
	}

	@Override
	public Instance launchInstance(ScaleOutConfig provisioningDetails)
			throws AmazonClientException {
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
	public void tagResource(String resourceId, List<Tag> tags)
			throws AmazonClientException {
		try {
			Instance instance = getInstanceMetadata(resourceId);
			instance.withTags(tags);
		} catch (NotFoundException e) {
			// amazon will throw an internal error
			throw new AmazonServiceException("no such resource", e);
		}
	}

	@Override
	public void untagResource(String resourceId, List<Tag> tags)
			throws AmazonClientException {
		try {
			Instance instance = getInstanceMetadata(resourceId);
			instance.getTags().removeAll(tags);
		} catch (NotFoundException e) {
			// amazon will throw an internal error
			throw new AmazonServiceException("no such resource", e);
		}
	}

	@Override
	public void terminateInstance(String instanceId) throws NotFoundException,
			AmazonClientException {
		Instance instance = getInstanceMetadata(instanceId);
		this.instances.remove(instance);
	}

}
