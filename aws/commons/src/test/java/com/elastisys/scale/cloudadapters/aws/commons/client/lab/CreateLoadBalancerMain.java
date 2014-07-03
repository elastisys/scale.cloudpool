package com.elastisys.scale.cloudadapters.aws.commons.client.lab;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.List;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.elastisys.scale.cloudadapters.aws.commons.requests.elb.CreateLoadBalancer;

/**
 * 
 * For pricing details, refer to <a
 * href="http://aws.amazon.com/pricing/elasticloadbalancing/">the
 * documentation</a>.
 * 
 * 
 * 
 */
public class CreateLoadBalancerMain extends AbstractClient {

	// TODO: set to region where you want Elastic Load Balancer to be hosted
	private static final String region = "us-east-1";
	// TODO: set to the availability zone(s) to load-balance between
	private static final List<String> availabilityZones = Arrays.asList(
			"us-east-1a", "us-east-1b");
	// TODO: set to the name of the load-balancer to create
	private static final String loadBalancerName = "end2endtest-load-balancer";

	public static void main(String[] args) throws Exception {
		logger.info(format("Creating load-balancer '%s' "
				+ "covering availability zones: %s", loadBalancerName,
				availabilityZones));
		List<Listener> portForwarding = Arrays.asList(new Listener()
				.withProtocol("HTTP").withLoadBalancerPort(80)
				.withInstanceProtocol("HTTP").withInstancePort(80));
		CreateLoadBalancer request = new CreateLoadBalancer(
				new PropertiesCredentials(credentialsFile), region,
				loadBalancerName, availabilityZones, portForwarding);
		CreateLoadBalancerResult result = request.call();
		logger.info("Created load balancer at: {}", result.getDNSName());
	}
}
