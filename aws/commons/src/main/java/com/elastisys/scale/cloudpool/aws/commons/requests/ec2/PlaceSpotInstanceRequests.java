package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import java.util.ArrayList;
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
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceSpecification;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.SpotInstanceType;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.aws.commons.poolclient.Ec2ProvisioningTemplate;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * A {@link Callable} task that, when executed, requests a number of AWS spot
 * instances, tags them, and waits for them to appear in the API, which may not
 * be immediate due to the <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/query-api-troubleshooting.html#eventual-consistency"
 * >eventual consistency semantics</a> of the Amazon API.
 */
public class PlaceSpotInstanceRequests extends AmazonEc2Request<List<SpotInstanceRequest>> {

    /** Initial exponential back-off delay in ms. */
    private static final int INITIAL_BACKOFF_DELAY = 1000;
    /** Maximum number of retries of operations. */
    private static final int MAX_RETRIES = 8;

    /** The provisioning template used to describe the instances to create. */
    private final Ec2ProvisioningTemplate instanceTemplate;

    /** The number of instances to request. */
    private final int count;

    /** The bid price to set for the spot request. */
    private final String bidPrice;

    /** Random generator to spread instances across subnets. */
    private final Random random = new Random(UtcTime.now().getMillis());

    public PlaceSpotInstanceRequests(AWSCredentials awsCredentials, String region, ClientConfiguration clientConfig,
            Ec2ProvisioningTemplate instanceTemplate, int count, double bidPrice) {
        super(awsCredentials, region, clientConfig);
        this.instanceTemplate = instanceTemplate;
        this.count = count;
        this.bidPrice = String.valueOf(bidPrice);
    }

    @Override
    public List<SpotInstanceRequest> call() {
        LaunchSpecification spec = new LaunchSpecification();
        spec.withInstanceType(this.instanceTemplate.getInstanceType());
        spec.withImageId(this.instanceTemplate.getAmiId());

        InstanceNetworkInterfaceSpecification nic = new InstanceNetworkInterfaceSpecification();
        nic.withDeviceIndex(0);
        // select a subnet at random
        nic.withSubnetId(randomSubnet());
        nic.withAssociatePublicIpAddress(this.instanceTemplate.isAssignPublicIp());
        nic.withGroups(this.instanceTemplate.getSecurityGroupIds());
        spec.withNetworkInterfaces(nic);

        spec.withKeyName(this.instanceTemplate.getKeyPair());
        spec.withIamInstanceProfile(
                new IamInstanceProfileSpecification().withArn(this.instanceTemplate.getIamInstanceProfileARN()));
        spec.withUserData(this.instanceTemplate.getEncodedUserData());
        spec.withEbsOptimized(this.instanceTemplate.isEbsOptimized());

        RequestSpotInstancesRequest spotRequest = new RequestSpotInstancesRequest().withInstanceCount(this.count)
                .withType(SpotInstanceType.Persistent).withSpotPrice(this.bidPrice).withLaunchSpecification(spec);

        RequestSpotInstancesResult result = getClient().getApi().requestSpotInstances(spotRequest);
        List<String> spotRequestIds = result.getSpotInstanceRequests().stream()
                .map(SpotInstanceRequest::getSpotInstanceRequestId).collect(Collectors.toList());

        if (!this.instanceTemplate.getTags().isEmpty()) {
            tagRequests(spotRequestIds);
        }

        return awaitSpotRequests(spotRequestIds);
    }

    private List<Tag> tags() {
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
     * Tags each spot request with the set of {@link Tag}s that were passed to
     * this {@link PlaceSpotInstanceRequests} task on creation.
     *
     * @param spotRequestIds
     */
    private void tagRequests(List<String> spotRequestIds) {
        Callable<Void> requester = new TagEc2Resources(getAwsCredentials(), getRegion(), getClientConfig(),
                spotRequestIds, tags());
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
        return spotRequests -> {
            List<String> spotIds = spotRequests.stream().map(SpotInstanceRequest::getSpotInstanceRequestId)
                    .collect(Collectors.toList());
            return spotIds.containsAll(expectedSpotRequestIds);
        };
    }
}
