package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.SpotInstanceType;
import com.amazonaws.services.ec2.model.SpotPlacement;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.aws.commons.functions.AwsEc2Functions;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

/**
 * A {@link Callable} task that, when executed, requests a number of AWS spot
 * instances, (optionally) tags them, and waits for the
 * {@link SpotInstanceRequest}s to appear in
 * {@code DescribeSpotInstanceRequests}, which may not be immediate due to the
 * <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/query-api-troubleshooting.html#eventual-consistency"
 * >eventual consistency semantics</a> of the Amazon API.
 */
public class PlaceSpotInstanceRequests extends AmazonEc2Request<List<SpotInstanceRequest>> {

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
    /** The bid price to set for the spot request. */
    private final String bidPrice;

    /** The number of spot instances to request. */
    private final int count;

    /**
     * The (possibly empty) set of {@link Tag}s to attach to the placed spot
     * instance requests.
     */
    private final List<Tag> tags;

    public PlaceSpotInstanceRequests(AWSCredentials awsCredentials, String region, ClientConfiguration clientConfig,
            double bidPrice, String availabilityZone, List<String> securityGroups, String keyPair, String instanceType,
            String imageId, String encodedUserData, int count, List<Tag> tags) {
        super(awsCredentials, region, clientConfig);
        this.bidPrice = String.valueOf(bidPrice);
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
    public List<SpotInstanceRequest> call() {
        SpotPlacement placement = new SpotPlacement().withAvailabilityZone(this.availabilityZone);
        LaunchSpecification launchSpec = new LaunchSpecification().withInstanceType(this.instanceType)
                .withImageId(this.imageId).withPlacement(placement).withSecurityGroups(this.securityGroups)
                .withKeyName(this.keyPair).withUserData(this.encodedUserData);
        RequestSpotInstancesRequest request = new RequestSpotInstancesRequest().withInstanceCount(this.count)
                .withType(SpotInstanceType.Persistent).withSpotPrice(this.bidPrice).withLaunchSpecification(launchSpec);
        RequestSpotInstancesResult result = getClient().getApi().requestSpotInstances(request);

        List<String> spotRequestIds = Lists.transform(result.getSpotInstanceRequests(),
                AwsEc2Functions.toSpotRequestId());

        if (!this.tags.isEmpty()) {
            tagRequests(spotRequestIds);
        }

        return awaitSpotRequests(spotRequestIds);
    }

    /**
     * Tags each spot request with the set of {@link Tag}s that were passed to
     * this {@link PlaceSpotInstanceRequests} task on creation.
     *
     * @param spotRequestIds
     */
    private void tagRequests(List<String> spotRequestIds) {
        Callable<Void> requester = new TagEc2Resources(getAwsCredentials(), getRegion(), getClientConfig(),
                spotRequestIds, this.tags);
        String tagTaskName = String.format("tag{%s}", spotRequestIds);
        Retryable<Void> retryable = Retryers.exponentialBackoffRetryer(tagTaskName, requester, INITIAL_BACKOFF_DELAY,
                TimeUnit.MILLISECONDS, MAX_RETRIES);
        try {
            retryable.call();
        } catch (Exception e) {
            throw new RuntimeException(String.format("gave up trying to tag spot instance requests %s: %s",
                    spotRequestIds, e.getMessage()), e);
        }
    }

    /**
     * Waits for all placed spot requests to become visible in the API.
     *
     * @param placedSpotRequestIds
     * @return
     */
    private List<SpotInstanceRequest> awaitSpotRequests(List<String> placedSpotRequestIds) {

        // wait for placed spot requests to be seen in API when listing *all*
        // spot requests (due to eventual consistency, EC2 will return cached
        // data, which means that the placed requests may not be visible
        // immediately)
        String name = String.format("await-spot-requests{%s}", placedSpotRequestIds);
        Callable<List<SpotInstanceRequest>> requester = new GetSpotInstanceRequests(getAwsCredentials(), getRegion(),
                getClientConfig(), null, null);
        Retryable<List<SpotInstanceRequest>> retryer = Retryers.exponentialBackoffRetryer(name, requester,
                INITIAL_BACKOFF_DELAY, TimeUnit.MILLISECONDS, MAX_RETRIES, contains(placedSpotRequestIds));

        try {
            // only return those spot requests that we actually placed
            List<SpotInstanceRequest> allInstances = retryer.call();
            return allInstances.stream()
                    .filter(request -> placedSpotRequestIds.contains(request.getSpotInstanceRequestId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(String.format("gave up waiting for spot instance " + "requests to appear %s: %s",
                    placedSpotRequestIds, e.getMessage()), e);
        }
    }

    /**
     * A predicate that returns <code>true</code> for any collection of input
     * {@link SpotInstanceRequest}s that contain an expected collection of spot
     * request identifiers.
     *
     * @param expectedSpotRequestIds
     * @return
     */
    private static Predicate<List<SpotInstanceRequest>> contains(final List<String> expectedSpotRequestIds) {
        return input -> {
            List<String> inputIds = Lists.transform(input, AwsEc2Functions.toSpotRequestId());
            return inputIds.containsAll(expectedSpotRequestIds);
        };
    }
}
