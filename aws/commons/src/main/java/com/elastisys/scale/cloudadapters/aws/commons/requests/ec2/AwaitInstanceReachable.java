package com.elastisys.scale.cloudadapters.aws.commons.requests.ec2;

import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.elastisys.scale.cloudadapters.aws.commons.tasks.InstanceStatusRequester;
import com.elastisys.scale.cloudadapters.aws.commons.tasks.RetryUntilReachable;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;

/**
 * A {@link Callable} task that, when executed, waits for an EC2 machine
 * instance to pass the system reachability tests.
 *
 * 
 *
 */
public class AwaitInstanceReachable extends AmazonEc2Request<Void> {

	/** The id of the machine instance to wait for. */
	private final String instanceId;
	/** Maximum number of times to poll machine instance for state. */
	private final int maxRetries;
	/** Delay (in ms) between each machine instance state poll. */
	private final int retryDelay;

	/**
	 * Constructs a new {@link AwaitInstanceRunning} task.
	 *
	 * @param awsCredentials
	 *            AWS security credentials for the account to be used.
	 * @param region
	 *            The AWS region that the request will be sent to.
	 * @param instanceId
	 *            The id of the machine instance to wait for.
	 * @param maxRetries
	 *            Maximum number of times to poll machine instance for state.
	 * @param retryDelay
	 *            Delay (in ms) between each machine instance state poll.
	 */
	public AwaitInstanceReachable(AWSCredentials awsCredentials, String region,
			String instanceId, int maxRetries, int retryDelay) {
		super(awsCredentials, region);
		this.instanceId = instanceId;
		this.maxRetries = maxRetries;
		this.retryDelay = retryDelay;
	}

	@Override
	public Void call() throws Exception {
		awaitReachability(this.instanceId);
		return null;
	}

	private void awaitReachability(String instanceId) throws Exception {
		String taskName = "await-reachable{" + instanceId + "}";
		Callable<InstanceStatus> reachabilityWaiter = new RetryableRequest<InstanceStatus>(
				new InstanceStatusRequester(getClient().getApi(), instanceId),
				new RetryUntilReachable(this.maxRetries, this.retryDelay),
				taskName);
		reachabilityWaiter.call();
	}

}
