package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.SpotInstanceState;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;

/**
 * A {@link Callable} task that, when executed, requests meta data for AWS EC2
 * {@link SpotInstanceRequest}s in a region.
 * <p/>
 * The query can be limited to only return meta data for a particular group of
 * {@link SpotInstanceRequest}s. Furthermore, query {@link Filter}s can be
 * supplied to further narrow down the result set. Note that without
 * {@link Filter}s, the result may contain requests in all states: open, active,
 * cancelled, etc.
 * <p/>
 * If a set of spot instance request ids is specified, the query will be retried
 * until meta data could be retrieved for all requested instances. This behavior
 * is useful to handle the eventually consistent semantics of the EC2 API.
 * <p/>
 * AWS limits the number of filter values to some number (at the time of
 * writing, that number is 200). For a detailed description of supported
 * {@link Filter}s refer to the <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeSpotInstanceRequests.html"
 * >Amazon EC2 API</a>.
 * <p/>
 * The query can be limited to only return meta data for a particular group of
 * {@link SpotInstanceRequest}s via {@link Filter}s. Note that without
 * {@link Filter}s, the result may contain {@link SpotInstanceRequest}s in all
 * {@link SpotInstanceState}s (open, active, closed, cancelled, etc).
 * <p/>
 * For a detailed description of supported {@link Filter}s refer to the <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeSpotInstanceRequests.html"
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
public class GetSpotInstanceRequests extends
		AmazonEc2Request<List<SpotInstanceRequest>> {

	/**
	 * A list of spot request ids of interest to limit the query to. If
	 * specified, meta data will only be fetched for these instances. If
	 * <code>null</code> or empty list, meta data will be fetched for all spot
	 * requests.
	 */
	private final Collection<String> spotRequestIds;
	/**
	 * An (optional) list of filter to narrow the query. Only
	 * {@link SpotInstanceRequest}s matching the given filters will be returned.
	 */
	private final Collection<Filter> filters;

	/**
	 * Constructs a new {@link GetSpotInstanceRequests} task that will fetch all
	 * {@link SpotInstanceRequest}s in the region.
	 *
	 * @param awsCredentials
	 *            The AWS security credentials to the account.
	 * @param region
	 *            The AWS region of interest.
	 */
	public GetSpotInstanceRequests(AWSCredentials awsCredentials, String region) {
		this(awsCredentials, region, null);
	}

	/**
	 * Constructs a new {@link GetSpotInstanceRequests} task that will fetch all
	 * {@link SpotInstanceRequest}s in the region that match any of the
	 * specified filters.
	 *
	 * @param awsCredentials
	 *            The AWS security credentials to the account.
	 * @param region
	 *            The AWS region of interest.
	 * @param filters
	 *            A list of filter to narrow the query. Only
	 *            {@link SpotInstanceRequest}s matching the given filters will
	 *            be returned. May be <code>null</code> (no filters).
	 */
	public GetSpotInstanceRequests(AWSCredentials awsCredentials,
			String region, Collection<Filter> filters) {
		this(awsCredentials, region, null, filters);
	}

	/**
	 * Constructs a new {@link GetSpotInstanceRequests} task that will only
	 * fetch meta data for the given spot instance request ids that match the
	 * given filters.
	 *
	 * @param awsCredentials
	 *            The AWS security credentials to the account.
	 * @param region
	 *            The AWS region of interest.
	 * @param spotRequestIds
	 *            The spot request ids of interest. May be <code>null</code>.
	 * @param filters
	 *            A list of filter to narrow the query. Only
	 *            {@link SpotInstanceRequest}s matching the given filters will
	 *            be returned. May be <code>null</code> (no filters).
	 */
	public GetSpotInstanceRequests(AWSCredentials awsCredentials,
			String region, Collection<String> spotRequestIds,
			Collection<Filter> filters) {
		super(awsCredentials, region);
		this.spotRequestIds = spotRequestIds;
		this.filters = filters;
	}

	@Override
	public List<SpotInstanceRequest> call() throws AmazonClientException {
		DescribeSpotInstanceRequestsRequest request = new DescribeSpotInstanceRequestsRequest();
		if (this.spotRequestIds != null) {
			request.withSpotInstanceRequestIds(this.spotRequestIds);
		}
		if (this.filters != null) {
			request.withFilters(this.filters);
		}
		DescribeSpotInstanceRequestsResult result = getClient().getApi()
				.describeSpotInstanceRequests(request);
		return result.getSpotInstanceRequests();
	}
}