package com.elastisys.scale.cloudadapters.aws.commons.requests.ec2;

import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.Instance;
import com.elastisys.scale.commons.net.retryable.Requester;
import com.elastisys.scale.commons.net.retryable.RetryHandler;
import com.elastisys.scale.commons.net.retryable.RetryableRequest;
import com.elastisys.scale.commons.net.retryable.retryhandlers.RetryUntilNoException;

/**
 * A {@link Requester} that, when called, retrieves the public IP address from
 * an EC2 instance. If the instance hasn't yet been assigned an IP address, an
 * {@link IllegalStateException} is thrown.
 * <p/>
 * Can be used in a {@link RetryableRequest} with a
 * {@link RetryUntilNoException} {@link RetryHandler} to wait for an instance to
 * be assigned an IP address.
 *
 * 
 *
 */
public class GetInstancePublicIp extends AmazonEc2Request<String> {

	static final Logger LOG = LoggerFactory
			.getLogger(GetInstancePublicIp.class);

	private final String instanceId;

	/**
	 * Creates a new {@link GetInstancePublicIp}.
	 *
	 * @param awsCredentials
	 * @param region
	 * @param instanceId
	 */
	public GetInstancePublicIp(AWSCredentials awsCredentials, String region,
			String instanceId) {
		super(awsCredentials, region);
		this.instanceId = instanceId;
	}

	@Override
	public String call() throws Exception {
		LOG.debug("requesting details for instance '{}'", this.instanceId);
		Instance instance = new GetEc2Instance(getAwsCredentials(),
				getRegion(), this.instanceId).call();
		LOG.debug("got details for instance '{}'", instance);
		String ipAddress = instance.getPublicIpAddress();

		checkState(ipAddress != null,
				"instance has not been assigned a public IP address");
		return ipAddress;
	}
}
