package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceSpecification;
import com.amazonaws.services.ec2.model.ResourceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2ProvisioningTemplate;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * A {@link Callable} task that, when executed, requests a number of AWS EC2
 * instances to be created, (optionally) tags them, and waits for the
 * {@link Instance}s to appear in the API, which may not be immediate due to the
 * <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/query-api-troubleshooting.html#eventual-consistency"
 * >eventual consistency semantics</a> of the Amazon API.
 */
public class CreateInstances extends AmazonEc2Request<List<Instance>> {
    /** Initial exponential back-off delay in ms. */
    private static final int INITIAL_BACKOFF_DELAY = 1000;
    /** Maximum number of retries of operations. */
    private static final int MAX_RETRIES = 8;

    /** The provisioning template used to describe the instances to create. */
    private final Ec2ProvisioningTemplate instanceTemplate;

    /** The number of instances to request. */
    private final int count;

    /** Random generator to spread instances across subnets. */
    private final Random random = new Random(UtcTime.now().getMillis());

    public CreateInstances(AWSCredentials awsCredentials, String region, ClientConfiguration clientConfig,
            Ec2ProvisioningTemplate instanceTemplate, int count) {
        super(awsCredentials, region, clientConfig);
        this.instanceTemplate = instanceTemplate;
        this.count = count;

    }

    @Override
    public List<Instance> call() {
        RunInstancesRequest request = new RunInstancesRequest();
        request.withInstanceType(this.instanceTemplate.getInstanceType());
        request.withImageId(this.instanceTemplate.getAmiId());

        InstanceNetworkInterfaceSpecification nic = new InstanceNetworkInterfaceSpecification();
        nic.withDeviceIndex(0);
        // select a subnet at random
        nic.withSubnetId(randomSubnet());
        nic.withAssociatePublicIpAddress(this.instanceTemplate.isAssignPublicIp());
        nic.withGroups(this.instanceTemplate.getSecurityGroupIds());
        request.withNetworkInterfaces(nic);

        request.withKeyName(this.instanceTemplate.getKeyPair());
        request.withIamInstanceProfile(
                new IamInstanceProfileSpecification().withArn(this.instanceTemplate.getIamInstanceProfileARN()));
        request.withUserData(this.instanceTemplate.getEncodedUserData());
        request.withEbsOptimized(this.instanceTemplate.isEbsOptimized());
        request.withMinCount(this.count).withMaxCount(this.count);
        if (!this.instanceTemplate.getTags().isEmpty()) {
            TagSpecification tagSpecifications = new TagSpecification().withResourceType(ResourceType.Instance);
            tagSpecifications.withTags(tags());
            request.withTagSpecifications(tagSpecifications);
        }

        RunInstancesResult result = getClient().getApi().runInstances(request);
        List<Instance> launchedInstances = result.getReservation().getInstances();
        List<String> instanceIds = launchedInstances.stream().map(Instance::getInstanceId).collect(Collectors.toList());

        return awaitInstances(instanceIds);
    }

    private Collection<Tag> tags() {
        List<Tag> tags = new ArrayList<>();
        for (Entry<String, String> tag : this.instanceTemplate.getTags().entrySet()) {
            tags.add(new Tag(tag.getKey(), tag.getValue()));
        }
        return tags;
    }

    private String randomSubnet() {
        int nextSubnet = this.random.nextInt(this.instanceTemplate.getSubnetIds().size());
        return this.instanceTemplate.getSubnetIds().get(nextSubnet);
    }

    /**
     * Waits for all placed spot requests to become visible in the API.
     *
     * @param createdInstanceIds
     * @return
     */
    private List<Instance> awaitInstances(List<String> createdInstanceIds) {
        String name = String.format("await-instances{%s}", createdInstanceIds);

        // wait for created instances to be seen in API when listing *all*
        // instances (due to eventual consistency, EC2 will return cached data,
        // which means that the created instances may not be visible
        // immediately)
        Callable<List<Instance>> requester = new GetInstances(getAwsCredentials(), getRegion(), getClientConfig(),
                null);
        Retryable<List<Instance>> retryer = Retryers.exponentialBackoffRetryer(name, requester, INITIAL_BACKOFF_DELAY,
                TimeUnit.MILLISECONDS, MAX_RETRIES, contains(createdInstanceIds));

        try {
            // only return those instances that we actually created
            List<Instance> allInstances = retryer.call();
            return allInstances.stream().filter(i -> createdInstanceIds.contains(i.getInstanceId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(String.format("gave up waiting for instances to become active %s: %s",
                    createdInstanceIds, e.getMessage()), e);
        }
    }

    /**
     * A predicate that returns <code>true</code> for any collection of input
     * {@link Instance}s that contain an expected collection of identifiers.
     *
     * @param expectedInstanceIds
     * @return
     */
    private static Predicate<List<Instance>> contains(final List<String> expectedInstanceIds) {
        return instances -> {
            List<String> instanceIds = instances.stream().map(Instance::getInstanceId).collect(Collectors.toList());
            return instanceIds.containsAll(expectedInstanceIds);
        };
    }
}
