package com.elastisys.scale.cloudpool.aws.commons.requests.elb;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.elasticloadbalancing.model.ConfigureHealthCheckRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.HealthCheck;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * A {@link Callable} task that, when executed, requests the creation of an AWS
 * Elastic Load Balancer.
 * 
 * 
 */
public class CreateLoadBalancer extends
		AmazonElbRequest<CreateLoadBalancerResult> {

	/** The name of the load-balancer to create. */
	private final String loadBalancerName;
	/** The availability zone(s) to load-balance between. */
	private final List<String> availabilityZones;
	/**
	 * Which inbound ports (and protocols) to listen for and what ports on
	 * instances to forward requests to.
	 */
	private final List<Listener> portForwarding;

	public CreateLoadBalancer(AWSCredentials awsCredentials, String region,
			String loadBalancerName, List<String> availabilityZones,
			List<Listener> portForwarding) {
		super(awsCredentials, region);
		this.loadBalancerName = loadBalancerName;
		this.availabilityZones = availabilityZones;
		this.portForwarding = portForwarding;
	}

	public static List<String> getNames(List<AvailabilityZone> availabilityZones) {
		List<String> names = Lists.newArrayList();
		for (AvailabilityZone zone : availabilityZones) {
			names.add(zone.getZoneName());
		}
		return names;
	}

	@Override
	public CreateLoadBalancerResult call() {
		try {
			Set<String> existingLoadBalancers = getLoadBalancers();
			if (existingLoadBalancers.contains(this.loadBalancerName)) {
				throw new IllegalArgumentException(String.format(
						"A load balancer with name '%s' already exists.",
						this.loadBalancerName));
			}
			CreateLoadBalancerRequest request = new CreateLoadBalancerRequest()
					.withLoadBalancerName(this.loadBalancerName)
					.withAvailabilityZones(this.availabilityZones)
					.withListeners(this.portForwarding);
			CreateLoadBalancerResult result = getClient().getApi()
					.createLoadBalancer(request);
			HealthCheck healthCheck = new HealthCheck().withTarget("HTTP:80/")
					.withInterval(10).withUnhealthyThreshold(3).withTimeout(5)
					.withHealthyThreshold(3);
			getClient()
					.getApi()
					.configureHealthCheck(
							new ConfigureHealthCheckRequest()
									.withLoadBalancerName(this.loadBalancerName)
									.withHealthCheck(healthCheck));
			return result;
		} finally {
			getClient().getApi().shutdown();
		}
	}

	private Set<String> getLoadBalancers() {
		Set<String> names = Sets.newHashSet();
		List<LoadBalancerDescription> loadBalancers = new GetLoadBalancers(
				getAwsCredentials(), getRegion()).call();
		for (LoadBalancerDescription loadBalancerDescription : loadBalancers) {
			names.add(loadBalancerDescription.getLoadBalancerName());
		}
		return names;
	}
}
