package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import static com.google.common.base.Predicates.not;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.SpotInstanceType;
import com.amazonaws.services.ec2.model.SpotPlacement;
import com.elastisys.scale.cloudpool.aws.commons.ScalingFilters;
import com.elastisys.scale.cloudpool.aws.commons.client.AmazonApiUtils;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * A {@link Callable} task that, when executed, requests a AWS spot instance and
 * waits for the {@link SpotInstanceRequest} to appear in
 * {@code DescribeSpotInstanceRequests}.
 * <p/>
 * Due to the <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/query-api-troubleshooting.html#eventual-consistency"
 * >eventual consistency semantics</a> of the Amazon API, operations may need
 * time to propagate through the system and results may not be immediate.
 */
public class PlaceSpotInstanceRequest extends
		AmazonEc2Request<SpotInstanceRequest> {

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

	public PlaceSpotInstanceRequest(AWSCredentials awsCredentials,
			String region, double bidPrice, String availabilityZone,
			List<String> securityGroups, String keyPair, String instanceType,
			String imageId, String bootScript) {
		super(awsCredentials, region);
		this.bidPrice = String.valueOf(bidPrice);
		this.availabilityZone = availabilityZone;
		this.securityGroups = securityGroups;
		this.keyPair = keyPair;
		this.instanceType = instanceType;
		this.imageId = imageId;
		this.bootScript = bootScript;
	}

	@Override
	public SpotInstanceRequest call() {
		SpotPlacement placement = new SpotPlacement()
				.withAvailabilityZone(this.availabilityZone);
		LaunchSpecification launchSpec = new LaunchSpecification()
				.withInstanceType(this.instanceType).withImageId(this.imageId)
				.withPlacement(placement)
				.withSecurityGroups(this.securityGroups)
				.withKeyName(this.keyPair)
				.withUserData(AmazonApiUtils.base64Encode(this.bootScript));
		RequestSpotInstancesRequest request = new RequestSpotInstancesRequest()
				.withInstanceCount(1).withType(SpotInstanceType.Persistent)
				.withSpotPrice(this.bidPrice)
				.withLaunchSpecification(launchSpec);
		RequestSpotInstancesResult result = getClient().getApi()
				.requestSpotInstances(request);

		SpotInstanceRequest spotRequest = Iterables.getOnlyElement(result
				.getSpotInstanceRequests());
		awaitSpotRequest(spotRequest.getSpotInstanceRequestId());
		return spotRequest;
	}

	/**
	 * Waits for the new spot request to become visible in the API.
	 *
	 * @param spotRequestId
	 * @return
	 */
	private SpotInstanceRequest awaitSpotRequest(String spotRequestId) {
		String name = String.format("await-spot-request{%s}", spotRequestId);
		Filter idFilter = new Filter().withName(
				ScalingFilters.SPOT_REQUEST_ID_FILTER)
				.withValues(spotRequestId);
		Callable<List<SpotInstanceRequest>> requester = new GetSpotInstanceRequests(
				getAwsCredentials(), getRegion(), asList(idFilter));

		int initialDelay = 1;
		int maxRetries = 8;
		Retryable<List<SpotInstanceRequest>> retryer = Retryers
				.exponentialBackoffRetryer(name, requester, initialDelay,
						TimeUnit.SECONDS, maxRetries, not(empty()));

		try {
			return Iterables.getOnlyElement(retryer.call());
		} catch (Exception e) {
			throw new RuntimeException(String.format(
					"gave up waiting for spot instance "
							+ "request to appear: '%s': %s", spotRequestId,
					e.getMessage()), e);
		}
	}

	/**
	 * A {@link Predicate} that returns true when given an empty list of
	 * {@link SpotInstanceRequest}s.
	 *
	 * @return
	 */
	private Predicate<List<SpotInstanceRequest>> empty() {
		return new Predicate<List<SpotInstanceRequest>>() {
			@Override
			public boolean apply(List<SpotInstanceRequest> input) {
				return input.isEmpty();
			}
		};
	}
}
