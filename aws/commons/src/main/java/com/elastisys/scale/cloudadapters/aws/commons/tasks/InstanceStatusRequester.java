package com.elastisys.scale.cloudadapters.aws.commons.tasks;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.sns.model.NotFoundException;
import com.elastisys.scale.commons.net.retryable.Requester;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;

/**
 * A {@link Requester} that returns the {@link InstanceStatus} for a given
 * Amazon EC2 instance.
 * <p/>
 * It can, for example, be used in a {@link RetryableRequest} in concert with
 * the {@link RetryUntilReachable} to wait for an instance's reachability tests
 * to pass.
 *
 * @see RetryableRequest
 * @see InstanceStateRequester
 *
 * 
 *
 */
public class InstanceStatusRequester implements Requester<InstanceStatus> {
	static Logger logger = LoggerFactory
			.getLogger(InstanceStatusRequester.class);

	private final AmazonEC2 ec2Client;
	private final String instanceId;

	public InstanceStatusRequester(AmazonEC2 ec2Client, String instanceId) {
		this.ec2Client = ec2Client;
		this.instanceId = instanceId;
	}

	@Override
	public InstanceStatus call() throws Exception {
		DescribeInstanceStatusResult statusResult = this.ec2Client
				.describeInstanceStatus(new DescribeInstanceStatusRequest()
				.withInstanceIds(this.instanceId));

		if (statusResult.getInstanceStatuses().isEmpty()) {
			throw new NotFoundException(
					format("DescribeInstanceStatus did not return any health status for %s",
							this.instanceId));
		}

		return getOnlyElement(statusResult.getInstanceStatuses());
	}
}
