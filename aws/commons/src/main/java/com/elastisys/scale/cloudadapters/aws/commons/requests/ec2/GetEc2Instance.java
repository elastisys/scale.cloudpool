package com.elastisys.scale.cloudadapters.aws.commons.requests.ec2;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.lang.String.format;

import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

/**
 * A {@link Callable} task that, when executed, requests details about a
 * particular AWS EC2 machine {@link Instance} in a region.
 * 
 * 
 */
public class GetEc2Instance extends AmazonEc2Request<Instance> {

	/** The identifier of {@link Instance} interest. */
	private final String instanceId;

	/**
	 * Constructs a new {@link GetEc2Instance} request task.
	 * 
	 * @param awsCredentials
	 * @param region
	 * @param instanceId
	 */
	public GetEc2Instance(AWSCredentials awsCredentials, String region,
			String instanceId) {
		super(awsCredentials, region);
		this.instanceId = instanceId;
	}

	@Override
	public Instance call() {
		DescribeInstancesRequest request = new DescribeInstancesRequest()
				.withInstanceIds(this.instanceId);
		DescribeInstancesResult result = getClient().getApi()
				.describeInstances(request);
		if (result.getReservations().isEmpty()) {
			throw new IllegalArgumentException(format(
					"no result was received on DescribeInstances for %s",
					this.instanceId));
		}
		Reservation reservation = getOnlyElement(result.getReservations());
		Instance instance = getOnlyElement(reservation.getInstances());
		return instance;
	}

}
