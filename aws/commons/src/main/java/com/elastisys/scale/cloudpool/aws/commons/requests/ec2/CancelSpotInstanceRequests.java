package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import static com.amazonaws.services.ec2.model.SpotInstanceState.Cancelled;
import static com.elastisys.scale.cloudpool.aws.commons.predicates.SpotRequestPredicates.allInAnyOfStates;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.google.common.collect.ImmutableList;

/**
 * A {@link Callable} task that, when executed, requests a collection of spot
 * instance requests to be canceled in a region and waits for the requests to
 * appear cancelled in {@code DescribeSpotRequestInstances}, which may not be
 * immediate due to the <a href=
 * "http://docs.aws.amazon.com/AWSEC2/latest/APIReference/query-api-troubleshooting.html#eventual-consistency"
 * >eventual consistency semantics</a> of the Amazon API.
 */
public class CancelSpotInstanceRequests extends
		AmazonEc2Request<CancelSpotInstanceRequestsResult> {
	/** Initial exponential back-off delay in ms. */
	private static final int INITIAL_BACKOFF_DELAY = 1000;
	/** Maximum number of retries of operations. */
	private static final int MAX_RETRIES = 8;

	/** The {@link SpotInstanceRequest} to cancel. */
	private final List<String> spotRequestIds;

	public CancelSpotInstanceRequests(AWSCredentials awsCredentials,
			String region, List<String> spotInstanceRequestIds) {
		super(awsCredentials, region);
		this.spotRequestIds = ImmutableList.copyOf(spotInstanceRequestIds);
	}

	@Override
	public CancelSpotInstanceRequestsResult call() throws AmazonClientException {
		CancelSpotInstanceRequestsRequest request = new CancelSpotInstanceRequestsRequest()
				.withSpotInstanceRequestIds(this.spotRequestIds);
		CancelSpotInstanceRequestsResult result = getClient().getApi()
				.cancelSpotInstanceRequests(request);
		awaitCancellation(this.spotRequestIds);
		return result;
	}

	/**
	 * Waits for the cancelled spot instance requests to be reported as
	 * cancelled by the Amazon API.
	 *
	 * @param spotRequestIds
	 */
	private void awaitCancellation(List<String> spotRequestIds) {
		String name = String.format("await-cancelled{%s}", spotRequestIds);
		GetSpotInstanceRequests requester = new GetSpotInstanceRequests(
				getAwsCredentials(), getRegion(), spotRequestIds, null);
		Retryable<List<SpotInstanceRequest>> retryer = Retryers
				.exponentialBackoffRetryer(name, requester,
						INITIAL_BACKOFF_DELAY, TimeUnit.MILLISECONDS,
						MAX_RETRIES, allInAnyOfStates(Cancelled.toString()));

		try {
			retryer.call();
		} catch (Exception e) {
			throw new RuntimeException(String.format(
					"gave up waiting for spot instance requests "
							+ "to be cancelled %s: %s", spotRequestIds,
					e.getMessage()), e);
		}
	}
}
