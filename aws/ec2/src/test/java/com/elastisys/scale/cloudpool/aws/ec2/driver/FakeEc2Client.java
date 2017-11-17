package com.elastisys.scale.cloudpool.aws.ec2.driver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2Client;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2ProvisioningTemplate;

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
    public void configure(String awsAccessKeyId, String awsSecretAccessKey, String region,
            ClientConfiguration clientConfig) {
    }

    @Override
    public List<Instance> getInstances(List<Filter> filters) throws AmazonClientException {
        return new ArrayList<>(this.instances);
    }

    @Override
    public Instance getInstanceMetadata(String instanceId) throws NotFoundException, AmazonClientException {
        for (Instance instance : this.instances) {
            if (instanceId.equals(instance.getInstanceId())) {
                return instance;
            }
        }
        throw new NotFoundException(String.format("no instance with id %s exists", instanceId));
    }

    @Override
    public List<Instance> launchInstances(Ec2ProvisioningTemplate provisioningDetails, int count)
            throws AmazonClientException {
        List<Instance> launchedInstances = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int idNum = ++this.idSequencer;
            Instance newInstance = new Instance().withInstanceId("i-" + idNum)
                    .withState(new InstanceState().withName("pending")).withPublicIpAddress("1.2.3." + idNum)
                    .withImageId(provisioningDetails.getAmiId())
                    .withInstanceType(provisioningDetails.getInstanceType());
            for (Entry<String, String> tagItem : provisioningDetails.getTags().entrySet()) {
                newInstance.withTags(new Tag(tagItem.getKey(), tagItem.getValue()));
            }
            this.instances.add(newInstance);
            launchedInstances.add(newInstance);
        }
        return launchedInstances;
    }

    @Override
    public void tagResource(String resourceId, List<Tag> tags) throws AmazonClientException {
        try {
            Instance instance = getInstanceMetadata(resourceId);
            replaceTags(instance, tags);
        } catch (NotFoundException e) {
            // amazon will throw an internal error
            throw new AmazonServiceException("no such resource", e);
        }
    }

    private void replaceTags(Instance instance, List<Tag> tags) {
        List<Tag> filteredTags = new ArrayList<>(instance.getTags());

        // first remove any old occurrences of updated tags
        Iterator<Tag> iterator = filteredTags.iterator();
        while (iterator.hasNext()) {
            Tag instanceTag = iterator.next();
            for (Tag updatedTag : tags) {
                if (instanceTag.getKey().equals(updatedTag.getKey())) {
                    iterator.remove();
                }
            }
        }
        instance.setTags(filteredTags);

        // ... then add updated tags
        instance.getTags().addAll(tags);
    }

    @Override
    public void untagResource(String resourceId, List<Tag> tags) throws AmazonClientException {
        try {
            Instance instance = getInstanceMetadata(resourceId);
            instance.getTags().removeAll(tags);
        } catch (NotFoundException e) {
            // amazon will throw an internal error
            throw new AmazonServiceException("no such resource", e);
        }
    }

    @Override
    public TerminateInstancesResult terminateInstances(List<String> instanceIds) throws AmazonClientException {
        TerminateInstancesResult result = new TerminateInstancesResult();
        for (String instanceId : instanceIds) {
            Instance instance = getInstanceMetadata(instanceId);
            this.instances.remove(instance);
            InstanceStateChange instanceStateChange = new InstanceStateChange().withInstanceId(instance.getInstanceId())
                    .withCurrentState(new InstanceState().withName(InstanceStateName.ShuttingDown));
            result.withTerminatingInstances(instanceStateChange);
        }
        return result;
    }

}
