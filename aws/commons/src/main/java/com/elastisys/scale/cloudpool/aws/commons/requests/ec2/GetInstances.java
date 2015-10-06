package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.google.common.collect.Lists;

/**
 * A {@link Callable} task that, when executed, requests meta data for AWS EC2
 * machine instances in a region.
 * <p/>
 * The query can be limited to only return meta data for a particular group of
 * instances. Furthermore, query {@link Filter}s can be supplied to further
 * narrow down the result set. Note that without {@link Filter}s, the result may
 * contain instances in all states: pending, running, terminated, etc.
 * <p/>
 * AWS limits the number of filter values to some number (at the time of
 * writing, that number is 200). For a detailed description of supported
 * {@link Filter}s refer to the <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DescribeInstances.html"
 * >Amazon EC2 API</a>.
 * <p/>
 * Note that due to the <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/query-api-troubleshooting.html#eventual-consistency"
 * >eventual consistency semantics</a> of the Amazon API, a recently created EC2
 * instance or spot instance request may not be immediately available for
 * tagging. Therefore, it might be wise to use a retry strategy (with
 * exponential back-off) when tagging a recently created resource.
 *
 * @see Retryable
 * @see Retryers
 */
public class GetInstances extends AmazonEc2Request<List<Instance>> {

	/**
	 * A list of instance ids of interest to limit the query to. If specified,
	 * meta data will only be fetched for these instances. If <code>null</code>
	 * or empty list, meta data will be fetched for all instances.
	 */
	private List<String> instanceIds;
	/**
	 * An (optional) list of filter to narrow the query. Only instances matching
	 * the given filters will be returned.
	 */
	private List<Filter> filters;

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
		this.instanceIds = null;
		this.filters = null;
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
	 *            <code>null</code> or empty list, meta data will be fetched for
	 *            all instances.
	 */
	public GetInstances(AWSCredentials awsCredentials, String region,
			List<String> instanceIds) {
		super(awsCredentials, region);
		this.instanceIds = instanceIds;
		this.filters = null;
	}

	/**
	 * Constructs a new {@link GetInstances} task that will fetch instances in
	 * the region that match any of the specified instance ids and satisfy the
	 * given filters.
	 *
	 * @param awsCredentials
	 *            The AWS security credentials to the account.
	 * @param region
	 *            The AWS region of interest.
	 * @param instanceIds
	 *            A list of instance ids of interest to limit the query to. If
	 *            <code>null</code> or empty list, meta data will be fetched for
	 *            all instances.
	 * @param filters
	 *            A list of filter to narrow the query. Only instances matching
	 *            the given filters will be returned.
	 */
	public GetInstances(AWSCredentials awsCredentials, String region,
			List<String> instanceIds, List<Filter> filters) {
		super(awsCredentials, region);
		this.instanceIds = instanceIds;
		this.filters = filters;
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
		this.filters = filters;
		return this;
	}

	/**
	 * Sets an instance id filter that will be used to narrow down the result
	 * set when the query is {@link #call()}ed.
	 *
	 * @param filters
	 *            A list of instance ids of interest to limit the query to. If
	 *            <code>null</code> or empty list, meta data will be fetched for
	 *            all instances.
	 */
	public GetInstances withInstanceIds(List<String> instanceIds) {
		this.instanceIds = instanceIds;
		return this;
	}

	@Override
	public List<Instance> call() {
		List<Instance> instances = Lists.newArrayList();
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		request.withInstanceIds(this.instanceIds);
		request.withFilters(this.filters);
		// paginate through result as long as there is another response token
		boolean moreResults = false;
		do {
			DescribeInstancesResult result = getClient().getApi()
					.describeInstances(request);
			instances.addAll(instances(result));
			moreResults = (result.getNextToken() != null)
					&& !result.getNextToken().equals("");
			request.setNextToken(result.getNextToken());
		} while (moreResults);

		return instances;
	}

	private List<Instance> instances(DescribeInstancesResult result) {
		List<Instance> instances = Lists.newArrayList();
		List<Reservation> reservations = result.getReservations();
		for (Reservation reservation : reservations) {
			instances.addAll(reservation.getInstances());
		}
		return instances;
	}

}