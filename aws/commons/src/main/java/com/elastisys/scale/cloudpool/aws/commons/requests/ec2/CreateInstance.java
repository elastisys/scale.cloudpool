package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import static com.elastisys.scale.cloudpool.aws.commons.predicates.InstancePredicates.instanceStateIn;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.elastisys.scale.cloudpool.aws.commons.client.AmazonApiUtils;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.google.common.collect.Iterables;

/**
 * A {@link Callable} task that, when executed, requests a AWS EC2 machine
 * instance to be created and waits for the instance to appear started in
 * {@code DescribeInstances}.
 * <p/>
 * Due to the eventual consistency semantics of the Amazon API, operations may
 * need time to propagate through the system and results may not be immediate.
 */
public class CreateInstance extends AmazonEc2Request<Instance> {

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

	public CreateInstance(AWSCredentials awsCredentials, String region,
			String availabilityZone, List<String> securityGroups,
			String keyPair, String instanceType, String imageId,
			String bootScript) {
		super(awsCredentials, region);
		this.availabilityZone = availabilityZone;
		this.securityGroups = securityGroups;
		this.keyPair = keyPair;
		this.instanceType = instanceType;
		this.imageId = imageId;
		this.bootScript = bootScript;
	}

	@Override
	public Instance call() {
		Placement placement = new Placement()
				.withAvailabilityZone(this.availabilityZone);
		RunInstancesRequest request = new RunInstancesRequest().withMinCount(1)
				.withMaxCount(1).withImageId(this.imageId)
				.withInstanceType(this.instanceType)
				.withUserData(AmazonApiUtils.base64Encode(this.bootScript))
				.withSecurityGroupIds(this.securityGroups)
				.withKeyName(this.keyPair).withPlacement(placement);
		RunInstancesResult result = getClient().getApi().runInstances(request);
		Instance launchedInstance = Iterables.getLast(result.getReservation()
				.getInstances());

		return awaitInstance(launchedInstance.getInstanceId());
	}

	private Instance awaitInstance(String instanceId) {
		String name = String.format("await-active-state{%s}", instanceId);
		Callable<Instance> requester = new GetInstance(getAwsCredentials(),
				getRegion(), instanceId);
		List<String> activeStates = asList("pending", "running");

		int initialDelay = 1;
		int maxRetries = 8;
		Retryable<Instance> retryer = Retryers.exponentialBackoffRetryer(name,
				requester, initialDelay, TimeUnit.SECONDS, maxRetries,
				instanceStateIn(activeStates));

		try {
			return retryer.call();
		} catch (Exception e) {
			throw new RuntimeException(String.format(
					"gave up waiting for instance to become active: '%s': %s",
					instanceId, e.getMessage()), e);
		}
	}
}
