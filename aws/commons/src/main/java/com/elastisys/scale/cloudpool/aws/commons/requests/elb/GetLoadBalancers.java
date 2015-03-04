package com.elastisys.scale.cloudpool.aws.commons.requests.elb;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;

/**
 * A {@link Callable} task that, when executed, requests a listing of AWS
 * Elastic Load Balancers.
 * 
 * 
 * 
 */
public class GetLoadBalancers extends
		AmazonElbRequest<List<LoadBalancerDescription>> {

	/**
	 * The load balancers of interest. If empty, all load balancers will be
	 * returned.
	 */
	private List<String> loadBalancerNames;

	/**
	 * @param awsCredentials
	 * @param region
	 * @param loadBalancerNames
	 *            Zero or more load balancers of interest. If no load balancer
	 *            is specified, information will be returned for all load
	 *            balancers.
	 */
	public GetLoadBalancers(AWSCredentials awsCredentials, String region,
			String... loadBalancerNames) {
		super(awsCredentials, region);
		this.loadBalancerNames = Arrays.asList(loadBalancerNames);
	}

	@Override
	public List<LoadBalancerDescription> call() {
		DescribeLoadBalancersRequest request = null;
		if (this.loadBalancerNames.isEmpty()) {
			request = new DescribeLoadBalancersRequest();
		} else {
			request = new DescribeLoadBalancersRequest(this.loadBalancerNames);
		}
		DescribeLoadBalancersResult result = getClient().getApi()
				.describeLoadBalancers(request);
		List<LoadBalancerDescription> loadBalancerDescriptions = result
				.getLoadBalancerDescriptions();
		return loadBalancerDescriptions;
	}

}
