package com.elastisys.scale.cloudadapters.aws.commons.requests.ec2;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.lang.String.format;

import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.elastisys.scale.cloudadapers.api.NotFoundException;
import com.elastisys.scale.commons.net.retryable.Retryable;

/**
 * A {@link Callable} task that, when executed, requests details about a
 * particular AWS EC2 machine {@link Instance} in a region.
 * <p/>
 * Due to the eventual consistency semantics of the Amazon API, a recently
 * launched instance may not show up immediately when calling
 * {@code DescribeInstances}.
 * <p/>
 * To wait for an instance to become available, this could be used with a
 * {@link Retryable}.
 */
public class GetInstance extends AmazonEc2Request<Instance> {

	/** The identifier of {@link Instance} interest. */
	private final String instanceId;

	/**
	 * Constructs a new {@link GetInstance} request task.
	 *
	 * @param awsCredentials
	 * @param region
	 * @param instanceId
	 */
	public GetInstance(AWSCredentials awsCredentials, String region,
			String instanceId) {
		super(awsCredentials, region);
		this.instanceId = instanceId;
	}

	@Override
	public Instance call() throws NotFoundException {
		DescribeInstancesRequest request = new DescribeInstancesRequest()
				.withInstanceIds(this.instanceId);
		DescribeInstancesResult result = getClient().getApi()
				.describeInstances(request);
		if (result.getReservations().isEmpty()) {
			throw new NotFoundException(format(
					"DescribeInstances: no such instance exists: '%s'",
					this.instanceId));
		}
		Reservation reservation = getOnlyElement(result.getReservations());
		Instance instance = getOnlyElement(reservation.getInstances());
		return instance;
	}

}
