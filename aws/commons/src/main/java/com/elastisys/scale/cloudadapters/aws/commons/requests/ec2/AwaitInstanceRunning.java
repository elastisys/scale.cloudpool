package com.elastisys.scale.cloudadapters.aws.commons.requests.ec2;

import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.InstanceState;
import com.elastisys.scale.cloudadapters.aws.commons.tasks.InstanceStateRequester;
import com.elastisys.scale.cloudadapters.aws.commons.tasks.RetryUntilRunning;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;

/**
 * A {@link Callable} task that, when executed, waits for an EC2 machine
 * instance to reach {@code running} state.
 *
 * 
 */
public class AwaitInstanceRunning extends AmazonEc2Request<InstanceState> {

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
	public AwaitInstanceRunning(AWSCredentials awsCredentials, String region,
			String instanceId, int maxRetries, int retryDelay) {
		super(awsCredentials, region);
		this.instanceId = instanceId;
		this.maxRetries = maxRetries;
		this.retryDelay = retryDelay;
	}

	@Override
	public InstanceState call() throws Exception {
		return awaitRunningState(this.instanceId);
	}

	private InstanceState awaitRunningState(String instanceID) throws Exception {
		String taskName = "await-running{" + this.instanceId + "}";
		Callable<InstanceState> runningStateWaiter = new RetryableRequest<InstanceState>(
				new InstanceStateRequester(getClient().getApi(),
						this.instanceId), new RetryUntilRunning(
						this.maxRetries, this.retryDelay), taskName);
		return runningStateWaiter.call();
	}

}
