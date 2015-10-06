package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.SpotInstanceType;
import com.amazonaws.services.ec2.model.SpotPlacement;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.aws.commons.client.AmazonApiUtils;
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
public class PlaceSpotInstanceRequests extends
		AmazonEc2Request<List<SpotInstanceRequest>> {

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

	/** The user data boot script to use for the created instance. */
	private final String bootScript;
	/** The bid price to set for the spot request. */
	private final String bidPrice;

	/** The number of spot instances to request. */
	private final int count;

	/**
	 * The (possibly empty) set of {@link Tag}s to attach to the placed spot
	 * instance requests.
	 */
	private final List<Tag> tags;

	public PlaceSpotInstanceRequests(AWSCredentials awsCredentials,
			String region, double bidPrice, String availabilityZone,
			List<String> securityGroups, String keyPair, String instanceType,
			String imageId, String bootScript, int count, List<Tag> tags) {
		super(awsCredentials, region);
		this.bidPrice = String.valueOf(bidPrice);
		this.availabilityZone = availabilityZone;
		this.securityGroups = securityGroups;
		this.keyPair = keyPair;
		this.instanceType = instanceType;
		this.imageId = imageId;
		this.bootScript = bootScript;
		this.count = count;
		this.tags = tags;
	}

	@Override
	public List<SpotInstanceRequest> call() {
		SpotPlacement placement = new SpotPlacement()
				.withAvailabilityZone(this.availabilityZone);
		LaunchSpecification launchSpec = new LaunchSpecification()
				.withInstanceType(this.instanceType).withImageId(this.imageId)
				.withPlacement(placement)
				.withSecurityGroups(this.securityGroups)
				.withKeyName(this.keyPair)
				.withUserData(AmazonApiUtils.base64Encode(this.bootScript));
		RequestSpotInstancesRequest request = new RequestSpotInstancesRequest()
				.withInstanceCount(this.count)
				.withType(SpotInstanceType.Persistent)
				.withSpotPrice(this.bidPrice)
				.withLaunchSpecification(launchSpec);
		RequestSpotInstancesResult result = getClient().getApi()
				.requestSpotInstances(request);

		List<String> spotRequestIds = Lists.transform(
				result.getSpotInstanceRequests(),
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
		Callable<Void> requester = new TagEc2Resources(getAwsCredentials(),
				getRegion(), spotRequestIds, this.tags);
		String tagTaskName = String.format("tag{%s}", spotRequestIds);
		Retryable<Void> retryable = Retryers.exponentialBackoffRetryer(
				tagTaskName, requester, INITIAL_BACKOFF_DELAY,
				TimeUnit.MILLISECONDS, MAX_RETRIES);
		try {
			retryable.call();
		} catch (Exception e) {
			throw new RuntimeException(String.format(
					"gave up trying to tag spot instance requests %s: %s",
					spotRequestIds, e.getMessage()), e);
		}
	}

	/**
	 * Waits for all placed spot requests to become visible in the API.
	 *
	 * @param spotRequestIds
	 * @return
	 */
	private List<SpotInstanceRequest> awaitSpotRequests(
			List<String> spotRequestIds) {

		String name = String.format("await-spot-requests{%s}", spotRequestIds);
		Callable<List<SpotInstanceRequest>> requester = new GetSpotInstanceRequests(
				getAwsCredentials(), getRegion(), spotRequestIds, null);

		Retryable<List<SpotInstanceRequest>> retryer = Retryers
				.exponentialBackoffRetryer(name, requester,
						INITIAL_BACKOFF_DELAY, TimeUnit.MILLISECONDS,
						MAX_RETRIES, contains(spotRequestIds));

		try {
			return retryer.call();
		} catch (Exception e) {
			throw new RuntimeException(String.format(
					"gave up waiting for spot instance "
							+ "requests to appear %s: %s", spotRequestIds,
					e.getMessage()), e);
		}
	}

	/**
	 * A predicate that returns <code>true</code> for any collection of input
	 * {@link SpotInstanceRequest}s that contain an exepcted collection of spot
	 * request identifiers.
	 *
	 * @param expectedSpotRequestIds
	 * @return
	 */
	private static Predicate<List<SpotInstanceRequest>> contains(
			final List<String> expectedSpotRequestIds) {
		return new Predicate<List<SpotInstanceRequest>>() {
			@Override
			public boolean apply(List<SpotInstanceRequest> input) {
				List<String> inputIds = Lists.transform(input,
						AwsEc2Functions.toSpotRequestId());
				return inputIds.containsAll(expectedSpotRequestIds);
			}
		};
	}
}
