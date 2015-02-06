package com.elastisys.scale.cloudadapters.aws.commons.requests.ec2;

import static java.lang.String.format;

import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.elastisys.scale.cloudadapters.aws.commons.tasks.InstanceListingRequester;
import com.elastisys.scale.cloudadapters.aws.commons.tasks.RetryUntilInstancesPresent;
import com.elastisys.scale.commons.net.retryable.RetryHandler;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;
import com.google.common.base.Optional;

/**
 * A {@link Callable} task that, when executed, requests meta data for AWS EC2
 * machine instances in a region. The query can be limited to only return meta
 * data for a particular group of instances. Furthermore, query {@link Filter}s
 * can be supplied to further narrow down the result set. Note that without
 * {@link Filter}s, the result may contain instances in all states: pending,
 * running, terminated, etc.
 * <p/>
 * If a set of instance ids is specified, the query will be retried until meta
 * data could be retrieved for all requested instances. This behavior is useful
 * to handle the eventually consistent semantics of the EC2 API.
 * <p/>
 * AWS limits the number of filter values to some number (at the time of
 * writing, that number is 200).
 * <p/>
 * For a detailed description of supported {@link Filter}s refer to the <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DescribeInstances.html"
 * >Amazon EC2 API</a>.
 */
public class GetInstances extends AmazonEc2Request<List<Instance>> {

	/** Exponential back-off means last wait is 2^(MAX_RETRIES-1) seconds. */
	private static final int MAX_RETRIES = 10;

	/**
	 * A list of instance ids of interest to limit the query to. If specified,
	 * meta data will only be fetched for these instances. If left out, meta
	 * data will be fetched for all instances.
	 */
	private Optional<List<String>> instanceIds;
	/**
	 * A list of filter to narrow the query. Only instances matching the given
	 * filters will be returned.
	 */
	private Optional<List<Filter>> filters;

	/**
	 * Constructs a new {@link GetInstances} task that will fetch all instances
	 * in the region.
	 *
	 * @param awsCredentials
	 *            The AWS security credentials to the account.
	 * @param region
	 *            The AWS region of interest.
	 */
	public GetInstances(AWSCredentials awsCredentials, String region) {
		super(awsCredentials, region);
		this.filters = Optional.absent();
		this.instanceIds = Optional.absent();
	}

	/**
	 * Constructs a new {@link GetInstances} task that will fetch instances in
	 * the region that match any of the specified instance ids.
	 *
	 * @param awsCredentials
	 *            The AWS security credentials to the account.
	 * @param region
	 *            The AWS region of interest.
	 * @param instanceIds
	 *            A list of instance ids of interest to limit the query to. If
	 *            specified, meta data will only be fetched for these instances.
	 */
	public GetInstances(AWSCredentials awsCredentials, String region,
			List<String> instanceIds) {
		super(awsCredentials, region);
		this.filters = Optional.absent();
		this.instanceIds = Optional.of(instanceIds);
	}

	/**
	 * Constructs a new {@link GetInstances} task that will fetch instances in
	 * the region that match any of the specified instance ids and the given
	 * filters.
	 *
	 * @param awsCredentials
	 *            The AWS security credentials to the account.
	 * @param region
	 *            The AWS region of interest.
	 * @param instanceIds
	 *            A list of instance ids of interest to limit the query to. If
	 *            specified, meta data will only be fetched for these instances.
	 * @param filters
	 *            A list of filter to narrow the query. Only instances matching
	 *            the given filters will be returned.
	 */
	public GetInstances(AWSCredentials awsCredentials, String region,
			List<String> instanceIds, List<Filter> filters) {
		super(awsCredentials, region);
		this.filters = Optional.of(filters);
		this.instanceIds = Optional.of(instanceIds);
	}

	/**
	 * Sets {@link Filter}s that will be used to narrow down the query when
	 * {@link #call()}ed.
	 *
	 * @param filters
	 *            A list of filter to narrow the query. Only instances matching
	 *            the given filters will be returned.
	 * @return
	 */
	public GetInstances withFilters(List<Filter> filters) {
		this.filters = Optional.fromNullable(filters);
		return this;
	}

	/**
	 * Sets an instance id filter that will be used to narrow down the result
	 * set when the query is {@link #call()}ed.
	 *
	 * @param filters
	 *            A list of instance ids of interest to limit the query to. If
	 *            specified, meta data will only be fetched for these instances.
	 */
	public GetInstances withInstanceIds(List<String> instanceIds) {
		this.instanceIds = Optional.fromNullable(instanceIds);
		return this;
	}

	@Override
	public List<Instance> call() {
		InstanceListingRequester request = new InstanceListingRequester(
				getAwsCredentials(), getRegion(), this.instanceIds,
				this.filters);

		// not waiting for any instance ids in particular: call once
		if (!this.instanceIds.isPresent()) {
			return request.call();
		}

		// waiting for a number of expected instance ids: repeat until all are
		// available in response
		RetryHandler<List<Instance>> retryHandler = new RetryUntilInstancesPresent(
				this.instanceIds.get(), MAX_RETRIES);
		String taskName = String.format("await-describe-instances");
		Callable<List<Instance>> retryableRequest = new RetryableRequest<List<Instance>>(
				request, retryHandler, taskName);
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
