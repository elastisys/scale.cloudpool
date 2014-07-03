package com.elastisys.scale.cloudadapters.aws.commons.client.lab;

import java.util.List;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.elastisys.scale.cloudadapters.aws.commons.requests.elb.DeleteLoadBalancer;
import com.elastisys.scale.cloudadapters.aws.commons.requests.elb.GetLoadBalancers;

public class DeleteLoadBalancersMain extends AbstractClient {

	// TODO: set to region where you want to delete Elastic Load Balancers
	private static final String region = "us-east-1";

	public static void main(String[] args) throws Exception {
		PropertiesCredentials awsCredentials = new PropertiesCredentials(
				credentialsFile);
		GetLoadBalancers listRequest = new GetLoadBalancers(awsCredentials,
				region);
		List<LoadBalancerDescription> loadBalancers = listRequest.call();
		logger.info("Deleting load-balancers in region '{}'", region);
		for (LoadBalancerDescription loadBalancer : loadBalancers) {
			logger.info("  Deleting {}: {}",
					loadBalancer.getLoadBalancerName(),
					loadBalancer.getDNSName());
			new DeleteLoadBalancer(awsCredentials, region,
					loadBalancer.getLoadBalancerName()).call();
		}
	}

}
