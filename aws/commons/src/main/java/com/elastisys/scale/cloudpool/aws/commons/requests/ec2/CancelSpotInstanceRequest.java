package com.elastisys.scale.cloudpool.aws.commons.requests.ec2;

import static com.amazonaws.services.ec2.model.SpotInstanceState.Cancelled;
import static com.elastisys.scale.cloudpool.aws.commons.predicates.SpotRequestPredicates.stateIn;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;

/**
 * A {@link Callable} task that, when executed, requests a
 * {@link SpotInstanceRequest} to be canceled in a region and waits for the
 * instance to appear canceled in {@code DescribeSpotRequestInstances}.
 * <p/>
 * Due to the eventual consistency semantics of the Amazon API, operations may
 * need time to propagate through the system and results may not be immediate.
 */
public class CancelSpotInstanceRequest extends
		AmazonEc2Request<CancelSpotInstanceRequestsResult> {

	/** The {@link SpotInstanceRequest} to cancel. */
	private final String spotRequestId;

	public CancelSpotInstanceRequest(AWSCredentials awsCredentials,
			String region, String spotInstanceRequestId) {
		super(awsCredentials, region);
		this.spotRequestId = spotInstanceRequestId;
	}

	@Override
	public CancelSpotInstanceRequestsResult call() throws AmazonClientException {
		CancelSpotInstanceRequestsRequest request = new CancelSpotInstanceRequestsRequest()
				.withSpotInstanceRequestIds(this.spotRequestId);
		CancelSpotInstanceRequestsResult result = getClient().getApi()
				.cancelSpotInstanceRequests(request);
		awaitCancellation(this.spotRequestId);
		return result;
	}

	/**
	 * Waits for the canceled {@link SpotInstanceRequest} to be shown as
	 * cancelled in the API.
	 *
	 * @param spotRequestId
	 */
	private void awaitCancellation(String spotRequestId) {
		String name = String.format("await-cancelled{%s}", spotRequestId);
		GetSpotInstanceRequest requester = new GetSpotInstanceRequest(
				getAwsCredentials(), getRegion(), spotRequestId);
		int initialDelay = 1;
		int maxRetries = 8;
		Retryable<SpotInstanceRequest> retryer = Retryers
				.exponentialBackoffRetryer(name, requester, initialDelay,
						TimeUnit.SECONDS, maxRetries,
						stateIn(Cancelled.toString()));

		try {
			retryer.call();
		} catch (Exception e) {
			throw new RuntimeException(String.format(
					"gave up waiting for spot instance request "
							+ "to appear canceled: '%s': %s", spotRequestId,
					e.getMessage()), e);
		}
	}
}
