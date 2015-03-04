package com.elastisys.scale.cloudpool.aws.commons.client.lab;

import java.util.List;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.elastisys.scale.cloudpool.aws.commons.requests.elb.GetLoadBalancers;

public class ListLoadBalancersMain extends AbstractClient {

	// TODO: set to region where you want to list Elastic Load Balancers
	private static final String region = "us-east-1";

	public static void main(String[] args) throws Exception {
		GetLoadBalancers request = new GetLoadBalancers(
				new PropertiesCredentials(credentialsFile), region);
		List<LoadBalancerDescription> loadBalancers = request.call();
		logger.info("Load-balancers in region '{}'", region);
		for (LoadBalancerDescription loadBalancer : loadBalancers) {
			logger.info("  {}: {} ({})", loadBalancer.getLoadBalancerName(),
					loadBalancer.getDNSName(), loadBalancer);
		}
	}

}
