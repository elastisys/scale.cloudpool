package com.elastisys.scale.cloudadapters.aws.commons.requests.ec2;

import java.util.Arrays;
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
 * For a detailed description of supported {@link Filter}s refer to the <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DescribeInstances.html"
 * >Amazon EC2 API</a>.
 *
 * 
 *
 */
public class GetInstances extends AmazonEc2Request<List<Instance>> {

	private List<Filter> filters;

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
		super(awsCredentials, region);
		this.filters = Lists.newArrayList(filters);
	}

	@Override
	public List<Instance> call() {
		List<Instance> instances = Lists.newLinkedList();

		DescribeInstancesResult result = getClient().getApi()
				.describeInstances(
						new DescribeInstancesRequest()
								.withFilters(this.filters));
		for (Reservation reservation : result.getReservations()) {
			for (Instance instance : reservation.getInstances()) {
				instances.add(instance);
			}
		}
		return instances;
	}

}
