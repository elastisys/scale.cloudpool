package com.elastisys.scale.cloudadapters.aws.commons.requests.ec2;

import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.autoscaling.model.Activity;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.elastisys.scale.cloudadapters.aws.commons.client.AmazonApiUtils;
import com.google.common.collect.Iterables;

/**
 * A {@link Callable} task that, when executed, requests a AWS EC2 machine
 * instance to be created. The call blocks until the machine instance has
 * reached running state and passed the system reachability test.
 * <p/>
 * The termination is a long-running {@link Activity} which can be tracked
 * through the returned object.
 *
 *
 *
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
		// await running and await reachable checks are disabled for now, since
		// they take quite long to finish (especially the reachability check).
		// These checks are also somewhat superfluous if we have boot-time
		// liveness checking enabled.
		// InstanceState state = awaitRunningState(launchedInstance);
		// launchedInstance = launchedInstance.withState(state);
		// awaitReachability(launchedInstance);
		return launchedInstance;
	}

	private InstanceState awaitRunningState(Instance instance) throws Exception {
		return new AwaitInstanceRunning(getAwsCredentials(), getRegion(),
				instance.getInstanceId(), 30).call();
	}

	private void awaitReachability(Instance instance) throws Exception {
		new AwaitInstanceReachable(getAwsCredentials(), getRegion(),
				instance.getInstanceId(), 30).call();
	}
}
