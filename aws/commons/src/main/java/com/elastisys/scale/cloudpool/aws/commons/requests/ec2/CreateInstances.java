package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import static com.elastisys.scale.cloudpool.aws.commons.predicates.InstancePredicates.allInAnyOfStates;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.elastisys.scale.cloudpool.aws.commons.client.AmazonApiUtils;
import com.elastisys.scale.cloudpool.aws.commons.functions.AwsEc2Functions;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
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

	/** The user data boot script to use for the created instance. */
	private final String bootScript;

	/** The number of spot instances to request. */
	private final int count;

	/**
	 * The (possibly empty) set of {@link Tag}s to attach to the placed spot
	 * instance requests.
	 */
	private final List<Tag> tags;

	public CreateInstances(AWSCredentials awsCredentials, String region,
			String availabilityZone, List<String> securityGroups,
			String keyPair, String instanceType, String imageId,
			String bootScript, int count, List<Tag> tags) {
		super(awsCredentials, region);
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
	public List<Instance> call() {
		Placement placement = new Placement()
				.withAvailabilityZone(this.availabilityZone);
		RunInstancesRequest request = new RunInstancesRequest()
				.withMinCount(this.count).withMaxCount(this.count)
				.withImageId(this.imageId).withInstanceType(this.instanceType)
				.withUserData(AmazonApiUtils.base64Encode(this.bootScript))
				.withSecurityGroupIds(this.securityGroups)
				.withKeyName(this.keyPair).withPlacement(placement);
		RunInstancesResult result = getClient().getApi().runInstances(request);
		List<Instance> launchedInstances = result.getReservation()
				.getInstances();

		List<String> instanceIds = Lists.transform(launchedInstances,
				AwsEc2Functions.toInstanceId());

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
		Callable<Void> requester = new TagEc2Resources(getAwsCredentials(),
				getRegion(), instanceIds, this.tags);
		String tagTaskName = String.format("tag{%s}", instanceIds);
		Retryable<Void> retryable = Retryers.exponentialBackoffRetryer(
				tagTaskName, requester, INITIAL_BACKOFF_DELAY,
				TimeUnit.MILLISECONDS, MAX_RETRIES);
		try {
			retryable.call();
		} catch (Exception e) {
			throw new RuntimeException(String.format(
					"gave up trying to tag instances %s: %s", instanceIds,
					e.getMessage()), e);
		}
	}

	private List<Instance> awaitInstances(List<String> instanceIds) {
		String name = String.format("await-active-state{%s}", instanceIds);
		Callable<List<Instance>> requester = new GetInstances(
				getAwsCredentials(), getRegion(), instanceIds);

		Retryable<List<Instance>> retryer = Retryers.exponentialBackoffRetryer(
				name, requester, INITIAL_BACKOFF_DELAY, TimeUnit.MILLISECONDS,
				MAX_RETRIES, allInAnyOfStates("pending", "running"));

		try {
			return retryer.call();
		} catch (Exception e) {
			throw new RuntimeException(String.format(
					"gave up waiting for instances to become active %s: %s",
					instanceIds, e.getMessage()), e);
		}
	}
}
