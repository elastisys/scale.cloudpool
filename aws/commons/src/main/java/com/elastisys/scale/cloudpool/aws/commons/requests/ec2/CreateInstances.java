package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.aws.commons.functions.AwsEc2Functions;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

/**
 * A {@link Callable} task that, when executed, requests a number of AWS EC2
 * instances to be created, (optionally) tags them, and waits for the
 * {@link SpotInstanceRequest}s to appear in
 * {@code DescribeSpotInstanceRequests}, which may not be immediate due to the
 * <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/query-api-troubleshooting.html#eventual-consistency"
 * >eventual consistency semantics</a> of the Amazon API. and waits for the
 * instance to appear started in {@code DescribeInstances}.
 */
public class CreateInstances extends AmazonEc2Request<List<Instance>> {

    /** Initial exponential back-off delay in ms. */
    private static final int INITIAL_BACKOFF_DELAY = 1000;
    /** Maximum number of retries of operations. */
    private static final int MAX_RETRIES = 8;

    /** The availability zone (within the region) to launch machine in. */
    private final String availabilityZone;

    /** The AWS security group(s) to use for the created instance. */
    private final List<String> securityGroups;
    /** The EC2 key pair to use for the created instance. */
    private final String keyPair;

    /** The EC2 instance type to use for the created instance. */
    private final String instanceType;
    /** The AMI (amazon machine image) id to use for the created instance. */
    private final String imageId;

    /**
     * The base64-encoded <a href=
     * "http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html">
     * user data</a> boot to pass to the created instance.
     */
    private final String encodedUserData;

    /** The number of spot instances to request. */
    private final int count;

    /**
     * The (possibly empty) set of {@link Tag}s to attach to the placed spot
     * instance requests.
     */
    private final List<Tag> tags;

    public CreateInstances(AWSCredentials awsCredentials, String region, ClientConfiguration clientConfig,
            String availabilityZone, List<String> securityGroups, String keyPair, String instanceType, String imageId,
            String encodedUserData, int count, List<Tag> tags) {
        super(awsCredentials, region, clientConfig);
        this.availabilityZone = availabilityZone;
        this.securityGroups = securityGroups;
        this.keyPair = keyPair;
        this.instanceType = instanceType;
        this.imageId = imageId;
        this.encodedUserData = encodedUserData;
        this.count = count;
        this.tags = tags;
    }

    @Override
    public List<Instance> call() {
        Placement placement = new Placement().withAvailabilityZone(this.availabilityZone);
        RunInstancesRequest request = new RunInstancesRequest().withMinCount(this.count).withMaxCount(this.count)
                .withImageId(this.imageId).withInstanceType(this.instanceType).withUserData(this.encodedUserData)
                .withSecurityGroupIds(this.securityGroups).withKeyName(this.keyPair).withPlacement(placement);
        RunInstancesResult result = getClient().getApi().runInstances(request);
        List<Instance> launchedInstances = result.getReservation().getInstances();

        List<String> instanceIds = Lists.transform(launchedInstances, AwsEc2Functions.toInstanceId());

        if (!this.tags.isEmpty()) {
            tagRequests(instanceIds);
        }

        return awaitInstances(instanceIds);
    }

    /**
     * Tags each instance with the set of {@link Tag}s that were passed to this
     * {@link CreateInstances} task on creation.
     *
     * @param instanceIds
     */
    private void tagRequests(List<String> instanceIds) {
        Callable<Void> requester = new TagEc2Resources(getAwsCredentials(), getRegion(), getClientConfig(), instanceIds,
                this.tags);
        String tagTaskName = String.format("tag{%s}", instanceIds);
        Retryable<Void> retryable = Retryers.exponentialBackoffRetryer(tagTaskName, requester, INITIAL_BACKOFF_DELAY,
                TimeUnit.MILLISECONDS, MAX_RETRIES);
        try {
            retryable.call();
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("gave up trying to tag instances %s: %s", instanceIds, e.getMessage()), e);
        }
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
        return input -> {
            List<String> inputIds = Lists.transform(input, AwsEc2Functions.toInstanceId());
            return inputIds.containsAll(expectedInstanceIds);
        };
    }
}
