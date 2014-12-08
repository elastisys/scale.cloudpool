package com.elastisys.scale.cloudadapters.aws.commons.requests.ec2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
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
		List<Instance> instances = Lists.newLinkedList();

		DescribeInstancesRequest request = new DescribeInstancesRequest();

		if (this.filters.size() > 0) {
			request.withFilters(this.filters);
		}

		if (this.instanceIds.size() > 0) {
			request.setInstanceIds(this.instanceIds);
		}

		DescribeInstancesResult result = getClient().getApi()
				.describeInstances(request);

		for (Reservation reservation : result.getReservations()) {
			for (Instance instance : reservation.getInstances()) {
				instances.add(instance);
			}
		}
		return instances;
	}

}
