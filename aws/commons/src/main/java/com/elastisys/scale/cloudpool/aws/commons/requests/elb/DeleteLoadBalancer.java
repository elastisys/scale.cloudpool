package com.elastisys.scale.cloudpool.aws.commons.requests.elb;

import static java.lang.String.format;

import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;

/**
 * A {@link Callable} task that, when executed, requests the deletion of an AWS
 * Elastic Load Balancer.
 * 
 * 
 * 
 */
public class DeleteLoadBalancer extends AmazonElbRequest<Void> {

	private String loadBalancerName;

	public DeleteLoadBalancer(AWSCredentials awsCredentials, String region,
			String loadBalancerName) {
		super(awsCredentials, region);
		this.loadBalancerName = loadBalancerName;
	}

	@Override
	public Void call() {
		List<LoadBalancerDescription> existingLoadBalancers = new GetLoadBalancers(
				getAwsCredentials(), getRegion()).call();
		for (LoadBalancerDescription loadBalancer : existingLoadBalancers) {
			if (loadBalancer.getLoadBalancerName()
					.equals(this.loadBalancerName)) {
				getClient().getApi().deleteLoadBalancer(
						new DeleteLoadBalancerRequest()
								.withLoadBalancerName(this.loadBalancerName));
				return null;
			}
		}
		throw new IllegalArgumentException(format(
				"Failed to delete load balancer '%s': it doesn't exist",
				this.loadBalancerName));
	}
}
