package com.elastisys.scale.cloudadapters.aws.commons.tasks;

import static java.lang.String.format;

import java.util.List;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.elastisys.scale.cloudadapters.aws.commons.functions.AwsEc2Functions;
import com.elastisys.scale.commons.net.retryable.Action;
import com.elastisys.scale.commons.net.retryable.RetryLimitExceededException;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;

/**
 * Due to eventual consistency, all instances reported as members of an auto
 * scaling group might not all actually be present when one asks via a
 * {@link DescribeInstancesRequest}. This class allows one to retry that query
 * until it succeeds.
 */
public class RetryUntilInstancesPresent extends
AbstractAmazonLimitedRetryHandler<List<Instance>> {
	/**
	 * The set of instance identifiers that we hope to get back from the query.
	 */
	private final ImmutableSet<String> instanceIds;

	/**
	 * Creates a new instance.
	 *
	 * @param instanceIds
	 *            The instance identifiers that should be part of the returned
	 *            set of identifiers. As long as they are not, the query will be
	 *            retried until the max number of retries is exceeded (or an
	 *            error from Amazon indicates that retries will not help).
	 *
	 * @param maxRetries
	 *            The maximum number of retries that will be attempted before
	 *            failing with a {@link RetryLimitExceededException}.
	 */
	public RetryUntilInstancesPresent(final List<String> instanceIds,
			int maxRetries) {
		super(maxRetries);
		this.instanceIds = ImmutableSet.copyOf(instanceIds);
	}

	@Override
	public boolean isSuccessful(List<Instance> response) {
		return this.instanceIds.containsAll(Collections2.transform(response,
				AwsEc2Functions.toInstanceId()));
	}

	@Override
	public Action<List<Instance>> maxRetriesExceeded(List<Instance> withResponse) {
		String message = format("Maximum number of retries (%d) exceeded. "
				+ "Last response: %s", this.maxRetries, withResponse);
		RetryLimitExceededException failureReason = new RetryLimitExceededException(
				message);
		return Action.fail(failureReason);
	}

}
