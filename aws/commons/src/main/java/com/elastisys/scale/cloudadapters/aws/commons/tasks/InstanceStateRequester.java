package com.elastisys.scale.cloudadapters.aws.commons.tasks;

import static com.google.common.collect.Iterables.getOnlyElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.sns.model.NotFoundException;
import com.elastisys.scale.commons.net.retryable.Requester;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;

/**
 * A {@link Requester} that returns the {@link InstanceState} for a given Amazon
 * EC2 instance.
 * <p/>
 * It can, for example, be used in a {@link RetryableRequest} in concert with
 * the {@link RetryUntilRunning} to wait for an instance to reach
 * {@code running} state.
 *
 * @see RetryableRequest
 * @see InstanceStateRequester
 *
 * @see RetryableRequest
 * 
 *
 */
public class InstanceStateRequester implements Requester<InstanceState> {
	static Logger logger = LoggerFactory
			.getLogger(InstanceStateRequester.class);

	private final AmazonEC2 ec2Client;
	private final String instanceId;

	public InstanceStateRequester(AmazonEC2 ec2Client, String instanceId) {
		this.ec2Client = ec2Client;
		this.instanceId = instanceId;
	}

	@Override
	public InstanceState call() throws Exception {
		DescribeInstancesRequest request = new DescribeInstancesRequest()
		.withInstanceIds(this.instanceId);
		DescribeInstancesResult result = this.ec2Client
				.describeInstances(request);
		if (!result.getReservations().isEmpty()) {
			Reservation reservation = getOnlyElement(result.getReservations());
			return getOnlyElement(reservation.getInstances()).getState();
		} else {
			throw new NotFoundException(
					"No reservation received for describe instance call");
		}
	}

}
