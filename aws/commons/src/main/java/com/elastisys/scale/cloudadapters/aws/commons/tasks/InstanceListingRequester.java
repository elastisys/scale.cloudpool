package com.elastisys.scale.cloudadapters.aws.commons.tasks;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.elastisys.scale.cloudadapters.aws.commons.requests.ec2.AmazonEc2Request;
import com.elastisys.scale.commons.net.retryable.Requester;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * {@link Requester} for retrieving meta data about a specific collection of
 * instances. The {@link Requester} can be repeatedly invoked if need be due to
 * eventual consistency.
 */
public class InstanceListingRequester extends AmazonEc2Request<List<Instance>> {
	static Logger logger = LoggerFactory
			.getLogger(InstanceStateRequester.class);

	private final Optional<List<String>> instanceIds;
	private final Optional<List<Filter>> filters;

	public InstanceListingRequester(AWSCredentials awsCredentials,
			String region, Optional<List<String>> instanceIds,
			Optional<List<Filter>> filters) {
		super(awsCredentials, region);
		this.instanceIds = instanceIds;
		this.filters = filters;
	}

	@Override
	public List<Instance> call() {
		// NOTE: we've specified that we're interested in a particular group of
		// instances (although empty). In this case, don't call
		// DescribeInstances with an empty list of instance ids, since that
		// would mean fetching meta data about *all* instances (in the region),
		// which is *not* what we want.
		if (this.instanceIds.isPresent() && this.instanceIds.get().isEmpty()) {
			return Collections.emptyList();
		}

		List<Instance> instances = Lists.newArrayList();
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		if (this.instanceIds.isPresent()) {
			request.withInstanceIds(this.instanceIds.get());
		}
		if (this.filters.isPresent()) {
			request.withFilters(this.filters.get());
		}
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
