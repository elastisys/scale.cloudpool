package com.elastisys.scale.cloudadapters.aws.commons.requests.ec2;

import static com.elastisys.scale.cloudadapters.aws.commons.predicates.InstancePredicates.instanceStateIn;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.google.common.collect.Iterables;

/**
 * A {@link Callable} task that, when executed, requests an EC2 instance to be
 * terminated and waits for the instance to appear terminating/terminated in
 * {@code DescribeInstances}.
 * <p/>
 * Due to the eventual consistency semantics of the Amazon API, operations may
 * need time to propagate through the system and results may not be immediate.
 */
public class TerminateInstance extends AmazonEc2Request<InstanceStateChange> {

	/** The machine instance id to be terminated. */
	private final String instanceId;

	public TerminateInstance(AWSCredentials awsCredentials, String region,
			String instanceId) {
		super(awsCredentials, region);
		this.instanceId = instanceId;
	}

	@Override
	public InstanceStateChange call() {
		TerminateInstancesRequest request = new TerminateInstancesRequest()
				.withInstanceIds(this.instanceId);
		TerminateInstancesResult result = getClient().getApi()
				.terminateInstances(request);
		InstanceStateChange stateChange = Iterables.getOnlyElement(result
				.getTerminatingInstances());

		awaitTermination(this.instanceId);

		return stateChange;
	}

	private void awaitTermination(String instanceId) {
		String name = String.format("await-terminal-state{%s}", instanceId);
		Callable<Instance> stateRequester = new GetInstance(
				getAwsCredentials(), getRegion(), instanceId);
		List<String> terminalStates = asList("shutting-down", "terminated");
		int initialDelay = 1;
		int maxRetries = 8;
		Retryable<Instance> retryer = Retryers.exponentialBackoffRetryer(name,
				stateRequester, initialDelay, TimeUnit.SECONDS, maxRetries,
				instanceStateIn(terminalStates));

		try {
			retryer.call();
		} catch (Exception e) {
			throw new RuntimeException(String.format(
					"gave up waiting for instance to terminate: '%s': %s",
					instanceId, e.getMessage()), e);
		}
	}
}
