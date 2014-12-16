package com.elastisys.scale.cloudadapters.aws.commons.requests.ec2;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.elastisys.scale.cloudadapters.aws.commons.tasks.InstanceListingRequester;
import com.elastisys.scale.cloudadapters.aws.commons.tasks.RetryUntilInstancesPresent;
import com.elastisys.scale.commons.net.retryable.RetryHandler;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;
import com.google.common.collect.Lists;

/**
 * A {@link Callable} task that, when executed, requests a listing of all AWS
 * EC2 machine instance in a region. Note that, unless query {@link Filter}s are
 * supplied, the result may contain instances in all states: pending, running,
 * terminated, etc.
 * <p/>
 * AWS limits the number of filter values to some number (at the time of
 * writing, that number is 200), but {@link DescribeInstancesRequest} has
 * special support that does not use filters to describe instances by id. For
 * that reason, there is special support for optionally supplying a list of
 * instance ids of interest.
 * <p/>
 * For a detailed description of supported {@link Filter}s refer to the <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DescribeInstances.html"
 * >Amazon EC2 API</a>.
 *
 *
 *
 */
public class GetInstances extends AmazonEc2Request<List<Instance>> {

	private static final int MAX_NUMBER_OF_RETRIES = 30;
	private final List<Filter> filters;
	private final List<String> instanceIds;

	/**
	 * Constructs a new {@link GetInstances} task.
	 *
	 * @param awsCredentials
	 *            The AWS security credentials to the account.
	 * @param region
	 *            The AWS region of interest.
	 * @param filters
	 *            A list of filter to narrow the query.
	 */
	public GetInstances(AWSCredentials awsCredentials, String region,
			Filter... filters) {
		this(awsCredentials, region, Arrays.asList(filters));
	}

	/**
	 * Constructs a new {@link GetInstances} task.
	 *
	 * @param awsCredentials
	 *            The AWS security credentials to the account.
	 * @param region
	 *            The AWS region of interest.
	 * @param filters
	 *            A list of filter to narrow the query.
	 */
	public GetInstances(AWSCredentials awsCredentials, String region,
			List<Filter> filters) {
		this(awsCredentials, region, Collections.<String> emptyList(), filters);
	}

	/**
	 * Constructs a new {@link GetInstances} task.
	 *
	 * @param awsCredentials
	 *            The AWS security credentials to the account.
	 * @param region
	 *            The AWS region of interest.
	 * @param instanceIds
	 *            A list of instance ids of interest to limit the query to. AWS
	 *            limits the number of filter values to some number (at the time
	 *            of writing, that number is 200), but
	 *            {@link DescribeInstancesRequest} has special support that does
	 *            not use filters to describe instances by id.
	 * @param filters
	 *            A list of filter to narrow the query.
	 */
	public GetInstances(AWSCredentials awsCredentials, String region,
			List<String> instanceIds, List<Filter> filters) {
		super(awsCredentials, region);
		this.filters = Lists.newArrayList(filters);
		this.instanceIds = Lists.newArrayList(instanceIds);
	}

	@Override
	public List<Instance> call() {
		RetryHandler<List<Instance>> retryHandler = new RetryUntilInstancesPresent(
				this.instanceIds, MAX_NUMBER_OF_RETRIES);

		String taskName = String.format("await-identified-instances");
		Callable<List<Instance>> retryableRequest = new RetryableRequest<List<Instance>>(
				new InstanceListingRequester(getAwsCredentials(), getRegion(),
						this.instanceIds, this.filters), retryHandler, taskName);

		try {
			return retryableRequest.call();
		} catch (Exception e) {
			String message = format(
					"Gave up waiting for instance listing to include all "
							+ "members that were sought due to %s",
							e.getMessage());
			throw new RuntimeException(message, e);
		}
	}

}
