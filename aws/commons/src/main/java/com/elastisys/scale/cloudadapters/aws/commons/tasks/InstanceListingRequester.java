package com.elastisys.scale.cloudadapters.aws.commons.tasks;

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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Requester for a listing of instances that can be repeatedly invoked if need
 * be due to eventual consistency.
 */
public class InstanceListingRequester extends AmazonEc2Request<List<Instance>> {
	static Logger logger = LoggerFactory
			.getLogger(InstanceStateRequester.class);

	private final ImmutableList<String> instanceIds;
	private final ImmutableList<Filter> filters;

	public InstanceListingRequester(AWSCredentials awsCredentials,
			String region, List<String> instanceIds, List<Filter> filters) {
		super(awsCredentials, region);
		this.instanceIds = ImmutableList.copyOf(instanceIds);
		this.filters = ImmutableList.copyOf(filters);
	}

	@Override
	public List<Instance> call() throws Exception {
		List<Instance> instances = Lists.newLinkedList();

		DescribeInstancesRequest request = new DescribeInstancesRequest();

		if (this.instanceIds.size() > 0) {
			request.withInstanceIds(this.instanceIds);
		}

		if (this.filters.size() > 0) {
			request.withFilters(this.filters);
		}

		DescribeInstancesResult result = getClient().getApi()
				.describeInstances(request);

		String nextToken = "";

		do {
			for (Reservation reservation : result.getReservations()) {
				for (Instance instance : reservation.getInstances()) {
					instances.add(instance);
				}
			}

			nextToken = result.getNextToken();
			request.setNextToken(nextToken);
		} while (nextToken != null);

		return instances;

	}
}
