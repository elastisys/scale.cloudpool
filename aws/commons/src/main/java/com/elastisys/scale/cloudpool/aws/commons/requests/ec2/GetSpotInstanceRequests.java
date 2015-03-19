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

/**
 * A {@link Callable} task that, when executed, requests meta data for AWS EC2
 * {@link SpotInstanceRequest}s in a region.
 * <p/>
 * The query can be limited to only return meta data for a particular group of
 * {@link SpotInstanceRequest}s via {@link Filter}s. Note that without
 * {@link Filter}s, the result may contain {@link SpotInstanceRequest}s in all
 * {@link SpotInstanceState}s (open, active, closed, cancelled, etc).
 * <p/>
 * For a detailed description of supported {@link Filter}s refer to the <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeSpotInstanceRequests.html"
 * >Amazon EC2 API</a>.
 *
 * @see PersistentGetInstances
 */
public class GetSpotInstanceRequests extends
		AmazonEc2Request<List<SpotInstanceRequest>> {

	/**
	 * An (optional) list of filter to narrow the query. Only
	 * {@link SpotInstanceRequest}s matching the given filters will be returned.
	 */
	private final Collection<Filter> filters;

	/**
	 * Constructs a new {@link GetSpotInstanceRequests} task that will fetch
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
		super(awsCredentials, region);
		this.filters = filters;
	}

	@Override
	public List<SpotInstanceRequest> call() throws AmazonClientException {
		DescribeSpotInstanceRequestsRequest request = new DescribeSpotInstanceRequestsRequest();
		request.withFilters(this.filters);
		DescribeSpotInstanceRequestsResult result = getClient().getApi()
				.describeSpotInstanceRequests(request);
		return result.getSpotInstanceRequests();
	}
}